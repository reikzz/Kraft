package org.kraft.world

enum class BlockType(val id: Byte, val isSolid: Boolean, val textureName: String) {
    AIR(0, false, ""),
    STONE(1, true, "stone.png"),
    DIRT(2, true, "dirt.png"),
    GRASS(3, true, "grass.png");

    val isAir: Boolean
        get() = this == AIR

    companion object {
        private val map = entries.associateBy(BlockType::id)
        fun fromId(id: Byte): BlockType = map[id] ?: AIR
    }
}