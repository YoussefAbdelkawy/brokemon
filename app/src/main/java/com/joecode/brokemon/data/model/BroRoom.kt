package com.joecode.brokemon.data.model

/**
 * A bro's decoratable room. Like [BroLook], every field is an index into an
 * option list in [RoomOptions] (append-only), stored as a small list of ints.
 */
data class BroRoom(
    val wallpaper: Int = 0,
    val floor: Int = 0,
    val wallLeft: Int = 1,
    val wallRight: Int = 6,
    val floorLeft: Int = 1,
    val seat: Int = 1,
    val floorRight: Int = 3,
    val rug: Int = 1,
) {
    fun toList(): List<Int> = listOf(wallpaper, floor, wallLeft, wallRight, floorLeft, seat, floorRight, rug)

    operator fun get(part: RoomPart): Int = toList()[part.ordinal]

    fun with(part: RoomPart, index: Int): BroRoom {
        val values = toList().toMutableList()
        values[part.ordinal] = index.mod(part.count)
        return fromList(values)
    }

    companion object {
        fun fromList(values: List<Int>): BroRoom {
            val d = BroRoom().toList()
            fun at(part: RoomPart) = values.getOrElse(part.ordinal) { d[part.ordinal] }.mod(part.count)
            return BroRoom(
                wallpaper = at(RoomPart.WALLPAPER),
                floor = at(RoomPart.FLOOR),
                wallLeft = at(RoomPart.WALL_LEFT),
                wallRight = at(RoomPart.WALL_RIGHT),
                floorLeft = at(RoomPart.FLOOR_LEFT),
                seat = at(RoomPart.SEAT),
                floorRight = at(RoomPart.FLOOR_RIGHT),
                rug = at(RoomPart.RUG),
            )
        }
    }
}

enum class RoomPart(val label: String, val options: List<String>) {
    WALLPAPER("Wall", RoomOptions.wallpapers),
    FLOOR("Floor", RoomOptions.floors),
    WALL_LEFT("Wall left", RoomOptions.wallItems),
    WALL_RIGHT("Wall right", RoomOptions.wallItems),
    FLOOR_LEFT("Corner left", RoomOptions.floorItems),
    SEAT("Seat", RoomOptions.seats),
    FLOOR_RIGHT("Corner right", RoomOptions.floorItems),
    RUG("Rug", RoomOptions.rugs);

    val count: Int get() = options.size
}

object RoomOptions {
    val wallpapers = listOf("Midnight", "Brick", "Teal", "Cream", "Stars", "Green")
    val floors = listOf("Wood", "Tiles", "Carpet", "Concrete")
    val wallItems = listOf("Nothing", "Game poster", "Pennant", "Manga shelf", "Wall TV", "Neon sign", "Clock", "Fairy lights", "Lantern")
    val seats = listOf("Stand", "Couch", "Gaming chair", "Bean bag", "Floor cushion")
    val floorItems = listOf("Nothing", "Plant", "Lamp", "Manga tower", "Gaming desk", "Mini fridge", "Guitar", "Dumbbells", "Speaker")
    val rugs = listOf("No rug", "Red rug", "Blue rug", "Kilim")
}
