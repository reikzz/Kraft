package org.kraft.physics

import com.badlogic.gdx.math.Vector3

/**
 * Interface representing a physical object that can move and collide within a voxel world.
 */
interface PhysicsBody {
    /**
     * Position of the body in world space.
     */
    val position: Vector3

    /**
     * Velocity of the body.
     */
    val velocity: Vector3

    /**
     * Width of the body's bounding box along the X and Z axes.
     */
    val width: Float

    /**
     * Height of the body's bounding box along the Y axis.
     */
    val height: Float

    /**
     * Whether the body is currently resting on the ground.
     */
    var isGrounded: Boolean
}
