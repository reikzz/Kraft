package org.kraft.client.player

import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.math.Vector3
import org.kraft.client.input.GameAction
import org.kraft.client.input.InputService
import org.kraft.math.VoxelRaycaster
import org.kraft.world.BlockType
import org.kraft.world.VoxelWorld

/**
 * Maps player controls and camera logic to the active player instance.
 * Coordinates input states from [InputService] to manipulate movement velocity, hotbar items, and cursor-targeted block actions.
 */
class PlayerController(
    private val player: Player,
    private val world: VoxelWorld,
    private val camera: PerspectiveCamera,
    private val inputService: InputService,
    private val movementSpeed: Float = 5.0f,
    private val interactionRange: Float = 5.0f,
    private val onBlockInteracted: (x: Int, y: Int, z: Int, type: BlockType) -> Unit = { x, y, z, type ->
        world.setBlockAt(x, y, z, type)
    }
) {
    private val forwardVec = Vector3()
    private val rightVec = Vector3()
    private val movementDir = Vector3()

    private val tmp = Vector3()
    private val degreesPerPixel = 0.3f
    private var skipFrames = 3
    private var bobbingPhase = 0f
    private val bobbingAmount = 0.08f

    /**
     * Voxel intersection hit result under the screen crosshair. Null if out of range.
     */
    var targetBlock: VoxelRaycaster.Result? = null
        private set
        
    var miningProgress: Float = 0f
        private set
    private var miningTargetX: Int = -1
    private var miningTargetY: Int = -1
    private var miningTargetZ: Int = -1

    /**
     * Active inventory hotbar slot index (0 to 8).
     */
    var selectedSlot: Int = 0
        private set

    /**
     * Player quick-access hotbar containing block types.
     */
    val hotbar = arrayOf(
        BlockType.STONE,
        BlockType.DIRT,
        BlockType.GRASS,
        BlockType.AIR,
        BlockType.AIR,
        BlockType.AIR,
        BlockType.AIR,
        BlockType.AIR,
        BlockType.AIR
    )

    private val slotKeys = arrayOf(
        GameAction.SELECT_SLOT_1, GameAction.SELECT_SLOT_2, GameAction.SELECT_SLOT_3,
        GameAction.SELECT_SLOT_4, GameAction.SELECT_SLOT_5, GameAction.SELECT_SLOT_6,
        GameAction.SELECT_SLOT_7, GameAction.SELECT_SLOT_8, GameAction.SELECT_SLOT_9
    )

    /**
     * Polls action inputs, computes horizontal movement direction, and updates camera looking angles.
     */
    fun updateInput(deltaTime: Float) {
        updateCameraLook()

        forwardVec.set(camera.direction.x, 0f, camera.direction.z).nor()
        
        rightVec.set(camera.direction).crs(camera.up)
        rightVec.y = 0f
        rightVec.nor()

        movementDir.setZero()

        if (inputService.isActionPressed(GameAction.MOVE_FORWARD)) {
            movementDir.add(forwardVec)
        }
        if (inputService.isActionPressed(GameAction.MOVE_BACKWARD)) {
            movementDir.sub(forwardVec)
        }
        if (inputService.isActionPressed(GameAction.MOVE_LEFT)) {
            movementDir.sub(rightVec)
        }
        if (inputService.isActionPressed(GameAction.MOVE_RIGHT)) {
            movementDir.add(rightVec)
        }

        if (movementDir.len2() > 0f) {
            movementDir.nor()
        }

        val speed = if (inputService.isActionPressed(GameAction.SPRINT)) movementSpeed * 1.5f else movementSpeed
        player.moveInput.set(movementDir.scl(speed))

        if (inputService.isActionPressed(GameAction.JUMP)) {
            player.jump()
        }

        for (i in slotKeys.indices) {
            if (inputService.isActionJustPressed(slotKeys[i])) {
                selectedSlot = i
                break
            }
        }

        // Scroll wheel slot selection (scroll up = previous slot, scroll down = next slot)
        val scroll = inputService.scrollDelta
        if (scroll != 0) {
            selectedSlot = Math.floorMod(selectedSlot + scroll, hotbar.size)
            inputService.consumeScroll()
        }
    }

    /**
     * Positions camera at player eye height, updates camera projection matrices, and executes block placements/destructions.
     */
    fun updateCameraAndInteraction() {
        val horizontalSpeed = kotlin.math.sqrt(player.velocity.x * player.velocity.x + player.velocity.z * player.velocity.z)
        if (player.isGrounded && horizontalSpeed > 0.1f) {
            bobbingPhase += horizontalSpeed * 2.5f * com.badlogic.gdx.Gdx.graphics.deltaTime
        } else {
            // Smoothly return to 0
            bobbingPhase += (0f - bobbingPhase) * 10f * com.badlogic.gdx.Gdx.graphics.deltaTime
        }
        val bobOffset = kotlin.math.sin(bobbingPhase) * bobbingAmount
        
        camera.position.set(player.position.x, player.position.y + player.eyeHeight + bobOffset, player.position.z)
        camera.update()

        targetBlock = if (inputService.isCursorCatched) {
            VoxelRaycaster.cast(world, camera.position, camera.direction, interactionRange)
        } else {
            null
        }

        handleInteraction()
    }

    private fun handleInteraction() {
        if (!inputService.isCursorCatched) {
            miningProgress = 0f
            return
        }
        val target = targetBlock
        if (target == null) {
            miningProgress = 0f
            return
        }

        if (inputService.isActionPressed(GameAction.DESTROY_BLOCK)) {
            if (target.hitX != miningTargetX || target.hitY != miningTargetY || target.hitZ != miningTargetZ) {
                miningTargetX = target.hitX
                miningTargetY = target.hitY
                miningTargetZ = target.hitZ
                miningProgress = 0f
            }
            val targetBlockType = world.getBlockAt(miningTargetX, miningTargetY, miningTargetZ)
            if (targetBlockType != BlockType.AIR) {
                miningProgress += com.badlogic.gdx.Gdx.graphics.deltaTime / targetBlockType.hardness
                if (miningProgress >= 1f) {
                    onBlockInteracted(miningTargetX, miningTargetY, miningTargetZ, BlockType.AIR)
                    miningProgress = 0f
                }
            } else {
                miningProgress = 0f
            }
        } else {
            miningProgress = 0f
            if (inputService.isActionJustPressed(GameAction.PLACE_BLOCK)) {
                val blockToPlace = hotbar[selectedSlot]
                if (blockToPlace != BlockType.AIR) {
                    if (!player.intersects(target.placeX, target.placeY, target.placeZ)) {
                        onBlockInteracted(target.placeX, target.placeY, target.placeZ, blockToPlace)
                    }
                }
            }
        }
    }

    private fun updateCameraLook() {
        if (inputService.isCursorCatched) {
            if (skipFrames > 0) {
                skipFrames--
                return
            }
            val deltaX = -inputService.mouseDeltaX * degreesPerPixel
            val deltaY = -inputService.mouseDeltaY * degreesPerPixel

            camera.direction.rotate(camera.up, deltaX)
            tmp.set(camera.direction).crs(camera.up).nor()
            
            val newDirection = Vector3(camera.direction).rotate(tmp, deltaY).nor()
            // Limit pitch to prevent looking straight up or down (avoids gimbal lock/NaNs)
            val limit = 0.99f
            if (newDirection.y in -limit..limit) {
                camera.direction.set(newDirection)
            }
        } else {
            skipFrames = 3
        }
    }
}
