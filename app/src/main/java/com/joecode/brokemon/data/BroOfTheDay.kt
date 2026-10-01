package com.joecode.brokemon.data

import com.joecode.brokemon.data.model.Bro
import com.joecode.brokemon.domain.CheckOnBro
import java.time.LocalDate
import kotlin.random.Random

/**
 * One bro per day for the home-screen widget. The pick is saved so it stays
 * the same all day (even after you check in), and it never repeats yesterday's bro.
 */
suspend fun BroRepository.broOfTheDay(prefs: UserPrefs, today: LocalDate = LocalDate.now()): Bro? {
    val bros = allBrosOnce()
    if (bros.isEmpty()) return null
    val day = today.toEpochDay()
    val saved = prefs.broOfTheDay()
    if (saved != null && saved.first == day) bros.firstOrNull { it.id == saved.second }?.let { return it }
    val pick = CheckOnBro.recommend(bros, lastRecommendedId = saved?.second, random = Random(day)) ?: return null
    prefs.setBroOfTheDay(day, pick.id)
    return pick
}
