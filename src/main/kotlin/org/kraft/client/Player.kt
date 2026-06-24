package org.kraft.client

import com.badlogic.gdx.math.Vector3
import org.kraft.world.BlockType
import org.kraft.world.Chunk
import org.kraft.world.World
import kotlin.math.floor

/**
 * Represents the physical state and dimensions of the player in the 3D world.
 * 
 * Simulates gravity, jumps, and horizontal movements, and resolves collisions
 * with solid voxel blocks using axis-aligned bounding box (AABB) collision checks.
 * 
 * Separating movement into independent vertical and horizontal steps allows
 * the player to slide smoothly along walls instead of getting stuck completely.
 */
class Player(
    private val world: World,
) {
    val position = Vector3(8f, 15f, 8f)
    val velocity = Vector3()

    val playerWidth = 0.6f
    val playerHeight = 1.8f
    val eyeHeight = 1.6f

    private val gravity = -22f
    private val jumpForce = 8f

    var isGrounded = false
        private set

    val moveInput = Vector3()

    val x: Float
        get() = position.x

    val z: Float
        get() = position.z

    fun update(deltaTime: Float) {
        if (!isGrounded) {
            velocity.y += gravity * deltaTime
        }

        moveVertically(velocity.y * deltaTime)
        moveHorizontally(moveInput.x * deltaTime, 0f)
        moveHorizontally(0f, moveInput.z * deltaTime)
    }

    fun jump() {
        if (isGrounded) {
            velocity.y = jumpForce
            isGrounded = false
        }
    }

    private fun moveVertically(amount: Float) {
        isGrounded = false

        position.y += amount
        if (!collidesAt(position)) return

        if (amount < 0f) {
            position.y = floor(position.y) + 1f
            isGrounded = true
        } else if (amount > 0f) {
            position.y = floor(position.y + playerHeight) - playerHeight
        }

        velocity.y = 0f
    }

    private fun moveHorizontally(amountX: Float, amountZ: Float) {
        position.x += amountX
        position.z += amountZ

        if (!collidesAt(position)) return

        position.x -= amountX
        position.z -= amountZ
    }

    fun collidesAt(candidatePosition: Vector3): Boolean {
        val halfWidth = playerWidth / 2f
        val minX = floor(candidatePosition.x - halfWidth).toInt()
        val maxX = floor(candidatePosition.x + halfWidth).toInt()
        val minY = floor(candidatePosition.y).toInt()
        val maxY = floor(candidatePosition.y + playerHeight).toInt()
        val minZ = floor(candidatePosition.z - halfWidth).toInt()
        val maxZ = floor(candidatePosition.z + halfWidth).toInt()

        for (x in minX..maxX) {
            for (y in minY..maxY) {
                for (z in minZ..maxZ) {
                    if (world.getBlockAt(x, y, z).isSolid) return true
                }
            }
        }

        return false
    }

    fun intersects(blockX: Int, blockY: Int, blockZ: Int): Boolean {
        val halfWidth = playerWidth / 2f
        val minX = floor(position.x - halfWidth).toInt()
        val maxX = floor(position.x + halfWidth).toInt()
        val minY = floor(position.y).toInt()
        val maxY = floor(position.y + playerHeight).toInt()
        val minZ = floor(position.z - halfWidth).toInt()
        val maxZ = floor(position.z + halfWidth).toInt()

        return blockX in minX..maxX && blockY in minY..maxY && blockZ in minZ..maxZ
    }
}
