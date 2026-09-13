/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.buildlogic.license

/**
 * Maps free-form POM license names and URLs to SPDX identifiers.
 *
 * The mapping is deliberately conservative: anything ambiguous (for example a bare "BSD") returns
 * `null`, which the policy treats as an unknown license that needs an explicit, evidenced override.
 * Copyleft families are matched before permissive ones so that e.g. "GNU Lesser General Public
 * License" can never be mistaken for something weaker.
 */
object SpdxNormalizer {
    private class Rule(
        val spdxId: String,
        vararg alternatives: String,
    ) {
        val pattern = Regex(alternatives.joinToString("|"))
    }

    private val KNOWN_IDS =
        listOf(
            "Apache-2.0",
            "MIT",
            "MIT-0",
            "BSD-2-Clause",
            "BSD-3-Clause",
            "ISC",
            "Unicode-3.0",
            "Unicode-DFS-2016",
            "CC0-1.0",
            "EPL-1.0",
            "EPL-2.0",
            "MPL-2.0",
            "LGPL-2.1-only",
            "LGPL-2.1-or-later",
            "LGPL-3.0-only",
            "LGPL-3.0-or-later",
            "GPL-2.0-only",
            "GPL-2.0-or-later",
            "GPL-3.0-only",
            "GPL-3.0-or-later",
            "AGPL-3.0-only",
            "AGPL-3.0-or-later",
            "CDDL-1.0",
            "CDDL-1.1",
            "LicenseRef-Public-Domain",
        ).associateBy { it.lowercase() }

    private val NAME_RULES =
        listOf(
            Rule("AGPL-3.0-or-later", """\bagpl""", "affero"),
            Rule("LGPL-2.1-or-later", """\blgpl""", "lesser general public", "library general public"),
            Rule("GPL-2.0-with-classpath-exception", """(\bgpl|general public license).*(classpath|\bcpe\b)"""),
            Rule("GPL-2.0-or-later", """\bgpl""", "general public license"),
            Rule("MPL-2.0", """\bmpl\b""", "mozilla public"),
            Rule("CDDL-1.1", """\bcddl""", "common development and distribution"),
            Rule(
                "EPL-2.0",
                """\bepl[\s-]*(v|version)?[\s.-]*2""",
                """eclipse public license[\s,-]*(v|version)?[\s.-]*2""",
            ),
            Rule(
                "EPL-1.0",
                """\bepl[\s-]*(v|version)?[\s.-]*1""",
                """eclipse public license[\s,-]*(v|version)?[\s.-]*1""",
            ),
            // The Eclipse Distribution License 1.0 is the BSD 3-Clause license.
            Rule("BSD-3-Clause", "eclipse distribution license", """\bedl\b"""),
            Rule("Apache-2.0", """\bapache\b.*\b2(\.0)?\b""", """\basl[\s-]*2""", """\bal[\s-]*2\.0"""),
            Rule("MIT-0", """\bmit-0\b""", "mit no attribution"),
            // The Bouncy Castle Licence is an MIT license with a different title.
            Rule("MIT", """\bmit\b""", "bouncy castle licen[cs]e"),
            Rule("BSD-2-Clause", """\bbsd[\s-]*(license[\s-]*)?2""", "simplified bsd", "freebsd", "2-clause"),
            // The Go License is the BSD 3-Clause license.
            Rule(
                "BSD-3-Clause",
                """\bbsd[\s-]*(license[\s-]*)?3""",
                "new bsd",
                "revised bsd",
                "modified bsd",
                "3-clause",
                """\bgo license""",
            ),
            Rule("ISC", """\bisc\b"""),
            Rule("Unicode-3.0", """\bunicode\b""", """\bicu license"""),
            Rule("CC0-1.0", """\bcc0\b""", "creative commons zero"),
            Rule("LicenseRef-Public-Domain", "public domain"),
        )

    private val URL_RULES =
        listOf(
            Rule("Apache-2.0", """apache\.org/licenses/license-2\.0"""),
            Rule("MIT", """opensource\.org/licenses?/mit(\.php|\.html)?/?$"""),
            Rule("BSD-2-Clause", """opensource\.org/licenses?/bsd-2-clause"""),
            Rule("BSD-3-Clause", """opensource\.org/licenses?/bsd-3-clause"""),
            Rule("EPL-2.0", """eclipse\.org/legal/epl-(v20|2\.0)"""),
            Rule("EPL-1.0", """eclipse\.org/legal/epl-v10"""),
            Rule("Unicode-3.0", """unicode\.org/(copyright|license)"""),
            Rule("CC0-1.0", """creativecommons\.org/publicdomain/zero"""),
            Rule("AGPL-3.0-or-later", """gnu\.org/licenses/agpl"""),
            Rule("LGPL-2.1-or-later", """gnu\.org/licenses/(old-licenses/)?lgpl"""),
            Rule("GPL-2.0-or-later", """gnu\.org/licenses/(old-licenses/)?gpl"""),
            Rule("MPL-2.0", """mozilla\.org/(en-us/)?mpl"""),
        )

    private val ALTERNATIVE_SEPARATOR = Regex("""\s+or\s+""", RegexOption.IGNORE_CASE)

    /** Splits an SPDX-style disjunction ("(EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0)") into alternatives. */
    fun alternatives(name: String): List<String> =
        name
            .trim()
            .removePrefix("(")
            .removeSuffix(")")
            .split(ALTERNATIVE_SEPARATOR)
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    /** Returns the SPDX id for a declared license, trying the exact id, then the name, then the URL. */
    fun normalize(
        name: String?,
        url: String?,
    ): String? {
        val cleanName = name?.trim()?.lowercase().orEmpty()
        val cleanUrl = url?.trim()?.lowercase().orEmpty()
        return KNOWN_IDS[cleanName]
            ?: NAME_RULES.firstMatch(cleanName)
            ?: URL_RULES.firstMatch(cleanUrl)
    }

    private fun List<Rule>.firstMatch(text: String): String? =
        if (text.isEmpty()) null else firstOrNull { it.pattern.containsMatchIn(text) }?.spdxId
}
