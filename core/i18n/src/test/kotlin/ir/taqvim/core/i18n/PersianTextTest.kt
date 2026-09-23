/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.i18n

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.common.ExperimentalKotest
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.char
import io.kotest.property.arbitrary.choice
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll
import ir.taqvim.core.testing.PropertyTesting
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

class PersianTextTest {
    /**
     * Fixed seed: the same inputs, and so the same covered branches, on every run and machine. The opt-in is for
     * `iterations`, which Kotest 6 still marks experimental.
     */
    @OptIn(ExperimentalKotest::class)
    private val propertyConfig = PropTestConfig(seed = 20_260_920L, iterations = PropertyTesting.iterations)

    private val zwnj = "‌"

    /** 50 normalization pairs: input → expected canonical form. */
    private val normalizationPairs =
        listOf(
            "علي" to "علی",
            "مكه" to "مکه",
            "كتاب" to "کتاب",
            "يلدا" to "یلدا",
            "موسى" to "موسی",
            "كيان" to "کیان",
            "تحويل سال" to "تحویل سال",
            "مـــحــرم" to "محرم",
            "نــوروز" to "نوروز",
            "عـيد" to "عید",
            "مُحَمَّد" to "محمد",
            "رَمَضان" to "رمضان",
            "قُرآن" to "قرآن",
            "اِعتدال" to "اعتدال",
            "بِسْمِ" to "بسم",
            "صَلاة" to "صلاة",
            "رحمٰن" to "رحمن",
            "شبِ یلدا" to "شب یلدا",
            "می${zwnj}روم" to "می روم",
            "نیم${zwnj}سال" to "نیم سال",
            "خانه${zwnj}ها" to "خانه ها",
            "روز  ملی" to "روز ملی",
            "  تقویم  " to "تقویم",
            "\tجشن\nسده" to "جشن سده",
            "سال‍نو" to "سالنو",
            "یک$zwnj ${zwnj}دو" to "یک دو",
            "${zwnj}آغاز" to "آغاز",
            "پایان$zwnj" to "پایان",
            "عيد فطر" to "عید فطر",
            "عيد قربان" to "عید قربان",
            "عيد غدير" to "عید غدیر",
            "ولادت امام علي" to "ولادت امام علی",
            "روز جهاني كار" to "روز جهانی کار",
            "شهادت امام كاظم" to "شهادت امام کاظم",
            "كريسمس" to "کریسمس",
            "يكشنبه" to "یکشنبه",
            "دي" to "دی",
            "مهرگان" to "مهرگان",
            "Nowruz" to "Nowruz",
            "Eid al-Fitr" to "Eid al-Fitr",
            "1405" to "1405",
            "۱۴۰۵" to "۱۴۰۵",
            "" to "",
            "   " to "",
            "a‌b" to "a b",
            "ﻻ" to "ﻻ",
            "كٌ" to "ک",
            "ــ" to "",
            "يَ" to "ی",
            "ى" to "ی",
        )

    /** 50 fuzzy pairs: query, candidate, expected match. */
    private val fuzzyPairs =
        listOf(
            Triple("نوروز", "نوروز", true),
            Triple("نوريز", "نوروز", true),
            Triple("نورز", "نوروز", true),
            Triple("نو روز", "نوروز", true),
            Triple("نو${zwnj}روز", "نوروز", true),
            Triple("nowruz", "Nowruz", true),
            Triple("nowrouz", "Nowruz", true),
            Triple("norooz", "nowruz", false),
            Triple("noruz", "Nowruz", true),
            Triple("يلدا", "یلدا", true),
            Triple("یلذا", "یلدا", true),
            Triple("شب يلدا", "شب یلدا", true),
            Triple("علي", "علی", true),
            Triple("امام علی", "امام علي", true),
            Triple("مكه", "مکه", true),
            Triple("كتاب", "کتاب", true),
            Triple("کتبا", "کتاب", true),
            Triple("قرآن", "قران", true),
            Triple("أحمد", "احمد", true),
            Triple("إسلام", "اسلام", true),
            Triple("مؤمن", "مومن", true),
            Triple("رئیس", "رییس", true),
            Triple("عيد فطر", "عید فطر", true),
            Triple("عیدفطر", "عید فطر", true),
            Triple("عید قربان", "عید فطر", false),
            Triple("رمضان", "رمضان", true),
            Triple("رمضن", "رمضان", true),
            Triple("رمض", "رمضان", true),
            Triple("رم", "رمضان", false),
            Triple("محرم", "مـحـرم", true),
            Triple("محرّم", "محرم", true),
            Triple("تاسوعا", "عاشورا", false),
            Triple("christmas", "Christmas", true),
            Triple("chirstmas", "christmas", true),
            Triple("xmas", "christmas", false),
            Triple("eid", "Eid", true),
            Triple("eidd", "eid", true),
            Triple("ied", "eid", true),
            Triple("mehregan", "Mehrgan", true),
            Triple("sadeh", "Sade", true),
            Triple("yalda", "Yalda", true),
            Triple("yaldaa", "yalda", true),
            Triple("yld", "yalda", true),
            Triple("y", "yalda", false),
            Triple("", "", true),
            Triple("", "ab", true),
            Triple("", "abc", false),
            Triple("abcd", "badc", true),
            Triple("abcdef", "abcfed", true),
            Triple("سپندارمذگان", "اسفندگان", false),
        )

    @TestFactory
    fun `normalization pairs`() =
        normalizationPairs.mapIndexed { index, (input, expected) ->
            DynamicTest.dynamicTest("normalize #$index") { PersianText.normalize(input) shouldBe expected }
        }

    @TestFactory
    fun `fuzzy pairs`() =
        fuzzyPairs.mapIndexed { index, (query, candidate, expected) ->
            DynamicTest.dynamicTest("match #$index") { FuzzyMatcher.matches(query, candidate) shouldBe expected }
        }

    @Test
    fun `search keys fold hamza carriers and spaces`() {
        PersianText.searchKey("آب  أنار") shouldBe "ابانار"
        PersianText.searchKey("Nowruz ${zwnj}Day") shouldBe "nowruzday"
    }

    @Test
    fun `distance counts edits and stops early`() {
        FuzzyMatcher.distance("kitten", "sitting", maxDistance = 5) shouldBe 3
        FuzzyMatcher.distance("ca", "ac") shouldBe 1
        FuzzyMatcher.distance("abc", "abc") shouldBe 0
        FuzzyMatcher.distance("abcdef", "uvwxyz") shouldBe 3
        FuzzyMatcher.distance("a", "abcdef") shouldBe 3
        FuzzyMatcher.distance("abc", "xyz", maxDistance = 0) shouldBe 1
        shouldThrow<IllegalArgumentException> { FuzzyMatcher.distance("a", "b", maxDistance = -1) }
    }

    private val persianish =
        Arb
            .list(
                Arb.choice(
                    Arb.char('ء'..'ي'),
                    Arb.char('ً'..'ٰ'),
                    Arb.element(listOf('ک', 'ی', 'ـ', '‌', '‍', ' ', '\t', '\n', 'a', 'Z')),
                ),
                0..24,
            ).map { it.joinToString("") }

    @Test
    fun `normalization and search keys are idempotent`(): Unit =
        runBlocking {
            checkAll(propertyConfig, persianish) { text ->
                val normalized = PersianText.normalize(text)
                PersianText.normalize(normalized) shouldBe normalized
                val key = PersianText.searchKey(text)
                PersianText.searchKey(key) shouldBe key
            }
        }

    @Test
    fun `distance is symmetric and zero only for equal strings`(): Unit =
        runBlocking {
            checkAll(propertyConfig, persianish, persianish) { a, b ->
                val forward = FuzzyMatcher.distance(a, b, maxDistance = 30)
                forward shouldBe FuzzyMatcher.distance(b, a, maxDistance = 30)
                (forward == 0) shouldBe (a == b)
            }
        }
}
