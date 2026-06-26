package org.kraft.client.player

import com.badlogic.gdx.math.Vector3
import org.kraft.physics.PhysicsBody
import kotlin.math.floor

/**
 * Represents the physical state and dimensions of the player in the 3D world.
 * 
 * Implements [PhysicsBody] to delegate AABB collision logic and updates to the physics engine.
 */
class Player(
    override val width: Float = 0.6f,
    override val height: Float = 1.8f,
    val eyeHeight: Float = 1.6f
) : PhysicsBody {
    override val position = Vector3(8f, 15f, 8f)
    override val velocity = Vector3()
    override var isGrounded = false

    private val jumpForce = 8f

    val moveInput = Vector3()

    val x: Float
        get() = position.x

    val z: Float
        get() = position.z

    fun jump() {
        if (isGrounded) {
            velocity.y = jumpForce
            isGrounded = false
        }
    }

    /**
     * Checks if the player's bounding box intersects with the given block coordinate.
     */
    fun intersects(blockX: Int, blockY: Int, blockZ: Int): Boolean {
        val halfWidth = width / 2f
        val minX = floor(position.x - halfWidth).toInt()
        val maxX = floor(position.x + halfWidth).toInt()
        val minY = floor(position.y).toInt()
        val maxY = floor(position.y + height).toInt()
        val minZ = floor(position.z - halfWidth).toInt()
        val maxZ = floor(position.z + halfWidth).toInt()

        return blockX in minX..maxX && blockY in minY..maxY && blockZ in minZ..maxZ
    }
}
