package org.kraft.client

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.math.Vector3
import org.kraft.math.VoxelRaycaster
import org.kraft.world.BlockType
import org.kraft.world.World

/**
 * Maps user input to the player's physical state.
 * 
 * Projects the camera's look direction onto the horizontal X-Z plane
 * to prevent the player from flying or sinking while moving forward/backward.
 * 
 * Coordinates the update lifecycle: inputs are processed before the physics step,
 * whereas camera alignment and block interactions are executed after the physics step
 * to prevent camera jitter.
 */
class PlayerController(
    private val player: Player,
    private val world: World,
    private val camera: PerspectiveCamera,
    private val movementSpeed: Float = 5.0f,
    private val interactionRange: Float = 5.0f
) {
    private val forwardVec = Vector3()
    private val rightVec = Vector3()
    private val movementDir = Vector3()

    /**
     * Stores the raycast result of the block currently under the crosshair.
     */
    var targetBlock: VoxelRaycaster.Result? = null
        private set

    /**
     * Polls keyboard movement input and computes the target horizontal velocity.
     * Normalizes the direction vector to prevent faster diagonal movement.
     */
    fun updateInput(deltaTime: Float) {
        forwardVec.set(camera.direction.x, 0f, camera.direction.z).nor()
        
        rightVec.set(camera.direction).crs(camera.up)
        rightVec.y = 0f
        rightVec.nor()

        movementDir.setZero()

        if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            movementDir.add(forwardVec)
        }
        if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            movementDir.sub(forwardVec)
        }
        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            movementDir.sub(rightVec)
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            movementDir.add(rightVec)
        }

        if (movementDir.len2() > 0f) {
            movementDir.nor()
        }

        player.moveInput.set(movementDir.scl(movementSpeed))

        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            player.jump()
        }
    }

    /**
     * Synchronizes the camera position with the player's eye height,
     * updates the camera matrix, and handles mouse click actions.
     */
    fun updateCameraAndInteraction() {
        camera.position.set(player.position.x, player.position.y + player.eyeHeight, player.position.z)
        camera.update()

        // Cast ray to find target block under crosshair
        targetBlock = if (Gdx.input.isCursorCatched) {
            VoxelRaycaster.cast(world, camera.position, camera.direction, interactionRange)
        } else {
            null
        }

        handleInteraction()
    }

    private fun handleInteraction() {
        if (!Gdx.input.isCursorCatched) return
        val target = targetBlock ?: return

        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            world.setBlockAt(target.hitX, target.hitY, target.hitZ, BlockType.AIR)
        } else if (Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT)) {
            if (!player.intersects(target.placeX, target.placeY, target.placeZ)) {
                world.setBlockAt(target.placeX, target.placeY, target.placeZ, BlockType.STONE)
            }
        }
    }
}
