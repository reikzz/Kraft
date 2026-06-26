package org.kraft.world

import org.kraft.event.Subscription
import kotlin.reflect.KClass

/**
 * Interface representing a 3D voxel-based world.
 * Provides APIs for block querying, modification, and chunk management.
 */
interface VoxelWorld {
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

    /**
     * Subscribes to world events of type [T].
     * Returns a [Subscription] to cancel the subscription later.
     */
    fun <T : Any> subscribe(eventClass: KClass<T>, handler: (T) -> Unit): Subscription
}

/**
 * Reified convenience extension — allows `world.subscribe<BlockChangedEvent> { ... }` syntax.
 */
inline fun <reified T : Any> VoxelWorld.subscribe(noinline handler: (T) -> Unit): Subscription {
    return subscribe(T::class, handler)
}

