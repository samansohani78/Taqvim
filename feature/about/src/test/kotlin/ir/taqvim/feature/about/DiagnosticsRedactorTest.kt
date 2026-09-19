/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.about

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll
import ir.taqvim.core.testing.PropertyTesting
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

/** F-16: diagnostics lose titles, coordinates beyond two decimals, e-mail addresses, link tokens and secrets. */
class DiagnosticsRedactorTest {
    private val persianLetters = "آابپتثجچحخدذرزژسشصضطظعغفقکگلمنوهی".toList()
    private val titles = Arb.list(Arb.element(persianLetters), 6..24).map { it.joinToString("") }
    private val tokens = Arb.list(Arb.element(('a'..'z') + ('0'..'9')), 12..40).map { it.joinToString("") }
    private val longFraction = Regex("""\p{Nd}[.٫]\p{Nd}{3}""")

    private val titleTemplates: List<(String) -> String> =
        listOf(
            { "Saved event \"$it\"" },
            { "title=$it updated" },
            { "Title: \"$it\" id=12" },
            { "note=«$it»" },
            { "Reminder «$it» fired" },
            { "name=$it" },
        )

    @Test
    fun `quoted titles and sensitive values never survive`(): Unit =
        runBlocking {
            checkAll(PropertyTesting.iterations, titles, Arb.element(titleTemplates)) { title, template ->
                DiagnosticsRedactor.redact(template(title)) shouldNotContain title
            }
        }

    @Test
    fun `coordinates keep at most two decimals in any digit script`(): Unit =
        runBlocking {
            val degrees = Arb.int(-180..180)
            val fractions = Arb.int(0..999_999)
            val scripts = Arb.int(0..1)
            checkAll(PropertyTesting.iterations, degrees, fractions, degrees, fractions, scripts) { a, b, c, d, s ->
                val lat = localize("$a.${b.toString().padStart(6, '0')}", s)
                val lon = localize("$c.${d.toString().padStart(6, '0')}", s)
                val accuracy = localize("12.5", s)
                val redacted = DiagnosticsRedactor.redact("Fix at $lat,$lon then $lat $lon accuracy $accuracy m")
                longFraction.containsMatchIn(redacted) shouldBe false
                redacted shouldContain "accuracy $accuracy m"
            }
        }

    @Test
    fun `e-mail addresses and link tokens are removed`(): Unit =
        runBlocking {
            val templates: List<(String) -> String> =
                listOf(
                    { "fetch https://calendar.example.org/feed.ics?token=$it failed" },
                    { "webcal://host.example/$it/cal.ics returned 304" },
                    { "sent to user.$it@example.com" },
                    { "https://user:$it@example.com/x" },
                    { "opened content://media/$it" },
                )
            checkAll(PropertyTesting.iterations, tokens, Arb.element(templates)) { token, template ->
                DiagnosticsRedactor.redact(template(token)) shouldNotContain token
            }
        }

    @Test
    fun `passphrases are never kept`(): Unit =
        runBlocking {
            checkAll(PropertyTesting.iterations, tokens) { secret ->
                DiagnosticsRedactor.redact("Backup passphrase=$secret rejected") shouldNotContain secret
                DiagnosticsRedactor.redact("Password: $secret") shouldNotContain secret
            }
        }

    @Test
    fun `a sensitive value is removed whole, not only its first word`() {
        // Review R03: only the first word of an unquoted value used to be removed.
        val cases =
            mapOf(
                "title=Meeting with oncologist tomorrow" to "title=[redacted]",
                "note: call Dr Ahmadi about the biopsy" to "note: [redacted]",
                "sync title=Meeting with oncologist\ncount=3" to "sync title=[redacted]\ncount=3",
                "title=a b; place=Tehran clinic" to "title=[redacted]; place=[redacted]",
            )
        cases.forEach { (text, redacted) -> DiagnosticsRedactor.redact(text) shouldBe redacted }
    }

    @Test
    fun `credentials in headers and bearer tokens are removed`() {
        val secrets =
            listOf(
                "Authorization: Bearer abcdefSECRET",
                "authorization=Basic dXNlcjpwYXNzd29yZA==",
                "Proxy-Authorization: Digest abcdefSECRET",
                "Cookie: session=abcdefSECRET; theme=dark",
                "Set-Cookie: id=abcdefSECRET",
                "X-Api-Key: abcdefSECRET",
                "api_key=abcdefSECRET",
                "access_token=abcdefSECRET",
                "refresh-token: abcdefSECRET",
                "request failed, bearer abcdefSECRET rejected",
            )
        secrets.forEach { DiagnosticsRedactor.redact(it) shouldNotContain "abcdefSECRET" }
    }

    @Test
    fun `keys in the app's languages are sensitive too`() {
        val secret = "ملاقات خصوصی با دکتر"
        val keys =
            listOf(
                "عنوان",
                "نام",
                "توضیحات",
                "یادداشت",
                "رمز",
                "گذرواژه",
                "نشانی",
                "مکان",
                "شهر",
                "اسم",
                "الاسم",
                "ملاحظات",
                "كلمة المرور",
                "العنوان",
                "الموقع",
                "سرلیک",
                "نوم",
                "پته",
                "ناونیشان",
                "ناو",
                "شوێن",
                "sernav",
                "nav",
                "cih",
                "शीर्षक",
                "नाम",
                "ठेगाना",
                "स्थान",
                "टिप्पणी",
            )
        keys.forEach { key ->
            listOf("$key=$secret", "$key: $secret", "$key = $secret").forEach { text ->
                (text to DiagnosticsRedactor.redact(text)) shouldBe
                    (text to text.substringBefore(secret) + "[redacted]")
            }
        }
    }

    @Test
    fun `ordinary words that merely contain a key are kept`() {
        listOf("rename=done", "renamed: 3 files", "subtitle=ok", "tokenizer: ready").forEach {
            DiagnosticsRedactor.redact(it) shouldBe it
        }
    }

    @Test
    fun `technical text is kept and long digit runs are masked`() {
        listOf("Refreshed 3 subscriptions in 120 ms", "Room migration 2->3 done", "HTTP 304 Not Modified").forEach {
            DiagnosticsRedactor.redact(it) shouldBe it
        }
        DiagnosticsRedactor.redact("call 09121234567") shouldBe "call [number]"
        DiagnosticsRedactor.redact("link https://example.com/a?b=c") shouldBe "link https://example.com/[redacted]"
        val entry = DiagnosticEntry(1, DiagnosticLevel.INFO, "tag \"x\"", "at 1.23456")
        DiagnosticsRedactor.redact(entry) shouldBe entry.copy(tag = "tag [redacted]", message = "at 1.23")
    }

    private fun localize(
        text: String,
        script: Int,
    ): String =
        if (script == 0) {
            text
        } else {
            text
                .map {
                    if (it.isDigit()) {
                        '۰' + (it - '0')
                    } else if (it == '.') {
                        '٫'
                    } else {
                        it
                    }
                }.joinToString("")
        }
}
