package com.joecode.brokemon.data.model

import java.util.UUID

/** A flat "category: value" entry, e.g. "Favorite song: Mr. Brightside". */
data class Fact(
    val id: String = UUID.randomUUID().toString(),
    val category: String,
    val value: String,
)

object FactCategories {
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
