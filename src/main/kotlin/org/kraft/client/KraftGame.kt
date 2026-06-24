package org.kraft.client

import com.badlogic.gdx.Game
import org.kraft.client.screen.GameplayScreen
import org.kraft.client.screen.MainMenuScreen

/**
 * Main entry point of the LibGDX application.
 * Manages game screens and state transitions (e.g. main menu, loading, gameplay).
 */
class KraftGame(private val serverHost: String? = null) : Game() {
    override fun create() {
        if (serverHost != null) {
            setScreen(GameplayScreen(this, serverHost))
        } else {
            setScreen(MainMenuScreen(this))
        }
    }
}
