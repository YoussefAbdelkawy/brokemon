package com.joecode.brokemon.data.model

import java.util.UUID

/** A flat "category: value" entry, e.g. "Favorite song: Mr. Brightside". */
data class Fact(
    val id: String = UUID.randomUUID().toString(),
    val category: String,
    val value: String,
    /** For date facts like Birthday: "MM-dd", used for reminders. Null for plain text facts. */
    val monthDay: String? = null,
)

object FactCategories {
    const val BIRTHDAY = "Birthday"

    val presets = listOf(
        "Birthday",
        "Favorite song",
        "Favorite game",
        "Favorite food",
        "Go-to drink",
        "Sports team",
        "Hometown",
        "Job",
        "Car",
        "Pet's name",
        "Celebrity crush",
        "Go-to karaoke",
        "Hidden talent",
        "Biggest fear",
    )
}
