package org.kraft.physics

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import org.kraft.world.VoxelWorld
import kotlin.math.floor

/**
 * Game physics engine performing AABB-based motion and collision resolution against voxel blocks.
 */
class VoxelPhysicsEngine(
    private val world: VoxelWorld,
    private val gravity: Float = -22f
) {
    /**
     * Updates coordinates, velocity, and state of the physics body.
     * Resolves movements independently across the vertical and horizontal axes to prevent diagonal tunneling.
     */
    fun update(body: PhysicsBody, moveInput: Vector3, deltaTime: Float) {
        if (!body.isGrounded) {
            body.velocity.y += gravity * deltaTime
        } else {
            body.velocity.y = 0f
        }

        val accel = if (body.isGrounded) 12f else 3.5f
        body.velocity.x = MathUtils.lerp(body.velocity.x, moveInput.x, accel * deltaTime)
        body.velocity.z = MathUtils.lerp(body.velocity.z, moveInput.z, accel * deltaTime)

        moveVertically(body, body.velocity.y * deltaTime)

        if (moveHorizontally(body, body.velocity.x * deltaTime, 0f)) {
            body.velocity.x = 0f
        }
        if (moveHorizontally(body, 0f, body.velocity.z * deltaTime)) {
            body.velocity.z = 0f
        }
    }

    private fun moveVertically(body: PhysicsBody, amount: Float) {
        body.isGrounded = false
        body.position.y += amount

        if (!collidesAt(body, body.position)) return

        if (amount < 0f) {
            body.position.y = floor(body.position.y) + 1f
            body.isGrounded = true
        } else if (amount > 0f) {
            body.position.y = floor(body.position.y + body.height) - body.height
        }
        body.velocity.y = 0f
    }

    private fun moveHorizontally(body: PhysicsBody, amountX: Float, amountZ: Float): Boolean {
        body.position.x += amountX
        body.position.z += amountZ

        if (!collidesAt(body, body.position)) return false

        body.position.x -= amountX
        body.position.z -= amountZ
        return true
    }

    /**
     * Checks if the body's bounding box collides with solid voxels at the given candidate position.
     */
    fun collidesAt(body: PhysicsBody, candidatePosition: Vector3): Boolean {
        val halfWidth = body.width / 2f
        val minX = floor(candidatePosition.x - halfWidth).toInt()
        val maxX = floor(candidatePosition.x + halfWidth).toInt()
        val minY = floor(candidatePosition.y).toInt()
        val maxY = floor(candidatePosition.y + body.height).toInt()
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
}
