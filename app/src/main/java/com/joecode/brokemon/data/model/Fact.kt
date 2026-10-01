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
        "Favorite song",
        "Favorite game",
        "Favorite book",
        "Favorite food",
        "Sports team",
        "Hometown",
        "Birthday",
        "Go-to karaoke",
        "Hidden talent",
    )
}
