package org.kraft.client.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.badlogic.gdx.InputMultiplexer
import org.kraft.client.GameSession
import org.kraft.client.KraftGame
import org.kraft.client.input.GameAction
import org.kraft.client.input.GdxInputService
import org.kraft.client.rendering.WorldRenderer
import org.kraft.client.ui.UiBuilder
import org.kraft.client.ui.UiStyleRegistry

class GameplayScreen(private val game: KraftGame, private val serverHost: String? = null) : Screen {
    private lateinit var camera: PerspectiveCamera
    private lateinit var session: GameSession
    private lateinit var worldRenderer: WorldRenderer
    private lateinit var inputService: GdxInputService
    private lateinit var uiBuilder: UiBuilder

    private lateinit var stage: Stage
    private lateinit var skin: Skin

    override fun show() {
        camera = PerspectiveCamera(67f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        camera.near = 0.1f
        camera.far = 300f
        camera.update()

        inputService = GdxInputService()
        session = GameSession(serverHost, inputService, camera)

        try {
            session.connect()
        } catch (e: Exception) {
            session.dispose()
            game.setScreen(MainMenuScreen(game, "Failed to connect: ${e.message}"))
            return
        }

        worldRenderer = WorldRenderer(session.world)

        stage = Stage(ScreenViewport())
        skin = UiStyleRegistry.createBasicSkin()
        uiBuilder = UiBuilder(skin)
        uiBuilder.targetStage = stage

        val multiplexer = InputMultiplexer(stage, inputService)
        Gdx.input.inputProcessor = multiplexer

        resumeGame()
    }

    private fun showPauseMenuUi() {
        uiBuilder.changeView("pause_menu.xml") {
            onClick("btnResume") {
                resumeGame()
            }

            onClick("btnBack") {
                quitToTitle()
            }
        }
    }

    private val isPaused: Boolean get() = !inputService.isCursorCatched

    private fun pauseGame() {
        inputService.isCursorCatched = false

        showPauseMenuUi()
    }

    private fun resumeGame() {
        inputService.isCursorCatched = true

        uiBuilder.closeCurrentView()
    }

    private fun quitToTitle() {
        session.dispose()
        game.setScreen(MainMenuScreen(game))
    }

    override fun render(delta: Float) {
        if (!session.processNetworkPackets()) {
            game.setScreen(MainMenuScreen(game, "Disconnected from server."))
            return
        }

        if (inputService.isActionJustPressed(GameAction.TOGGLE_CURSOR)) {
            if (isPaused) resumeGame() else pauseGame()
        }

        if (!isPaused) {
            session.update(delta)
        } else {
            session.updatePaused(delta)
        }

        Gdx.gl.glClearColor(0.5f, 0.7f, 1.0f, 1.0f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)
        worldRenderer.render(camera, session.player, session.playerController, session.remotePlayers.values)

        if (isPaused) {
            stage.act(delta)
            stage.draw()
        }
    }

    override fun resize(width: Int, height: Int) {
        camera.viewportWidth = width.toFloat()
        camera.viewportHeight = height.toFloat()
        camera.update()
        stage.viewport.update(width, height, true)
    }

    override fun pause() {}
    override fun resume() {}

    override fun hide() {
        Gdx.input.inputProcessor = null
    }

    override fun dispose() {
        session.dispose()
        worldRenderer.dispose()
        if (::stage.isInitialized) stage.dispose()
        if (::skin.isInitialized) skin.dispose()
    }
}