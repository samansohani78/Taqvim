/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.location

import androidx.test.ext.junit.runners.AndroidJUnit4
import ir.taqvim.core.i18n.LanguageTable
import ir.taqvim.core.model.Coordinates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * T-603 (U, on the ICU of a Robolectric device): city names sort by each language's collation. Expectations follow
 * the alphabets and the Unicode Collation Algorithm levels (case and accents are secondary/tertiary differences unless
 * a language tailors them into letters); each input is chosen so that plain code-point order gives a different result.
 */
@RunWith(AndroidJUnit4::class)
class CityCollationTest {
    private fun assertSorted(
        code: String,
        expected: List<String>,
    ) {
        val language = checkNotNull(LanguageTable.forCode(code)) { "no language $code" }
        val input = expected.reversed()
        val cities =
            input.mapIndexed { index, name ->
                val localized = if (code == City.ENGLISH) emptyMap() else mapOf(code to name)
                City(index.toLong(), name, localized, null, null, Coordinates(0.0, 0.0), null, null)
            }

        assertNotEquals(expected, input.sorted())
        assertEquals(expected, CityCatalog(cities).sortedByName(language).map { it.name(code) })
    }

    @Test
    fun englishIgnoresCaseAtThePrimaryLevel() {
        assertSorted("en", listOf("apple", "Banana", "cherry"))
    }

    @Test
    fun germanSortsUmlautsWithTheirBaseLetters() {
        assertSorted("de", listOf("Überlingen", "Ulm", "Vaduz"))
    }

    @Test
    fun turkishTreatsCedillaAndDiaeresisLettersAsSeparateLetters() {
        assertSorted("tr", listOf("Cizre", "Çorum", "Denizli"))
        // o < ö < p in the Turkish alphabet; untailored collation would put Ödemiş before Ordu.
        assertSorted("tr", listOf("Ordu", "Ödemiş", "Pazar"))
    }

    @Test
    fun persianFollowsThePersianAlphabet() {
        // ب < پ < ق < ک < گ < ه < ی
        assertSorted("fa", listOf("بابل", "پاوه", "قم", "کرج", "گرگان", "همدان", "یزد"))
    }

    @Test
    fun arabicIgnoresHarakatAtThePrimaryLevel() {
        // The fatha on the first name is a secondary difference, so غ < ي decides.
        assertSorted("ar", listOf("بَغداد", "بيروت", "تونس"))
    }
}
