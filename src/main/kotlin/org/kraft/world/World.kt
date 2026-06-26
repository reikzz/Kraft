package org.kraft.world

import org.kraft.event.BlockChangedEvent
import org.kraft.event.ChunkLoadedEvent
import org.kraft.event.EventBus
import org.kraft.world.generator.TerrainGenerator
import org.kraft.world.generator.NoiseTerrainGenerator
import kotlin.math.floor

/**
 * Implementation of [VoxelWorld] handling block storage, updates, and chunk lifecycle.
 * Supports both local terrain generation (singleplayer/server) and remote chunk streaming (multiplayer client).
 */
class World(
    private val terrainGenerator: TerrainGenerator? = NoiseTerrainGenerator(),
) : VoxelWorld {
    override val eventBus = EventBus()
    private val chunks = mutableMapOf<ChunkCoordinate, Chunk>()

    init {
        // Only load starting chunks if local terrain generation is enabled
        if (terrainGenerator != null) {
            loadChunksAround(0f, 0f, radius = 1)
        }
    }

    override val loadedChunks: Collection<Chunk>
        get() = chunks.values

    override fun getChunk(coordinate: ChunkCoordinate): Chunk? {
        return chunks[coordinate]
    }

    override fun loadChunksAround(worldX: Float, worldZ: Float, radius: Int, maxLoadsPerFrame: Int): Set<ChunkCoordinate> {
        if (terrainGenerator == null) return emptySet()

        val centerChunkX = Math.floorDiv(floor(worldX).toInt(), Chunk.WIDTH)
        val centerChunkZ = Math.floorDiv(floor(worldZ).toInt(), Chunk.DEPTH)
        val loaded = mutableSetOf<ChunkCoordinate>()
        var loads = 0

        for (chunkX in centerChunkX - radius..centerChunkX + radius) {
            for (chunkZ in centerChunkZ - radius..centerChunkZ + radius) {
                val coord = ChunkCoordinate(chunkX, chunkZ)
                if (coord !in chunks) {
                    if (loads < maxLoadsPerFrame) {
                        if (loadChunk(chunkX, chunkZ)) {
                            loaded += coord
                            loads++
                        }
                    }
                }
            }
        }

        return loaded
    }

    override fun isChunkLoaded(worldX: Int, worldZ: Int): Boolean {
        val chunkX = Math.floorDiv(worldX, Chunk.WIDTH)
        val chunkZ = Math.floorDiv(worldZ, Chunk.DEPTH)
        return chunks.containsKey(ChunkCoordinate(chunkX, chunkZ))
    }

    override fun getBlockAt(worldX: Float, worldY: Float, worldZ: Float): BlockType {
        return getBlockAt(floor(worldX).toInt(), floor(worldY).toInt(), floor(worldZ).toInt())
    }

    override fun getBlockAt(worldX: Int, worldY: Int, worldZ: Int): BlockType {
        val chunkX = Math.floorDiv(worldX, Chunk.WIDTH)
        val chunkZ = Math.floorDiv(worldZ, Chunk.DEPTH)
        val chunk = chunks[ChunkCoordinate(chunkX, chunkZ)] ?: return BlockType.AIR
        val localX = Math.floorMod(worldX, Chunk.WIDTH)
        val localZ = Math.floorMod(worldZ, Chunk.DEPTH)
        return chunk.getBlock(localX, worldY, localZ)
    }

    override fun setBlockAt(worldX: Int, worldY: Int, worldZ: Int, type: BlockType): Boolean {
        val chunkX = Math.floorDiv(worldX, Chunk.WIDTH)
        val chunkZ = Math.floorDiv(worldZ, Chunk.DEPTH)
        val chunk = chunks[ChunkCoordinate(chunkX, chunkZ)] ?: return false
        val localX = Math.floorMod(worldX, Chunk.WIDTH)
        val localZ = Math.floorMod(worldZ, Chunk.DEPTH)

        val oldType = chunk.getBlock(localX, worldY, localZ)
        if (oldType == type) return true

        chunk.setBlock(localX, worldY, localZ, type)

        eventBus.publish(BlockChangedEvent(worldX, worldY, worldZ, oldType, type))
        return true
    }

    /**
     * Receives raw chunk block data from network packets and notifies the renderer.
     * Used for multiplayer network clients.
     */
    fun handleChunkData(chunkX: Int, chunkZ: Int, data: ByteArray) {
        val coordinate = ChunkCoordinate(chunkX, chunkZ)
        val chunk = chunks.getOrPut(coordinate) { Chunk(chunkX, chunkZ) }
        chunk.setBlocksData(data)
        eventBus.publish(ChunkLoadedEvent(chunkX, chunkZ))
    }

    private fun loadChunk(chunkX: Int, chunkZ: Int): Boolean {
        val coordinate = ChunkCoordinate(chunkX, chunkZ)
        if (coordinate in chunks) return false

        val chunk = Chunk(chunkX, chunkZ)
        terrainGenerator?.generate(chunk)
        chunks[coordinate] = chunk

        eventBus.publish(ChunkLoadedEvent(chunkX, chunkZ))
        return true
    }
}

data class ChunkCoordinate(val x: Int, val z: Int)
