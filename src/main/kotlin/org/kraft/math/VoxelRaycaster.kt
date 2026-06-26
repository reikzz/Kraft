package org.kraft.math

import com.badlogic.gdx.math.Vector3
import org.kraft.world.VoxelWorld
import org.kraft.world.BlockType
import org.kraft.world.Chunk
import kotlin.math.abs
import kotlin.math.floor

/**
 * Traverses voxel grids along a 3D ray direction using Amanatides & Woo's DDA algorithm.
 */
object VoxelRaycaster {
    /**
     * Hit result of a raycast, storing coordinates of the hit block and adjacent empty place coordinates.
     */
    data class Result(
        val hitX: Int, val hitY: Int, val hitZ: Int,
        val placeX: Int, val placeY: Int, val placeZ: Int
    )

    /**
     * Traverses voxel grids from a starting point along a direction up to a maximum distance.
     * @return the first non-AIR voxel [Result], or null if nothing is hit within [maxDistance].
     */
    fun cast(world: VoxelWorld, start: Vector3, direction: Vector3, maxDistance: Float): Result? {
        val dx = direction.x
        val dy = direction.y
        val dz = direction.z

        if (dx == 0f && dy == 0f && dz == 0f) return null

        var x = floor(start.x).toInt()
        var y = floor(start.y).toInt()
        var z = floor(start.z).toInt()

        val stepX = if (dx > 0f) 1 else if (dx < 0f) -1 else 0
        val stepY = if (dy > 0f) 1 else if (dy < 0f) -1 else 0
        val stepZ = if (dz > 0f) 1 else if (dz < 0f) -1 else 0

        val deltaX = if (dx != 0f) abs(1f / dx) else Float.MAX_VALUE
        val deltaY = if (dy != 0f) abs(1f / dy) else Float.MAX_VALUE
        val deltaZ = if (dz != 0f) abs(1f / dz) else Float.MAX_VALUE

        var tMaxX = if (dx > 0f) {
            (floor(start.x) + 1f - start.x) * deltaX
        } else if (dx < 0f) {
            (start.x - floor(start.x)) * deltaX
        } else {
            Float.MAX_VALUE
        }

        var tMaxY = if (dy > 0f) {
            (floor(start.y) + 1f - start.y) * deltaY
        } else if (dy < 0f) {
            (start.y - floor(start.y)) * deltaY
        } else {
            Float.MAX_VALUE
        }

        var tMaxZ = if (dz > 0f) {
            (floor(start.z) + 1f - start.z) * deltaZ
        } else if (dz < 0f) {
            (start.z - floor(start.z)) * deltaZ
        } else {
            Float.MAX_VALUE
        }

        if (tMaxX == 0f && dx < 0f) tMaxX = deltaX
        if (tMaxY == 0f && dy < 0f) tMaxY = deltaY
        if (tMaxZ == 0f && dz < 0f) tMaxZ = deltaZ

        var prevX = x
        var prevY = y
        var prevZ = z

        var distance = 0f

        while (distance < maxDistance) {
            if (world.getBlockAt(x, y, z) != BlockType.AIR) {
                if (y in 0 until Chunk.HEIGHT) {
                    return Result(x, y, z, prevX, prevY, prevZ)
                }
            }

            prevX = x
            prevY = y
            prevZ = z

            if (tMaxX < tMaxY && tMaxX < tMaxZ) {
                distance = tMaxX
                tMaxX += deltaX
                x += stepX
            } else if (tMaxY < tMaxZ) {
                distance = tMaxY
                tMaxY += deltaY
                y += stepY
            } else {
                distance = tMaxZ
                tMaxZ += deltaZ
                z += stepZ
            }
        }

        return null
    }
}
