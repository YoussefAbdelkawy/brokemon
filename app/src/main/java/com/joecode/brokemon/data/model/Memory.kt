package com.joecode.brokemon.data.model

import java.util.UUID

/**
 * A photo or video tied to a bro. [fileUri] always points into app-private
 * storage (filesDir/memories), never into the shared gallery.
 */
data class Memory(
    val id: String = UUID.randomUUID().toString(),
    val fileUri: String,
    val mediaType: MediaType,
    val caption: String = "",
    val date: Long = System.currentTimeMillis(),
)
