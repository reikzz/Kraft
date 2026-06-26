package org.kraft.world.generator

import org.kraft.world.Chunk
import org.kraft.world.BlockType

class FlatTerrainGenerator : TerrainGenerator {
    private companion object {
        val SURFACE_LEVEL = Chunk.HEIGHT / 4   // scales automatically with chunk height
        const val DIRT_DEPTH = 3
    }

    override fun generate(chunk: Chunk) {
        for (x in 0 until Chunk.WIDTH) {
            for (z in 0 until Chunk.DEPTH) {
                for (y in 0 until Chunk.HEIGHT) {
                    val blockType = when {
                        y > SURFACE_LEVEL             -> BlockType.AIR
                        y == SURFACE_LEVEL            -> BlockType.GRASS
                        y >= SURFACE_LEVEL - DIRT_DEPTH -> BlockType.DIRT
                        else                          -> BlockType.STONE
                    }
                    chunk.setBlock(x, y, z, blockType)
                }
            }
        }
    }
}
