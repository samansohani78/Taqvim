/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.nlp

import ir.taqvim.core.i18n.FormatTable
import ir.taqvim.core.i18n.LanguageTable
import ir.taqvim.core.i18n.Numerals
import ir.taqvim.core.i18n.PersianText
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Weekday
import java.util.Properties

/** Word classes of `lexicon.properties`. */
internal enum class Concept(
    val property: String,
) {
    TODAY("today"),
    TOMORROW("tomorrow"),
    DAY_AFTER_TOMORROW("dayAfterTomorrow"),
    YESTERDAY("yesterday"),
    DAY_BEFORE_YESTERDAY("dayBeforeYesterday"),
    DAY("unit.day"),
    WEEK("unit.week"),
    MONTH("unit.month"),
    YEAR("unit.year"),
    FUTURE("future"),
    PAST("past"),
    IN("in"),
    NEXT("next"),
    LAST("last"),
    THIS("this"),
    ANCHOR_BEFORE("anchorBefore"),
    ANCHOR_AFTER("anchorAfter"),
    RANGE_FROM("rangeFrom"),
    RANGE_TO("rangeTo"),
}

/**
 * Vocabulary of the grammar: the relative/range word list in `lexicon.properties` (fa and en), and month names,
 * weekday names and calendar abbreviations of all launch languages from `:core:i18n` (CLDR data, T-200/T-202).
 */
internal object Lexicon {
    private const val RESOURCE = "/ir/taqvim/core/nlp/lexicon.properties"
    private const val MAX_PHRASE_WORDS = 5

    /** Characters that may join the words of one name, as in `Dhuʻl-Hijjah`. */
    private val JOINERS = setOf("-", "'", "’")
    private const val MAX_MARKER_TOKENS = 5
    private const val NUMBER_WORDS = 10
    private val NAMED_CALENDARS = listOf(CalendarSystem.PERSIAN, CalendarSystem.ISLAMIC, CalendarSystem.GREGORIAN)

    private val entries: Map<String, String> by lazy {
        val stream = requireNotNull(Lexicon::class.java.getResourceAsStream(RESOURCE)) { "missing $RESOURCE" }
        stream.reader(Charsets.UTF_8).use { reader ->
            Properties().apply { load(reader) }.let { loaded ->
                loaded.stringPropertyNames().associateWith { loaded.getProperty(it) }
            }
        }
    }

    private val concepts: Map<Concept, Set<String>> by lazy { Concept.entries.associateWith { keysOf(it.property) } }

    private val numberWords: Map<String, Int> by lazy {
        (1..NUMBER_WORDS).flatMap { value -> keysOf("number.$value").map { it to value } }.toMap()
    }

    private val calendarMarkers: Map<String, CalendarSystem> by lazy {
        val eras =
            FormatTable.formats.values.flatMap { formats ->
                formats.eras.map { (system, era) ->
                    key(era) to
                        system
                }
            }
        val listed = CalendarSystem.entries.flatMap { system -> keysOf("calendar.${system.name}").map { it to system } }
        (eras + listed).filter { it.first.isNotEmpty() }.toMap()
    }

    private val monthNames: Map<String, Set<Pair<CalendarSystem, Int>>> by lazy {
        val standalone =
            LanguageTable.languages.flatMap { language ->
                NAMED_CALENDARS.flatMap { system -> named(language.monthNames.forSystem(system).orEmpty(), system) }
            }
        val format =
            FormatTable.formats.values.flatMap { formats ->
                NAMED_CALENDARS.flatMap { system -> named(formats.monthNames[system].orEmpty(), system) }
            }
        (standalone + format)
            .filter { (name, _) -> name.length > 1 && name.none { Numerals.digitValue(it) != null } }
            .groupBy({ it.first }, { it.second })
            .mapValues { it.value.toSet() }
    }

    private val weekdays: Map<String, Set<Weekday>> by lazy {
        FormatTable.formats.values
            .flatMap { formats ->
                formats.weekdays.values.flatMap { names ->
                    names.mapIndexed { i, name ->
                        key(name) to
                            Weekday.entries[i]
                    }
                }
            }.groupBy({ it.first }, { it.second })
            .mapValues { it.value.toSet() }
    }

    /** Matching key: letters and digits of [text] through [PersianText.searchKey]. */
    fun key(text: String): String =
        PersianText.searchKey(
            text
                .map {
                    if (Tokenizer.isWordChar(it) ||
                        it.isDigit()
                    ) {
                        it
                    } else {
                        ' '
                    }
                }.joinToString(""),
        )

    /** Width in words of the longest phrase of [concept] starting at token [index], or 0. */
    fun concept(
        tokens: List<Token>,
        index: Int,
        concept: Concept,
    ): Int = matchWords(tokens, index, MAX_PHRASE_WORDS) { it in concepts.getValue(concept) }

    /** The value of a number token or number word at [index]. */
    fun number(
        tokens: List<Token>,
        index: Int,
    ): Int? {
        val token = tokens.getOrNull(index)
        return token?.number ?: token?.takeIf { it.type == TokenType.WORD }?.let { numberWords[it.key] }
    }

    /** Calendars and month numbers of a month name at [index], with its width in words. */
    fun month(
        tokens: List<Token>,
        index: Int,
    ): Pair<Set<Pair<CalendarSystem, Int>>, Int>? {
        val width = matchWords(tokens, index, MAX_PHRASE_WORDS) { it in monthNames }
        return if (width == 0) null else monthNames.getValue(joinedKey(tokens.subList(index, index + width))) to width
    }

    /** Weekdays a word token can name. */
    fun weekday(token: Token?): Set<Weekday> =
        token?.takeIf { it.type == TokenType.WORD }?.let { weekdays[it.key] }.orEmpty()

    /** A calendar abbreviation (e.g. `AP`, `ه‍.ق.`) starting at [index], with its width in tokens. */
    fun calendarMarker(
        tokens: List<Token>,
        index: Int,
    ): Pair<CalendarSystem, Int>? {
        val width =
            (minOf(MAX_MARKER_TOKENS, tokens.size - index) downTo 1).firstOrNull { width ->
                val window = tokens.subList(index, index + width)
                window.first().type == TokenType.WORD &&
                    window.all { it.type == TokenType.WORD || it.raw == "." } &&
                    joinedKey(window) in calendarMarkers
            }
        return width?.let { calendarMarkers.getValue(joinedKey(tokens.subList(index, index + it))) to it }
    }

    private fun keysOf(property: String): Set<String> =
        entries[property]
            .orEmpty()
            .split('|')
            .map(::key)
            .filter { it.isNotEmpty() }
            .toSet()

    private fun named(
        names: List<String>,
        system: CalendarSystem,
    ): List<Pair<String, Pair<CalendarSystem, Int>>> = names.mapIndexed { i, name -> key(name) to (system to i + 1) }

    private fun joinedKey(tokens: List<Token>): String =
        tokens
            .filter {
                it.type == TokenType.WORD
            }.joinToString("") { it.key }

    private fun matchWords(
        tokens: List<Token>,
        index: Int,
        maxWords: Int,
        accept: (String) -> Boolean,
    ): Int =
        if (index < 0) {
            0
        } else {
            (minOf(maxWords, tokens.size - index) downTo 1).firstOrNull { width ->
                val window = tokens.subList(index, index + width)
                val bounded = window.first().type == TokenType.WORD && window.last().type == TokenType.WORD
                bounded && window.all { it.type == TokenType.WORD || it.raw in JOINERS } && accept(joinedKey(window))
            } ?: 0
        }
}
