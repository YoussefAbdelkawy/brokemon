package com.joecode.brokemon.domain

import java.text.Normalizer
import kotlin.math.min

/**
 * Name search that forgives typos: "marcuss", "omr" and "layal" still find
 * Marcus, Omar and Layla. Exact substrings always match; otherwise every word
 * you typed has to be close to some word in the name.
 */
object FuzzySearch {

    fun matches(query: String, name: String): Boolean {
        val q = normalize(query)
        if (q.isBlank()) return true
        val n = normalize(name)
        if (n.contains(q)) return true
        val nameWords = n.split(' ').filter { it.isNotEmpty() }
        return q.split(' ').filter { it.isNotEmpty() }.all { word -> nameWords.any { close(word, it) } }
    }

    /** Close enough: a typo budget that grows with the word length, also against name prefixes. */
    private fun close(word: String, target: String): Boolean {
        if (target.startsWith(word)) return true
        val budget = when {
            word.length <= 2 -> 0
            word.length <= 4 -> 1
            else -> 2
        }
        if (budget == 0) return false
        if (distance(word, target) <= budget) return true
        // Typing the start of a long name with a typo: compare against its prefix of the same length.
        return target.length > word.length && distance(word, target.take(word.length)) <= budget
    }

    fun normalize(text: String): String =
        Normalizer.normalize(text.lowercase().trim(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .replace(Regex("[^\\p{L}\\p{N} ]"), " ")
            .replace(Regex(" +"), " ")
            .trim()

    /** Damerau-Levenshtein distance (a swapped pair of letters counts as one typo). */
    fun distance(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        val d = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) d[i][0] = i
        for (j in 0..b.length) d[0][j] = j
        for (i in 1..a.length) for (j in 1..b.length) {
            val cost = if (a[i - 1] == b[j - 1]) 0 else 1
            d[i][j] = min(min(d[i - 1][j] + 1, d[i][j - 1] + 1), d[i - 1][j - 1] + cost)
            if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) {
                d[i][j] = min(d[i][j], d[i - 2][j - 2] + 1)
            }
        }
        return d[a.length][b.length]
    }
}
