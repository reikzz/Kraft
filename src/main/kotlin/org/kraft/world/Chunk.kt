package org.kraft.world

class Chunk(val chunkX: Int, val chunkZ: Int) {
    companion object {
        const val WIDTH = 16
        const val HEIGHT = 128
        const val DEPTH = 16
    }

    private val blocks = ByteArray(WIDTH * HEIGHT * DEPTH)

    /**
     * Marks this chunk as needing a mesh rebuild on the next render frame.
     * Set automatically by [setBlock]; cleared by the renderer after rebuilding.
     */
    var isDirty: Boolean = true

    private fun getIndex(x: Int, y: Int, z: Int): Int {
        return (x * HEIGHT * DEPTH) + (y * DEPTH) + z
    }

    fun getBlock(x: Int, y: Int, z: Int): BlockType {
        if (x !in 0 until WIDTH || y !in 0 until HEIGHT || z !in 0 until DEPTH) return BlockType.AIR
        return BlockType.fromId(blocks[getIndex(x, y, z)])
    }

    fun setBlock(x: Int, y: Int, z: Int, type: BlockType) {
        if (x !in 0 until WIDTH || y !in 0 until HEIGHT || z !in 0 until DEPTH) return
        blocks[getIndex(x, y, z)] = type.id
        isDirty = true
    }

    fun getBlocksData(): ByteArray {
        return blocks.copyOf()
    }

    fun setBlocksData(data: ByteArray) {
        System.arraycopy(data, 0, blocks, 0, blocks.size.coerceAtMost(data.size))
        isDirty = true
    }
}
