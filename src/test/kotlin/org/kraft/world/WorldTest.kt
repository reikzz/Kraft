package org.kraft.world

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.kraft.world.generator.FlatTerrainGenerator
import org.kraft.world.generator.NoiseTerrainGenerator


class WorldTest {
    @Test
    fun `initial world loads chunks around origin`() {
        val world = World(FlatTerrainGenerator())

        assertTrue(world.isChunkLoaded(0, 0))
        assertTrue(world.isChunkLoaded(Chunk.WIDTH, 0))
        assertTrue(world.isChunkLoaded(-1, 0))
    }

    @Test
    fun `world coordinates resolve across positive and negative chunk boundaries`() {
        val world = World(FlatTerrainGenerator())

        assertEquals(BlockType.GRASS, world.getBlockAt(0, 11, 0))
        assertEquals(BlockType.GRASS, world.getBlockAt(Chunk.WIDTH, 11, 0))
        assertEquals(BlockType.GRASS, world.getBlockAt(-1, 11, 0))
        assertEquals(BlockType.AIR, world.getBlockAt(0, Chunk.HEIGHT, 0))
    }

    @Test
    fun `loading chunks around a distant position returns only newly loaded chunks`() {
        val world = World(FlatTerrainGenerator())

        val loadedChunks = world.loadChunksAround(Chunk.WIDTH * 3f, 0f, radius = 1)

        assertTrue(ChunkCoordinate(2, 0) in loadedChunks)
        assertTrue(ChunkCoordinate(3, 0) in loadedChunks)
        assertTrue(ChunkCoordinate(4, 0) in loadedChunks)
        assertFalse(ChunkCoordinate(1, 0) in loadedChunks)
        assertTrue(world.isChunkLoaded(Chunk.WIDTH * 4, 0))
    }

    @Test
    fun `debug noise generator blocks`() {
        val world = World(NoiseTerrainGenerator())
        for (y in 0 until Chunk.HEIGHT) {
            println("DEBUG_BLOCK Y=$y: ${world.getBlockAt(0, y, 0)}")
        }
    }

    @Test
    fun `debug check texture colors`() {
        val paths = listOf("dirt", "grass", "stone")
        for (name in paths) {
            val file = java.io.File("assets/$name.png")
            if (!file.exists()) {
                println("TEXTURE_DEBUG $name: NOT FOUND")
                continue
            }
            val img = javax.imageio.ImageIO.read(file) ?: continue
            var r = 0L
            var g = 0L
            var b = 0L
            val w = img.width
            val h = img.height
            for (x in 0 until w) {
                for (y in 0 until h) {
                    val color = java.awt.Color(img.getRGB(x, y))
                    r += color.red
                    g += color.green
                    b += color.blue
                }
            }
            val total = w.toLong() * h
            println("TEXTURE_DEBUG $name: R=${r/total}, G=${g/total}, B=${b/total} (size=${w}x${h})")
        }
    }

    @Test
    fun `debug print surfaceY grid`() {
        val generator = NoiseTerrainGenerator()
        val chunk = Chunk(0, 0)
        generator.generate(chunk)
        for (x in 0 until Chunk.WIDTH) {
            val sb = java.lang.StringBuilder()
            for (z in 0 until Chunk.DEPTH) {
                var surfaceY = -1
                for (y in Chunk.HEIGHT - 1 downTo 0) {
                    if (chunk.getBlock(x, y, z) != BlockType.AIR) {
                        surfaceY = y
                        break
                    }
                }
                val blockName = if (surfaceY >= 0) chunk.getBlock(x, surfaceY, z).name else "AI"
                sb.append(String.format("%2d(%s) ", surfaceY, blockName.take(2)))
            }
            println("Row $x: $sb")
        }
    }
}
