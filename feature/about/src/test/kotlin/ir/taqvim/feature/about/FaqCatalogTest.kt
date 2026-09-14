/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.about

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch
import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import ir.taqvim.core.testing.PropertyTesting
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

/** T-1901 FAQ table: grouping, links in the documented `taqvim://` shapes, and search across letter and digit forms. */
class FaqCatalogTest {
    /** English stand-ins for the resource texts, with one Persian question to exercise letter variants. */
    private val texts: Map<FaqEntry, FaqText> =
        FaqEntry.entries.associateWith { entry ->
            val name = entry.name.lowercase()
            FaqText(entry.topic.name.lowercase(), "question $name", "answer ${entry.ordinal} days")
        } +
            (FaqEntry.MAIN_CALENDAR to FaqText(CALENDARS_FA, "تقویم اصلی کجاست؟", "در تنظیمات ۳۰ گزینه هست")) +
            (FaqEntry.PASSPHRASE to FaqText("پشتیبان", "گذرواژهٔ پشتیبان‌گیری", "بازیابی ممکن نیست"))

    @Test
    fun `every topic has questions and the default page lists them all in topic order`() {
        val page = FaqContent()

        page.groups.map { it.topic } shouldContainExactly FaqTopic.entries
        page.groups.flatMap { it.entries } shouldContainExactly FaqEntry.entries.sortedBy { it.topic.ordinal }
        page.noResults shouldBe false
    }

    @Test
    fun `links use the documented taqvim link shapes`() {
        FaqEntry.entries.mapNotNull { it.link }.forEach { it shouldMatch AUTOMATION_LINK }
        FaqEntry.entries.filter { it.link != null }.size shouldBe 10
    }

    @Test
    fun `search ignores Arabic letter forms, spaces, ZWNJ and digit scripts`() {
        FaqSearch.matches(texts, "تقويم اصلي") shouldContainExactly listOf(FaqEntry.MAIN_CALENDAR)
        FaqSearch.matches(texts, "30") shouldContainExactly listOf(FaqEntry.MAIN_CALENDAR)
        FaqSearch.matches(texts, "پشتیبان گیری") shouldContainExactly listOf(FaqEntry.PASSPHRASE)
        FaqSearch.matches(texts, "QUESTION  nepali") shouldContainExactly listOf(FaqEntry.NEPALI)
        FaqSearch.matches(texts, "question absent").isEmpty() shouldBe true
        FaqSearch.matches(texts, " ‌ ") shouldContainExactly FaqEntry.entries
    }

    @Test
    fun `filtered pages keep topic order and only the expanded entries that still match`() {
        val page = faqContent(texts, "nepali", setOf(FaqEntry.NEPALI, FaqEntry.ATHAN))

        page.groups.single().topic shouldBe FaqTopic.CALENDARS
        page.expanded shouldContainExactly setOf(FaqEntry.NEPALI)
        faqContent(texts, "absent words", setOf(FaqEntry.ATHAN)).noResults shouldBe true
    }

    @Test
    fun `any piece of a question finds its entry`(): Unit =
        runBlocking {
            checkAll(
                PropertyTesting.iterations,
                Arb.element(FaqEntry.entries),
                Arb.int(0..100),
                Arb.int(1..8),
            ) { entry, start, length ->
                val question = texts.getValue(entry).question.replace(" ", "")
                val from = start % question.length
                val piece = question.substring(from, minOf(question.length, from + length))
                FaqSearch.matches(texts, piece) shouldContain entry
            }
        }

    private companion object {
        const val CALENDARS_FA = "تقویم‌ها"

        /** Mirrors the link table of docs/AUTOMATION.md (the parser itself lives in `:app`). */
        val AUTOMATION_LINK =
            Regex(
                "taqvim://(calendar|times|astronomy|settings(/[a-z]+(-[a-z]+)*)?|event/[1-9][0-9]*" +
                    "|day/[0-9]{1,4}-[0-9]{2}-[0-9]{2}(\\?calendar=(persian|islamic|gregorian))?" +
                    "|search\\?q=[^\\s]+|convert\\?date=[^\\s&]+(&from=(persian|islamic|gregorian))?)",
            )
    }
}
