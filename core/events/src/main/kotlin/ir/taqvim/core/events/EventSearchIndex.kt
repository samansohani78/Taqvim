/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.events

import ir.taqvim.core.i18n.FuzzyMatcher
import ir.taqvim.core.i18n.PersianText

/**
 * In-memory search over event titles in every language and aliases (T-304). Texts are compared by
 * [PersianText.searchKey], so Arabic/Persian letter variants, diacritics, ZWNJ, spaces and letter case do not matter.
 *
 * Exact, prefix and substring matches come from a sorted table of key suffixes (binary search). Typo-tolerant matches
 * come from an index of single-character deletions of every key and word key: a candidate shares at least one
 * deletion variant with the query and is then verified with [FuzzyMatcher.distance]. This finds every match within one
 * edit and the two-edit matches that share a deletion (e.g. one insertion plus one deletion). Fuzzy matching runs only
 * for queries of at least [MIN_FUZZY_LENGTH] characters (two edits from [LONG_QUERY_LENGTH]) and only while fewer
 * than `limit` stronger hits were found.
 *
 * Ranking: exact > prefix (shorter completions first) > substring (earlier position first) > fuzzy (fewer edits
 * first); ties go to holidays, then to the event id. Each event appears at most once, with its best match.
 */
public class EventSearchIndex(
    events: Collection<EventDefinition>,
) {
    private val definitions: List<EventDefinition> = events.toList()
    private val texts: List<IndexedText> =
        definitions
            .flatMapIndexed { index, definition ->
                (definition.title.texts.values + definition.aliases).map { text ->
                    IndexedText(index, text, PersianText.searchKey(text))
                }
            }.filter { it.key.isNotEmpty() }
    private val suffixes: List<Suffix> =
        texts
            .flatMapIndexed { textIndex, text ->
                text.key.indices.map { offset -> Suffix(text.key.substring(offset), textIndex, offset) }
            }.sortedBy { it.value }
    private val terms: List<FuzzyTerm> =
        texts.flatMapIndexed { textIndex, text ->
            (wordKeys(text.text) + text.key).distinct().map { FuzzyTerm(it, textIndex) }
        }
    private val deletionIndex: Map<String, List<Int>> =
        terms
            .flatMapIndexed { termIndex, term -> deletionVariants(term.key).map { it to termIndex } }
            .groupBy({ it.first }, { it.second })

    /** Hits for [query], best first. Blank queries (after normalization) have no hits. */
    public fun search(query: SearchQuery): List<SearchHit> {
        val key = PersianText.searchKey(query.text)
        if (key.isEmpty()) return emptyList()
        val best = HashMap<Int, SearchHit>()
        literalCandidates(key).forEach { offer(best, it, query) }
        if (key.length >= MIN_FUZZY_LENGTH && best.size < query.limit) {
            fuzzyCandidates(key).forEach { offer(best, it, query) }
        }
        return best.values.sortedWith(RANKING).take(query.limit)
    }

    private fun literalCandidates(key: String): List<Candidate> {
        val candidates = mutableListOf<Candidate>()
        var position = lowerBound(key)
        while (position < suffixes.size && suffixes[position].value.startsWith(key)) {
            val suffix = suffixes[position]
            val textLength = texts[suffix.textIndex].key.length
            candidates +=
                when {
                    suffix.offset > 0 -> {
                        Candidate(suffix.textIndex, MatchKind.SUBSTRING, SUBSTRING_SCORE - band(suffix.offset))
                    }

                    textLength == key.length -> {
                        Candidate(suffix.textIndex, MatchKind.EXACT, EXACT_SCORE)
                    }

                    else -> {
                        Candidate(suffix.textIndex, MatchKind.PREFIX, PREFIX_SCORE - band(textLength - key.length))
                    }
                }
            position++
        }
        return candidates
    }

    private fun fuzzyCandidates(key: String): List<Candidate> {
        val maxDistance = if (key.length >= LONG_QUERY_LENGTH) FuzzyMatcher.DEFAULT_MAX_DISTANCE else 1
        return deletionVariants(key)
            .flatMap { deletionIndex[it].orEmpty() }
            .distinct()
            .mapNotNull { termIndex ->
                val term = terms[termIndex]
                val distance = FuzzyMatcher.distance(key, term.key, maxDistance)
                val score = FUZZY_SCORE - distance * EDIT_PENALTY
                if (distance > maxDistance) null else Candidate(term.textIndex, MatchKind.FUZZY, score)
            }
    }

    private fun offer(
        best: MutableMap<Int, SearchHit>,
        candidate: Candidate,
        query: SearchQuery,
    ) {
        val text = texts[candidate.textIndex]
        val definition = definitions[text.definitionIndex]
        val current = best[text.definitionIndex]
        if (query.accepts(definition) && (current == null || candidate.score > current.score)) {
            best[text.definitionIndex] = SearchHit(definition, text.text, candidate.kind, candidate.score)
        }
    }

    private fun lowerBound(key: String): Int {
        var low = 0
        var high = suffixes.size
        while (low < high) {
            val middle = (low + high) ushr 1
            if (suffixes[middle].value < key) low = middle + 1 else high = middle
        }
        return low
    }

    private data class IndexedText(
        val definitionIndex: Int,
        val text: String,
        val key: String,
    )

    private data class Suffix(
        val value: String,
        val textIndex: Int,
        val offset: Int,
    )

    private data class FuzzyTerm(
        val key: String,
        val textIndex: Int,
    )

    private data class Candidate(
        val textIndex: Int,
        val kind: MatchKind,
        val score: Int,
    )

    public companion object {
        /** Shortest query (in normalized characters) that is matched with typos. */
        public const val MIN_FUZZY_LENGTH: Int = 4

        /** Shortest query that tolerates two edits instead of one. */
        public const val LONG_QUERY_LENGTH: Int = 5

        private const val EXACT_SCORE = 4_000
        private const val PREFIX_SCORE = 3_000
        private const val SUBSTRING_SCORE = 2_000
        private const val FUZZY_SCORE = 1_000
        private const val EDIT_PENALTY = 100
        private const val MAX_PENALTY = 999

        private val RANKING: Comparator<SearchHit> =
            compareByDescending<SearchHit> { it.score }
                .thenByDescending { it.definition.isHoliday }
                .thenBy { it.definition.id.value }

        private fun band(penalty: Int): Int = minOf(penalty, MAX_PENALTY)

        /** [key] itself and every string made by deleting one of its characters. */
        private fun deletionVariants(key: String): List<String> =
            listOf(key) + key.indices.map { key.removeRange(it, it + 1) }

        private fun wordKeys(text: String): List<String> =
            PersianText
                .normalize(text)
                .split(' ')
                .map(PersianText::searchKey)
                .filter { it.isNotEmpty() }
    }
}
