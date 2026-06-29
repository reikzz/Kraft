package org.kraft.world

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