package org.kraft.world.generator

import org.kraft.world.Chunk
import org.kraft.world.BlockType
import org.kraft.math.PerlinNoise2D

/**
 * Generates terrain using layered 2D Perlin noise to produce varied elevation.
 */
class NoiseTerrainGenerator(private val seed: Long = 0L) : TerrainGenerator {
    // Seed offset applied to noise coordinates for world uniqueness.
    private val offsetX = (seed and 0xFFFFF).toFloat() * 0.1f
    private val offsetZ = ((seed shr 20) and 0xFFFFF).toFloat() * 0.1f

    override fun generate(chunk: Chunk) {
        val chunkWorldX = chunk.chunkX * Chunk.WIDTH
        val chunkWorldZ = chunk.chunkZ * Chunk.DEPTH

        for (x in 0 until Chunk.WIDTH) {
            val globalX = chunkWorldX + x
            for (z in 0 until Chunk.DEPTH) {
                val globalZ = chunkWorldZ + z

                // Layered noise for richer terrain — coarse base + fine detail
                val nx = globalX + offsetX
                val nz = globalZ + offsetZ
                val n1 = PerlinNoise2D.noise(nx * 0.015f, nz * 0.015f) * 28f   // large hills
                val n2 = PerlinNoise2D.noise(nx * 0.06f,  nz * 0.06f)  * 8f    // medium bumps
                val n3 = PerlinNoise2D.noise(nx * 0.2f,   nz * 0.2f)   * 2f    // fine detail

                // Surface sits roughly in the middle of the chunk column
                val surfaceY = (Chunk.HEIGHT / 2 + (n1 + n2 + n3)).toInt()
                    .coerceIn(4, Chunk.HEIGHT - 4)

                val surfaceBlock = when {
                    surfaceY >= Chunk.HEIGHT * 3 / 4 -> BlockType.STONE   // high peaks = bare stone
                    surfaceY <= Chunk.HEIGHT / 4      -> BlockType.DIRT    // low valleys = muddy dirt
                    else                              -> BlockType.GRASS
                }

                for (y in 0 until Chunk.HEIGHT) {
                    val blockType = when {
                        y == 0             -> BlockType.STONE  // bedrock-like bottom
                        y > surfaceY       -> BlockType.AIR
                        y == surfaceY      -> surfaceBlock
                        y >= surfaceY - 3  -> BlockType.DIRT
                        else               -> BlockType.STONE
                    }
                    chunk.setBlock(x, y, z, blockType)
                }
            }
        }
    }
}
