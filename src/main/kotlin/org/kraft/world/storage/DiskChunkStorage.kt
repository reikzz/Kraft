package org.kraft.world.storage

import org.kraft.world.Chunk

import org.kraft.core.GamePaths
import java.io.File
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

class DiskChunkStorage(private val worldName: String) : ChunkStorage {
    private fun getChunkFile(chunkX: Int, chunkZ: Int): File {
        return File(GamePaths.getWorldDir(worldName), "chunk_${chunkX}_${chunkZ}.bin")
    }

    override fun saveChunk(chunk: Chunk) {
        val file = getChunkFile(chunk.chunkX, chunk.chunkZ)
        try {
            GZIPOutputStream(file.outputStream()).use { gzip ->
                gzip.write(chunk.getBlocksData())
            }
        } catch (e: Exception) {
            System.err.println("Failed to save chunk ${chunk.chunkX}, ${chunk.chunkZ}: ${e.message}")
        }
    }

    override fun loadChunk(chunkX: Int, chunkZ: Int): Chunk? {
        val file = getChunkFile(chunkX, chunkZ)
        if (!file.exists()) return null

        try {
            val data = GZIPInputStream(file.inputStream()).use { gzip ->
                gzip.readBytes()
            }
            val chunk = Chunk(chunkX, chunkZ)
            chunk.setBlocksData(data)
            return chunk
        } catch (e: Exception) {
            System.err.println("Failed to load chunk $chunkX, $chunkZ: ${e.message}")
            return null
        }
    }

    override fun saveAll(chunks: Collection<Chunk>) {
        chunks.forEach { saveChunk(it) }
    }
}
