package org.kraft.world.generator

import org.kraft.world.Chunk
import org.kraft.world.BlockType
import org.kraft.math.PerlinNoise2D

class NoiseTerrainGenerator : TerrainGenerator {
    override fun generate(chunk: Chunk) {
        val chunkWorldX = chunk.chunkX * Chunk.WIDTH
        val chunkWorldZ = chunk.chunkZ * Chunk.DEPTH

        for (x in 0 until Chunk.WIDTH) {
            val globalX = chunkWorldX + x
            for (z in 0 until Chunk.DEPTH) {
                val globalZ = chunkWorldZ + z

                val n1 = PerlinNoise2D.noise(globalX * 0.025f, globalZ * 0.025f) * 6f
                val n2 = PerlinNoise2D.noise(globalX * 0.1f, globalZ * 0.1f) * 2f
                val heightNoise = n1 + n2

                val surfaceY = (8f + heightNoise).toInt().coerceIn(2, Chunk.HEIGHT - 2)

                val surfaceBlock = when {
                    surfaceY >= 12 -> BlockType.STONE
                    surfaceY <= 5 -> BlockType.DIRT
                    else -> BlockType.GRASS
                }

                for (y in 0 until Chunk.HEIGHT) {
                    val blockType = when {
                        y > surfaceY -> BlockType.AIR
                        y == surfaceY -> surfaceBlock
                        y >= surfaceY - 2 -> BlockType.DIRT
                        else -> BlockType.STONE
                    }
                    chunk.setBlock(x, y, z, blockType)
                }
            }
        }
    }
}
