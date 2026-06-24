package org.kraft.input

/**
 * Service abstracting keyboard and mouse inputs into game actions.
 */
interface InputService {
    /**
     * Checks if the given action is currently being held down.
     */
    fun isActionPressed(action: GameAction): Boolean

    /**
     * Checks if the given action was just pressed in the current frame.
     */
    fun isActionJustPressed(action: GameAction): Boolean

    /**
     * Controls whether the hardware mouse cursor is captured (catched) by the game window.
     */
    var isCursorCatched: Boolean

    /**
     * Mouse delta X movement since the last frame.
     */
    val mouseDeltaX: Float

    /**
     * Mouse delta Y movement since the last frame.
     */
    val mouseDeltaY: Float
}
