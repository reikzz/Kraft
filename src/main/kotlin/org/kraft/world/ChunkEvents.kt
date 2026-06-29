package org.kraft.world

/**
 * Dispatched when a chunk is loaded and ready.
 */
data class ChunkLoadedEvent(
    val chunkX: Int,
    val chunkZ: Int
)

/**
 * Dispatched when a chunk is unloaded and should be disposed.
 */
data class ChunkUnloadedEvent(
    val chunkX: Int,
    val chunkZ: Int
)
