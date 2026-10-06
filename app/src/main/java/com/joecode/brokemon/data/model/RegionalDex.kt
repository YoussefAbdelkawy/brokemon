package com.joecode.brokemon.data.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation

/** A user-made dex like "Uni Dex" with its own numbering. The National Dex (all bros) isn't stored. */
@Entity(tableName = "regional_dexes")
data class RegionalDex(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Index into a small color list, for the dex chip. */
    val colorIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
) {
    companion object {
        const val MAX_NAME = 20
    }
}

/** Many-to-many: one bro can be in several regional dexes, each with its own number. */
@Entity(
    tableName = "bro_dex_cross_ref",
    primaryKeys = ["broId", "dexId"],
    indices = [Index("dexId")],
    foreignKeys = [
        ForeignKey(entity = Bro::class, parentColumns = ["id"], childColumns = ["broId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = RegionalDex::class, parentColumns = ["id"], childColumns = ["dexId"], onDelete = ForeignKey.CASCADE),
    ],
)
data class BroDexCrossRef(
    val broId: Long,
    val dexId: Long,
    val regionalNumber: Int,
)

data class RegionalDexWithBros(
    @Embedded val dex: RegionalDex,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(BroDexCrossRef::class, parentColumn = "dexId", entityColumn = "broId"),
    )
    val bros: List<Bro>,
)
