package org.kraft.world.storage

import org.kraft.world.Chunk

interface ChunkStorage {
    fun loadChunk(chunkX: Int, chunkZ: Int): Chunk?
    fun saveChunk(chunk: Chunk)
    fun saveAll(chunks: Collection<Chunk>)
}
