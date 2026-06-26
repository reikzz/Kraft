package org.kraft.world

/**
 * Represents a voxel block type.
 * 
 * Refactored from enum class to dynamic registry to allow runtime registration of new block types
 * (e.g. from JSON config files or modding APIs) while keeping full backward compatibility.
 */
class BlockType private constructor(
    val id: Byte,
    val isSolid: Boolean,
    val textureName: String,
    val name: String
) {
    val isAir: Boolean
        get() = id == 0.toByte()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BlockType) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.toInt()

    override fun toString(): String = name

    companion object {
        // Core block types mapped as static companion properties for backward compatibility
        val AIR = BlockType(0, false, "", "AIR")
        val STONE = BlockType(1, true, "stone.png", "STONE")
        val DIRT = BlockType(2, true, "dirt.png", "DIRT")
        val GRASS = BlockType(3, true, "grass.png", "GRASS")

        private val registry = mutableMapOf<Byte, BlockType>()

        init {
            register(AIR)
            register(STONE)
            register(DIRT)
            register(GRASS)
        }

        /**
         * Dynamically registers a new block type.
         */
        fun register(blockType: BlockType) {
            require(!registry.containsKey(blockType.id)) {
                "Block type with ID ${blockType.id} is already registered!"
            }
            registry[blockType.id] = blockType
        }

        /**
         * Returns the block type associated with the given ID, or AIR if not registered.
         */
        fun fromId(id: Byte): BlockType = registry[id] ?: AIR

        /**
         * Collection of all currently registered block types.
         */
        val entries: Collection<BlockType>
            get() = registry.values
    }
}
