package org.kraft.world

import org.kraft.world.generator.TerrainGenerator
import org.kraft.world.generator.NoiseTerrainGenerator
import kotlin.math.floor

class World(
    private val terrainGenerator: TerrainGenerator = NoiseTerrainGenerator(),
) {
    private val chunks = mutableMapOf<ChunkCoordinate, Chunk>()
    private val listeners = mutableListOf<WorldListener>()

    init {
        loadChunksAround(0f, 0f, radius = 1)
    }

    val loadedChunks: Collection<Chunk>
        get() = chunks.values

    fun addListener(listener: WorldListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: WorldListener) {
        listeners.remove(listener)
    }

    fun getChunk(coordinate: ChunkCoordinate): Chunk? {
        return chunks[coordinate]
    }

    fun loadChunksAround(worldX: Float, worldZ: Float, radius: Int): Set<ChunkCoordinate> {
        val centerChunkX = Math.floorDiv(floor(worldX).toInt(), Chunk.WIDTH)
        val centerChunkZ = Math.floorDiv(floor(worldZ).toInt(), Chunk.DEPTH)
        val loaded = mutableSetOf<ChunkCoordinate>()

        for (chunkX in centerChunkX - radius..centerChunkX + radius) {
            for (chunkZ in centerChunkZ - radius..centerChunkZ + radius) {
                if (loadChunk(chunkX, chunkZ)) {
                    loaded += ChunkCoordinate(chunkX, chunkZ)
                }
            }
        }

        return loaded
    }

    fun isChunkLoaded(worldX: Int, worldZ: Int): Boolean {
        val chunkX = Math.floorDiv(worldX, Chunk.WIDTH)
        val chunkZ = Math.floorDiv(worldZ, Chunk.DEPTH)
        return chunks.containsKey(ChunkCoordinate(chunkX, chunkZ))
    }

    fun getBlockAt(worldX: Float, worldY: Float, worldZ: Float): BlockType {
        return getBlockAt(floor(worldX).toInt(), floor(worldY).toInt(), floor(worldZ).toInt())
    }

    fun getBlockAt(worldX: Int, worldY: Int, worldZ: Int): BlockType {
        val chunkX = Math.floorDiv(worldX, Chunk.WIDTH)
        val chunkZ = Math.floorDiv(worldZ, Chunk.DEPTH)
        val chunk = chunks[ChunkCoordinate(chunkX, chunkZ)] ?: return BlockType.AIR
        val localX = Math.floorMod(worldX, Chunk.WIDTH)
        val localZ = Math.floorMod(worldZ, Chunk.DEPTH)
        return chunk.getBlock(localX, worldY, localZ)
    }

    fun setBlockAt(worldX: Int, worldY: Int, worldZ: Int, type: BlockType): Boolean {
        val chunkX = Math.floorDiv(worldX, Chunk.WIDTH)
        val chunkZ = Math.floorDiv(worldZ, Chunk.DEPTH)
        val chunk = chunks[ChunkCoordinate(chunkX, chunkZ)] ?: return false
        val localX = Math.floorMod(worldX, Chunk.WIDTH)
        val localZ = Math.floorMod(worldZ, Chunk.DEPTH)

        val oldType = chunk.getBlock(localX, worldY, localZ)
        if (oldType == type) return true

        chunk.setBlock(localX, worldY, localZ, type)

        listeners.forEach { it.onBlockChanged(worldX, worldY, worldZ, oldType, type) }
        return true
    }

    private fun loadChunk(chunkX: Int, chunkZ: Int): Boolean {
        val coordinate = ChunkCoordinate(chunkX, chunkZ)
        if (coordinate in chunks) return false

        val chunk = Chunk(chunkX, chunkZ)
        terrainGenerator.generate(chunk)
        chunks[coordinate] = chunk

        listeners.forEach { it.onChunkLoaded(chunkX, chunkZ) }
        return true
    }
}

data class ChunkCoordinate(val x: Int, val z: Int)
