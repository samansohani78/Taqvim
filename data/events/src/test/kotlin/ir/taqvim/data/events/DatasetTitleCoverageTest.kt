/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.events

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import ir.taqvim.core.events.EventDefinition
import ir.taqvim.data.events.generated.OfficialEvents
import org.junit.jupiter.api.Test

/**
 * What a reader actually sees for an observance's name, and how much of the dataset is still Persian for them.
 *
 * `LocalizedText.forLanguage` falls back `requested → fa → ne`, so a language with no title for a record shows
 * Persian text inside an otherwise translated screen. That fallback is deliberate — an observance with no name is
 * worse than one named in the wrong language, and inventing names is not on the table (docs/i18n/DATASET_TITLE_DEBT.md
 * says why) — but it is debt, and debt that nothing measures is debt that grows.
 *
 * So the counts below are a ratchet, not an approval. They may only improve, and improving them means editing the
 * number here and the table in the debt document together, which is the point: the document cannot drift away from
 * what the dataset really holds.
 */
class DatasetTitleCoverageTest {
    private val all: List<EventDefinition> = OfficialEvents.ALL

    private fun withTitleIn(language: String) = all.filter { language in it.title.texts }

    private fun fallingBackIn(language: String) = all.filterNot { language in it.title.texts }

    @Test
    fun `the dataset is the size the debt document says it is`() {
        all.size shouldBe RECORDS
    }

    @Test
    fun `English coverage only improves`() {
        // 280 of 329 on 2026-10-10. A lower number than this means titles were removed; a higher one means the
        // debt shrank, and docs/i18n/DATASET_TITLE_DEBT.md has to say so.
        val present = withTitleIn("en").size

        (present >= ENGLISH_TITLES) shouldBe true
        if (present == ENGLISH_TITLES) fallingBackIn("en").size shouldBe RECORDS - ENGLISH_TITLES
    }

    @Test
    fun `Persian coverage only improves`() {
        val present = withTitleIn("fa").size

        (present >= PERSIAN_TITLES) shouldBe true
    }

    @Test
    fun `the observances everyone knows by name read in English`() {
        // Representative rendering: these are the ones a reader is most likely to meet, and until 2026-10-10 every
        // one of them showed Persian to an English reader.
        val named =
            mapOf(
                "ir.holiday.nowruz-1" to "Nowruz",
                "ir.holiday.eid-al-fitr" to "Eid al-Fitr",
                "ir.holiday.eid-al-adha" to "Eid al-Adha",
                "ir.holiday.ashura" to "Ashura",
                "ir.holiday.arbaeen" to "Arbaeen",
                "af.holiday.arafa" to "Day of Arafah",
            )

        named.forEach { (id, english) ->
            titleOf(id, "en") shouldBe english
        }
    }

    @Test
    fun `the same observances read in Persian for a Persian reader`() {
        titleOf("ir.holiday.eid-al-fitr", "fa") shouldBe "عید سعید فطر"
        titleOf("ir.holiday.ashura", "fa") shouldBe "عاشورای حسینی"
        titleOf("ir.holiday.nowruz-1", "fa") shouldBe "آغاز نوروز"
    }

    @Test
    fun `a language with no title of its own falls back to Persian, and that is visible here`() {
        // German has no dataset titles at all, so this is what a German reader sees. Asserted so that the day it
        // stops being true, somebody has to come here and say so.
        titleOf("ir.holiday.ashura", "de") shouldBe titleOf("ir.holiday.ashura", "fa")
        withTitleIn("de").shouldBeEmpty()
    }

    @Test
    fun `the Nepali records are the ones with no Persian title, and they fall back to Nepali`() {
        val withoutPersian = fallingBackIn("fa")

        withoutPersian.size shouldBe RECORDS - PERSIAN_TITLES
        withoutPersian.forEach { definition ->
            // Not an accident of the data: LocalizedText requires a Persian or a Nepali text, so these must have
            // Nepali, and a reader of any language sees Nepali for them.
            ("ne" in definition.title.texts) shouldBe true
            definition.title.forLanguage("en") shouldBe definition.title.texts.getValue("ne")
        }
    }

    @Test
    fun `every English title added without a cited English source is marked for review`() {
        // The 18 exonyms of 2026-10-10 are transliterations, not quotations from the sources the records cite.
        // They carry titleReview so nothing in the dataset claims they were reviewed. The dataset validator owns
        // the marker itself; this checks the records it was added for still have an English title to review.
        val exonyms =
            listOf(
                "ir.holiday.nowruz-1",
                "ir.holiday.eid-al-fitr",
                "ir.holiday.eid-al-ghadir",
                "ir.holiday.tasua",
                "af.holiday.eid-al-fitr.1",
            )

        all.filter { it.id.value in exonyms }.map { it.id.value } shouldContainAll exonyms
        exonyms.forEach { id -> ("en" in definitionOf(id).title.texts) shouldBe true }
    }

    private fun definitionOf(id: String): EventDefinition = all.first { it.id.value == id }

    private fun titleOf(
        id: String,
        language: String,
    ): String = definitionOf(id).title.forLanguage(language)

    private companion object {
        const val RECORDS = 329
        const val ENGLISH_TITLES = 280
        const val PERSIAN_TITLES = 299
    }
}
