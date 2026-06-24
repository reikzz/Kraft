package org.kraft.event

import org.kraft.world.BlockType

/**
 * Dispatched when a block type changes in the world.
 */
data class BlockChangedEvent(
    val x: Int,
    val y: Int,
    val z: Int,
    val oldType: BlockType,
    val newType: BlockType
)

/**
 * Dispatched when a chunk is loaded and ready.
 */
data class ChunkLoadedEvent(
    val chunkX: Int,
    val chunkZ: Int
)
