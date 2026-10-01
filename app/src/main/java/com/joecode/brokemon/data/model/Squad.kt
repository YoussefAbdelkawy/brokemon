package com.joecode.brokemon.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "squads")
data class Squad(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val memberIds: List<Long> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
) {
    companion object {
        const val MAX_MEMBERS = 6
    }
}
