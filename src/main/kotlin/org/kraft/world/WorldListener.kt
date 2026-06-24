package org.kraft.world

interface WorldListener {
    fun onBlockChanged(x: Int, y: Int, z: Int, oldType: BlockType, newType: BlockType)
    fun onChunkLoaded(chunkX: Int, chunkZ: Int)
}
