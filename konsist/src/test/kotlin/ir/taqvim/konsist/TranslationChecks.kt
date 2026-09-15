/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.konsist

/** A translated entry together with its English source (T-1702). */
data class TranslatedEntry(
    val folder: ResourceFolder,
    val name: String,
    val source: StringEntry,
    val translation: StringEntry,
) {
    /** `feature/about/src/main/res [fa] about_title` — the prefix of every violation. */
    val label: String get() = "${folder.resDir} [${folder.language ?: SOURCE_LANGUAGE}] $name"

    companion object {
        const val SOURCE_LANGUAGE = "en"
    }
}

/**
 * Translation quality rules (docs/i18n/TRANSLATING.md). Each rule returns readable violations; empty means compliant.
 * They check structure only — placeholders, plural categories, copied English, punctuation and control characters —
 * never whether a translation is correct, which stays a reviewer's job.
 */
object TranslationChecks {
    /** A format placeholder, or `%%` (a literal percent sign) so that it is consumed and never read as one. */
    private val PLACEHOLDER = Regex("""%%|%(\d+\$)?[-#+ 0,(]*\d*(\.\d+)?[a-zA-Z]""")
    private const val LITERAL_PERCENT = "%%"
    private val WORD = Regex("""\p{L}{2,}""")
    private val ARABIC_SCRIPT = Regex("""[؀-ۿݐ-ݿﭐ-﷿ﹰ-]""")
    private val LATIN_OR_DIGIT = Regex("[A-Za-z0-9]")
    private const val ASCII_PUNCTUATION = ",;?"
    private const val PLACEHOLDER_MARK = "\uFFFC"
    private const val OTHER = "other"

    /** Embedding, override and mark characters; isolates (U+2066–U+2069) are allowed (docs/i18n/STRINGS.md). */
    private val FORBIDDEN_BIDI = setOf('\u200E', '\u200F', '\u202A', '\u202B', '\u202C', '\u202D', '\u202E')

    /** Every translated entry that has an English source in the same `res` directory. */
    fun translated(folders: List<ResourceFolder>): List<TranslatedEntry> {
        val sources = folders.filter { it.language == null }.associateBy { it.resDir }
        return folders
            .filter { it.language != null }
            .flatMap { folder ->
                val source = sources[folder.resDir]?.entries.orEmpty()
                folder.entries.mapNotNull { (name, entry) ->
                    source[name]?.let { TranslatedEntry(folder, name, it, entry) }
                }
            }
    }

    /** Positional placeholders of [text] such as `%1$s`, sorted; `%%` is not a placeholder. */
    fun placeholders(text: String): List<String> =
        PLACEHOLDER
            .findAll(text)
            .map { it.value }
            .filter { it != LITERAL_PERCENT }
            .toList()
            .sorted()

    /** Translations whose placeholders differ from the source's (a plural item may omit the count). */
    fun placeholderMismatches(folders: List<ResourceFolder>): List<String> =
        translated(folders).mapNotNull { entry ->
            val problem =
                when (val source = entry.source) {
                    is StringEntry.Single -> singleMismatch(source, entry.translation)
                    is StringEntry.Array -> arrayMismatch(source, entry.translation)
                    is StringEntry.Plural -> pluralMismatch(source, entry.translation)
                }
            problem?.let { "${entry.label}: $it" }
        }

    /**
     * Plural entries missing a category the language needs, or with a category it never selects.
     *
     * @param requiredCategories Android quantities a language distinguishes, or `null` for an unknown language.
     */
    fun pluralCategoryIssues(
        folders: List<ResourceFolder>,
        requiredCategories: (String) -> Set<String>?,
    ): List<String> =
        folders.flatMap { folder ->
            val language = folder.language ?: TranslatedEntry.SOURCE_LANGUAGE
            val required = requiredCategories(language) ?: return@flatMap emptyList()
            folder.entries.mapNotNull { (name, entry) ->
                (entry as? StringEntry.Plural)?.let { categoryProblem(it.items.keys, required) }?.let {
                    "${folder.resDir} [$language] $name: $it"
                }
            }
        }

    /** Translations identical to an English source that contains words, unless [allowed] lists `resDir: name`. */
    fun sameAsSource(
        folders: List<ResourceFolder>,
        allowed: Set<String>,
    ): List<String> =
        translated(folders)
            .filter { "${it.folder.resDir}: ${it.name}" !in allowed }
            .filter { entry ->
                val source = entry.source.texts
                source.any(WORD::containsMatchIn) && source == entry.translation.texts
            }.map { "${it.label}: identical to the English source" }

    /** Arabic-script text in [rtlLanguages] that uses ASCII `,`, `;` or `?` outside Latin runs (use ، ؛ ؟). */
    fun rtlPunctuation(
        folders: List<ResourceFolder>,
        rtlLanguages: Set<String>,
    ): List<String> =
        folders
            .filter { it.language in rtlLanguages }
            .flatMap { folder ->
                folder.entries.mapNotNull { (name, entry) ->
                    entry.texts.firstNotNullOfOrNull(::asciiPunctuationInArabicText)?.let {
                        "${folder.resDir} [${folder.language}] $name: ASCII '$it' in right-to-left text"
                    }
                }
            }

    /** Resources in any language that contain bidi marks, embeddings or overrides. */
    fun bidiControls(folders: List<ResourceFolder>): List<String> =
        folders.flatMap { folder ->
            folder.entries
                .filterValues { entry -> entry.texts.any { text -> text.any { it in FORBIDDEN_BIDI } } }
                .keys
                .map { "${folder.resDir} [${folder.language ?: TranslatedEntry.SOURCE_LANGUAGE}] $it: bidi control" }
        }

    private fun singleMismatch(
        source: StringEntry.Single,
        translation: StringEntry,
    ): String? =
        when {
            translation !is StringEntry.Single -> {
                "is not a string in the source"
            }

            placeholders(source.text) != placeholders(translation.text) -> {
                "placeholders ${placeholders(translation.text)} but source has ${placeholders(source.text)}"
            }

            else -> {
                null
            }
        }

    private fun arrayMismatch(
        source: StringEntry.Array,
        translation: StringEntry,
    ): String? =
        when {
            translation !is StringEntry.Array -> {
                "is not a string-array in the source"
            }

            translation.items.size != source.items.size -> {
                "${translation.items.size} items but source has ${source.items.size}"
            }

            source.items.zip(translation.items).any { (a, b) -> placeholders(a) != placeholders(b) } -> {
                "item placeholders differ from the source"
            }

            else -> {
                null
            }
        }

    private fun pluralMismatch(
        source: StringEntry.Plural,
        translation: StringEntry,
    ): String? {
        if (translation !is StringEntry.Plural) return "is not a plurals in the source"
        val sourceOther = placeholders(source.items[OTHER].orEmpty())
        val extra =
            translation.items.values
                .flatMap(::placeholders)
                .toSet() - sourceOther.toSet()
        return when {
            placeholders(translation.items[OTHER].orEmpty()) != sourceOther -> {
                "'other' placeholders differ from the source $sourceOther"
            }

            extra.isNotEmpty() -> {
                "placeholders $extra are not in the source"
            }

            else -> {
                null
            }
        }
    }

    private fun categoryProblem(
        present: Set<String>,
        required: Set<String>,
    ): String? {
        val missing = required - present
        val unused = present - required
        return when {
            missing.isNotEmpty() -> "missing plural categories ${missing.sorted()}"
            unused.isNotEmpty() -> "plural categories ${unused.sorted()} are never selected in this language"
            else -> null
        }
    }

    private fun asciiPunctuationInArabicText(text: String): Char? {
        if (!ARABIC_SCRIPT.containsMatchIn(text)) return null
        val masked = PLACEHOLDER.replace(text, PLACEHOLDER_MARK)
        return masked.indices
            .firstOrNull { index ->
                masked[index] in ASCII_PUNCTUATION && !isInLatinRun(masked, index)
            }?.let(masked::get)
    }

    private fun isInLatinRun(
        text: String,
        index: Int,
    ): Boolean {
        val before = text.substring(0, index).trimEnd().lastOrNull()
        val after = text.substring(index + 1).trimStart().firstOrNull()
        return listOfNotNull(before, after).any { LATIN_OR_DIGIT.matches(it.toString()) }
    }
}
