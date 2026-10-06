package com.keeptrack.timeclicker.data

import java.time.Instant

data class Tracker(
    val id: Long,
    val name: String,
    val lastDoneAt: Instant,
    val groupId: Long?,
    val color: TileColor,
    val icon: TileIcon,
    val size: TileSize,
    /** File name inside the app's photo folder, or null for a colour tile. */
    val photo: String?,
    /** Times the tile was pressed since it was created or its counter was reset. */
    val pressCount: Int = 0,
)

data class TrackerGroup(
    val id: Long,
    val name: String,
)

/** Everything the user can edit on a tile. */
data class TileSpec(
    val name: String,
    val groupId: Long?,
    val color: TileColor,
    val icon: TileIcon,
    val size: TileSize,
    val photo: String?,
)

/** Stored by [key]; unknown keys (e.g. from a newer app version) fall back to the first entry. */
enum class TileColor(val key: String) {
    SAGE("sage"), LAVENDER("lavender"), PEACH("peach"), SKY("sky"), BUTTER("butter"), ROSE("rose"),

    /** Material You: follow the system colours on Android 12+. */
    PRIMARY("primary"), SECONDARY("secondary"), TERTIARY("tertiary");

    companion object {
        /** The colours offered when editing a tile. System colours are no longer offered; tiles using them keep them. */
        val pickable: List<TileColor> = entries.filter { it.ordinal < PRIMARY.ordinal }

        fun fromKey(key: String?) = entries.firstOrNull { it.key == key } ?: SAGE
    }
}

enum class TileSize(val key: String, val columns: Int, val rows: Int) {
    SMALL("small", 1, 1), WIDE("wide", 2, 1), TALL("tall", 1, 2);

    companion object {
        fun fromKey(key: String?) = entries.firstOrNull { it.key == key } ?: SMALL
    }
}

enum class TileIcon(val key: String) {
    /** No icon: the tile shows only its text. */
    NONE("none"),
    CHECK("check"), DROP("drop"), LEAF("leaf"), GRASS("grass"), FLOWER("flower"),
    BED("bed"), COFFEE("coffee"), SNOW("snow"), TRASH("trash"), CART("cart"),
    PHONE("phone"), HEART("heart"), PILL("pill"), PAW("paw"), SCISSORS("scissors"),
    BRUSH("brush"), GAUGE("gauge"), CAR("car");

    companion object {
        fun fromKey(key: String?) = entries.firstOrNull { it.key == key } ?: CHECK
    }
}
