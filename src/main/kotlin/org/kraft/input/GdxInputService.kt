package org.kraft.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input

/**
 * LibGDX implementation of the InputService.
 */
class GdxInputService : InputService {
    private val keyBindings = mapOf(
        GameAction.MOVE_FORWARD to Input.Keys.W,
        GameAction.MOVE_BACKWARD to Input.Keys.S,
        GameAction.MOVE_LEFT to Input.Keys.A,
        GameAction.MOVE_RIGHT to Input.Keys.D,
        GameAction.JUMP to Input.Keys.SPACE,
        GameAction.TOGGLE_CURSOR to Input.Keys.ESCAPE,
        GameAction.SELECT_SLOT_1 to Input.Keys.NUM_1,
        GameAction.SELECT_SLOT_2 to Input.Keys.NUM_2,
        GameAction.SELECT_SLOT_3 to Input.Keys.NUM_3,
        GameAction.SELECT_SLOT_4 to Input.Keys.NUM_4,
        GameAction.SELECT_SLOT_5 to Input.Keys.NUM_5,
        GameAction.SELECT_SLOT_6 to Input.Keys.NUM_6,
        GameAction.SELECT_SLOT_7 to Input.Keys.NUM_7,
        GameAction.SELECT_SLOT_8 to Input.Keys.NUM_8,
        GameAction.SELECT_SLOT_9 to Input.Keys.NUM_9
    )

    private val buttonBindings = mapOf(
        GameAction.DESTROY_BLOCK to Input.Buttons.LEFT,
        GameAction.PLACE_BLOCK to Input.Buttons.RIGHT
    )

    override fun isActionPressed(action: GameAction): Boolean {
        keyBindings[action]?.let { return Gdx.input.isKeyPressed(it) }
        buttonBindings[action]?.let { return Gdx.input.isButtonPressed(it) }
        return false
    }

    override fun isActionJustPressed(action: GameAction): Boolean {
        keyBindings[action]?.let { return Gdx.input.isKeyJustPressed(it) }
        buttonBindings[action]?.let { return Gdx.input.isButtonJustPressed(it) }
        return false
    }

    override var isCursorCatched: Boolean
        get() = Gdx.input.isCursorCatched
        set(value) {
            Gdx.input.isCursorCatched = value
        }

    override val mouseDeltaX: Float
        get() = Gdx.input.deltaX.toFloat()

    override val mouseDeltaY: Float
        get() = Gdx.input.deltaY.toFloat()
}
