package org.kraft.client

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import org.kraft.server.GameServer

/**
 * Main application launcher for Desktop.
 * Can start either a dedicated server or a game client depending on launch arguments.
 */
fun main(args: Array<String>) {
    if (args.isNotEmpty()) {
        if (args[0].lowercase() == "server") {
            val port = args.getOrNull(1)?.toIntOrNull() ?: 25565
            GameServer(port).start()
            return
        }
    }

    val serverHost = args.getOrNull(0)

    val config = Lwjgl3ApplicationConfiguration().apply {
        setTitle("Kraft")
        setWindowedMode(800, 600)
        useVsync(true)
    }

    Lwjgl3Application(KraftGame(serverHost), config)
}
