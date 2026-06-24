package org.kraft.math

import com.badlogic.gdx.math.Vector3
import org.kraft.world.World
import org.kraft.world.BlockType
import org.kraft.world.Chunk
import kotlin.math.floor

/**
 * Utility for raycasting within a voxel grid.
 * 
 * Steps along the direction of a ray with a small increment to find the first non-AIR block.
 * Returns the coordinates of the hit block (hit) and the adjacent empty block coordinates (place)
 * immediately before contact (useful for placing new blocks).
 */
object VoxelRaycaster {
    data class Result(
        val hitX: Int, val hitY: Int, val hitZ: Int,
        val placeX: Int, val placeY: Int, val placeZ: Int
    )

    fun cast(world: World, start: Vector3, direction: Vector3, maxDistance: Float): Result? {
        val currentPos = start.cpy()
        val step = direction.cpy().nor().scl(0.02f)
        val steps = (maxDistance / 0.02f).toInt()

        var prevX = floor(currentPos.x).toInt()
        var prevY = floor(currentPos.y).toInt()
        var prevZ = floor(currentPos.z).toInt()

        for (i in 0..steps) {
            currentPos.add(step)
            val x = floor(currentPos.x).toInt()
            val y = floor(currentPos.y).toInt()
            val z = floor(currentPos.z).toInt()

            if (world.getBlockAt(x, y, z) != BlockType.AIR) {
                if (y in 0 until Chunk.HEIGHT) {
                    return Result(x, y, z, prevX, prevY, prevZ)
                }
            }
            prevX = x
            prevY = y
            prevZ = z
        }
        return null
    }
}
