package org.kraft.client.input

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

    /**
     * Scroll wheel delta accumulated since the last [consumeScroll] call.
     * Positive = scroll up, negative = scroll down.
     */
    val scrollDelta: Int

    /**
     * Resets the accumulated [scrollDelta] to zero. Call once per frame after reading.
     */
    fun consumeScroll()
}
