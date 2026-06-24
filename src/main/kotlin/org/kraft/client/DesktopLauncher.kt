package org.kraft.client

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration

fun main() {
    val config = Lwjgl3ApplicationConfiguration().apply {
        setTitle("Kraft")
        setWindowedMode(800, 600)
        useVsync(true)
    }

    Lwjgl3Application(KraftGame(), config)
}