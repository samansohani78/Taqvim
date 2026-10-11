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

/** Dataset title coverage and representative display, independently of translation review status. */
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
        // Every shipped event has an English reference; a new record must supply one too.
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
    fun `German has translated names and otherwise uses the English reference`() {
        titleOf("ir.holiday.ashura", "de") shouldBe "Aschura"
        titleOf("ir.holiday.imam-reza-martyrdom", "de") shouldBe titleOf("ir.holiday.imam-reza-martyrdom", "en")
    }

    @Test
    fun `every event has both primary language titles`() {
        fallingBackIn("fa").shouldBeEmpty()
        fallingBackIn("en").shouldBeEmpty()
        titleOf("np.holiday.dashain", "en") shouldBe "Dashain holidays"
        titleOf("np.holiday.dashain", "fa") shouldBe "تعطیلات دشاین"
        titleOf("np.holiday.dashain", "ne") shouldBe "दशैं बिदा"
    }

    @Test
    fun `the earlier English exonym additions remain available`() {
        // The 18 exonyms of 2026-10-10 are transliterations, not quotations from the sources the records cite.
        // Review markers are checked against source JSON by tools/i18n; generated EventDefinition deliberately
        // does not carry editorial metadata. This assertion only checks the runtime titles.
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
        const val ENGLISH_TITLES = 329
        const val PERSIAN_TITLES = 329
    }
}
