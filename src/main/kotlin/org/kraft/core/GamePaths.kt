package org.kraft.core

import java.io.File

object GamePaths {
    val rootDir: File by lazy {
        val os = System.getProperty("os.name").lowercase()
        val userHome = System.getProperty("user.home")
        val appDirName = ".kraft"

        val dir = when {
            os.contains("win") -> {
                val appData = System.getenv("APPDATA") ?: userHome
                File(appData, appDirName)
            }
            os.contains("mac") -> File(userHome, "Library/Application Support/kraft")
            else -> File(userHome, appDirName)
        }

        if (!dir.exists()) {
            dir.mkdirs()
        }
        dir
    }

    val savesDir: File by lazy {
        File(rootDir, "saves").apply { if (!this.exists()) mkdirs() }
    }
    
    val modsDir: File by lazy {
        File(rootDir, "mods").apply { if (!this.exists()) mkdirs() }
    }

    fun getWorldDir(worldName: String): File {
        return File(savesDir, worldName).apply { if (!this.exists()) mkdirs() }
    }
}
