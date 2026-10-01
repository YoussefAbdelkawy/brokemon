package com.joecode.brokemon.data.model

/** Stored by name inside the memories JSON, so new values can be appended safely. */
enum class MediaType(val extension: String) {
    PHOTO("jpg"),
    VIDEO("mp4"),
    AUDIO("m4a"),
}
