/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.calendar

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.Test

/** T-802: what "share this day" puts on the clipboard, including the citations that make it worth sharing. */
class DayShareTextTest {
    private val sources = "Sources:"

    @Test
    fun `a sourced holiday carries its citation, page and URL`() {
        val text =
            DayShareText.build(
                dateLines = listOf("Friday 1 Farvardin 1405", "21 March 2026"),
                entries =
                    listOf(
                        DayShareText.Entry(
                            title = "Nowruz",
                            sources =
                                listOf(
                                    DayShareText.Source(
                                        title = "Official calendar of Iran 1405",
                                        page = "p. 1",
                                        url = "https://calendar.ut.ac.ir/",
                                    ),
                                ),
                        ),
                    ),
                sourcesHeading = sources,
            )

        text shouldBe
            """
            Friday 1 Farvardin 1405
            21 March 2026

            Nowruz
            Sources:
              Official calendar of Iran 1405 — p. 1
              https://calendar.ut.ac.ir/
            """.trimIndent()
    }

    @Test
    fun `an event with no citation is not dressed up as a sourced one`() {
        // A personal reminder has no primary source, and the whole value of this text is that a citation means
        // something. Printing an empty "Sources:" under one would quietly undermine that.
        val text =
            DayShareText.build(
                dateLines = listOf("21 March 2026"),
                entries = listOf(DayShareText.Entry("Call Maryam", sources = emptyList())),
                sourcesHeading = sources,
            )

        text shouldContain "Call Maryam"
        text shouldNotContain sources
    }

    @Test
    fun `a citation with no title or page falls back to its URL alone`() {
        val text =
            DayShareText.build(
                dateLines = listOf("21 March 2026"),
                entries =
                    listOf(
                        DayShareText.Entry(
                            "Nowruz",
                            listOf(DayShareText.Source(title = " ", page = null, url = "https://example.org/x")),
                        ),
                    ),
                sourcesHeading = sources,
            )

        text shouldContain "  https://example.org/x"
        // Not an empty dash-joined line followed by a stray URL.
        text shouldNotContain "—"
    }

    @Test
    fun `a day with no events is just its dates`() {
        DayShareText.build(listOf("21 March 2026"), emptyList(), sources) shouldBe "21 March 2026"
    }
}
