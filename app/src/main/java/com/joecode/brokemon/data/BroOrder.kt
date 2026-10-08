package com.joecode.brokemon.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The order of bros in the dex view the user is looking at (current dex, search
 * and filters, in their current sort). The card detail screen swipes through it.
 */
class BroOrder {
    private val _ids = MutableStateFlow<List<Long>>(emptyList())
    val ids: StateFlow<List<Long>> = _ids.asStateFlow()

    fun set(ids: List<Long>) {
        if (_ids.value != ids) _ids.value = ids
    }

    /** The neighbor of [current] in direction [step] (+1 next, -1 previous), wrapping around. Null if there's nowhere to go. */
    fun neighbor(current: Long, step: Int): Long? {
        val list = _ids.value
        val i = list.indexOf(current)
        if (i < 0 || list.size < 2) return null
        return list[(i + step).mod(list.size)]
    }

    /** "3 / 12" position, or null when [current] isn't in the list. */
    fun position(current: Long): Pair<Int, Int>? {
        val list = _ids.value
        val i = list.indexOf(current)
        return if (i < 0) null else (i + 1) to list.size
    }
}
