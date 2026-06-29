package org.kraft.client.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.utils.viewport.ScreenViewport
import org.kraft.client.KraftGame
import org.kraft.client.ui.UiBuilder
import org.kraft.client.ui.UiStyleRegistry

class MainMenuScreen(
    private val game: KraftGame,
    private var errorMessage: String? = null
) : Screen {
    private lateinit var stage: Stage
    private lateinit var skin: Skin
    private lateinit var uiBuilder: UiBuilder

    override fun show() {
        stage = Stage(ScreenViewport())
        skin = UiStyleRegistry.createBasicSkin()

        uiBuilder = UiBuilder(skin)
        uiBuilder.targetStage = stage

        showMainMenu()

        Gdx.input.inputProcessor = stage
    }

    private fun showMainMenu() {
        uiBuilder.changeView("main_menu.xml") {
            errorMessage?.let { msg ->
                find<Label>("lblError")?.run {
                    setText(msg)
                    setColor(com.badlogic.gdx.graphics.Color.RED)
                }
            }

            onClick("btnPlay") {
                game.setScreen(GameplayScreen(game, serverHost = null))
            }

            onClick("btnMultiplayer") {
                game.setScreen(GameplayScreen(game, serverHost = "localhost"))
            }

            onClick("btnSettings") {
                showSettingsMenu()
            }

            onClick("btnExit") {
                Gdx.app.exit()
            }
        }
    }

    private fun showSettingsMenu() {
        uiBuilder.changeView("core/settings_menu.xml") {

            onClick("btnBack") {
                showMainMenu()
            }
        }
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1.0f)
        Gdx.gl.glClear(com.badlogic.gdx.graphics.GL20.GL_COLOR_BUFFER_BIT)

        stage.act(delta)
        stage.draw()
    }

    override fun resize(width: Int, height: Int) {
        stage.viewport.update(width, height, true)
    }

    override fun pause() {}
    override fun resume() {}

    override fun hide() {
        Gdx.input.inputProcessor = null
    }

    override fun dispose() {
        if (::stage.isInitialized) stage.dispose()
        if (::skin.isInitialized) skin.dispose()
    }
}