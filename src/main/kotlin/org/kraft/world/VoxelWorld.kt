package org.kraft.world

import org.kraft.event.EventBus

/**
 * Interface representing a 3D voxel-based world.
 * Provides APIs for block querying, modification, and chunk management.
 */
interface VoxelWorld {
    /**
     * The event bus local to this world instance.
     */
    val eventBus: EventBus

    /**
     * Set of all currently loaded chunks.
     */
    val loadedChunks: Collection<Chunk>

    /**
     * Returns the chunk at the specified chunk coordinate, or null if it is not loaded.
     */
    fun getChunk(coordinate: ChunkCoordinate): Chunk?

    /**
     * Checks if a chunk covering the given global world coordinates is currently loaded.
     */
    fun isChunkLoaded(worldX: Int, worldZ: Int): Boolean

    /**
     * Gets the block type at the specified global floating point coordinate.
     */
    fun getBlockAt(worldX: Float, worldY: Float, worldZ: Float): BlockType

    /**
     * Gets the block type at the specified global integer coordinate.
     */
    fun getBlockAt(worldX: Int, worldY: Int, worldZ: Int): BlockType

    /**
     * Sets the block type at the specified global integer coordinate.
     * Returns true if the block was successfully set, or false if the chunk is not loaded.
     */
    fun setBlockAt(worldX: Int, worldY: Int, worldZ: Int, type: BlockType): Boolean

    /**
     * Loads chunks around a given world position within a radius.
     * Returns the set of chunk coordinates that were newly loaded during this call.
     */
    fun loadChunksAround(worldX: Float, worldZ: Float, radius: Int, maxLoadsPerFrame: Int = Int.MAX_VALUE): Set<ChunkCoordinate>

}
