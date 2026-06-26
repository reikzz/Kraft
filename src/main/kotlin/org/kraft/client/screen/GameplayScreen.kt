package org.kraft.client.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.NinePatch
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.badlogic.gdx.InputMultiplexer
import org.kraft.client.GameSession
import org.kraft.client.KraftGame
import org.kraft.client.input.GameAction
import org.kraft.client.input.GdxInputService
import org.kraft.client.rendering.WorldRenderer

class GameplayScreen(private val game: KraftGame, private val serverHost: String? = null) : Screen {
    private lateinit var camera: PerspectiveCamera
    private lateinit var session: GameSession
    private lateinit var worldRenderer: WorldRenderer
    private lateinit var inputService: GdxInputService

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

        val multiplexer = InputMultiplexer(inputService)
        Gdx.input.inputProcessor = multiplexer

        inputService.isCursorCatched = true

        setupPauseMenu()
    }

    private fun setupPauseMenu() {
        stage = Stage(ScreenViewport())

        skin = Skin()
        val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888).apply {
            setColor(Color.WHITE)
            fill()
        }
        skin.add("white", Texture(pixmap))
        pixmap.dispose()

        val font = BitmapFont()
        for (i in 0 until font.regions.size) {
            font.regions.get(i).texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        }
        font.data.setScale(1.0f)
        skin.add("default", font)

        val titleFont = BitmapFont()
        for (i in 0 until titleFont.regions.size) {
            titleFont.regions.get(i).texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        }
        titleFont.data.setScale(2.0f)
        skin.add("title", titleFont)

        val borderPixmap = Pixmap(12, 12, Pixmap.Format.RGBA8888).apply {
            setColor(Color(0.04f, 0.04f, 0.06f, 0.85f))
            fill()
            setColor(Color(0f, 0.8f, 0.8f, 0.9f))
            drawRectangle(0, 0, 12, 12)
            drawRectangle(1, 1, 10, 10)
        }
        val borderTexture = Texture(borderPixmap)
        borderPixmap.dispose()
        skin.add("card_bg", NinePatch(borderTexture, 3, 3, 3, 3))

        val btnPixmap = Pixmap(12, 12, Pixmap.Format.RGBA8888).apply {
            setColor(Color(0.08f, 0.08f, 0.1f, 0.9f))
            fill()
            setColor(Color(0.2f, 0.2f, 0.25f, 0.9f))
            drawRectangle(0, 0, 12, 12)
        }
        val btnTexture = Texture(btnPixmap)
        btnPixmap.dispose()
        skin.add("btn_up", NinePatch(btnTexture, 2, 2, 2, 2))

        val btnOverPixmap = Pixmap(12, 12, Pixmap.Format.RGBA8888).apply {
            setColor(Color(0.12f, 0.12f, 0.15f, 0.95f))
            fill()
            setColor(Color(0f, 0.8f, 0.8f, 0.9f))
            drawRectangle(0, 0, 12, 12)
        }
        val btnOverTexture = Texture(btnOverPixmap)
        btnOverPixmap.dispose()
        skin.add("btn_over", NinePatch(btnOverTexture, 2, 2, 2, 2))

        val btnDownPixmap = Pixmap(12, 12, Pixmap.Format.RGBA8888).apply {
            setColor(Color(0.04f, 0.04f, 0.05f, 1.0f))
            fill()
            setColor(Color(0f, 0.5f, 0.5f, 0.9f))
            drawRectangle(0, 0, 12, 12)
        }
        val btnDownTexture = Texture(btnDownPixmap)
        btnDownPixmap.dispose()
        skin.add("btn_down", NinePatch(btnDownTexture, 2, 2, 2, 2))

        val textButtonStyle = TextButtonStyle().apply {
            up = skin.getDrawable("btn_up")
            over = skin.getDrawable("btn_over")
            down = skin.getDrawable("btn_down")
            this.font = font
            fontColor = Color.WHITE
            overFontColor = Color(0f, 0.8f, 0.8f, 1f)
        }
        skin.add("default", textButtonStyle)

        val labelStyle = LabelStyle(font, Color.WHITE)
        skin.add("default", labelStyle)

        val titleStyle = LabelStyle(titleFont, Color(0f, 0.8f, 0.8f, 1f))
        skin.add("title", titleStyle)

        val rootTable = Table().apply { setFillParent(true) }
        val cardTable = Table().apply {
            background = this@GameplayScreen.skin.getDrawable("card_bg")
            pad(35f)
        }

        cardTable.add(Label("GAME PAUSED", skin, "title")).padBottom(25f).row()

        val btnResume = TextButton("Resume Game", skin).apply {
            addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) { resumeGame() }
            })
        }
        cardTable.add(btnResume).size(240f, 40f).padBottom(12f).row()

        val btnQuit = TextButton("Save and Quit", skin).apply {
            addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) { quitToTitle() }
            })
        }
        cardTable.add(btnQuit).size(240f, 40f).row()

        rootTable.add(cardTable).width(340f)
        stage.addActor(rootTable)
    }

    private val isPaused: Boolean get() = !inputService.isCursorCatched

    private fun pauseGame() {
        inputService.isCursorCatched = false

        (Gdx.input.inputProcessor as? InputMultiplexer)?.let { mux ->
            if (!mux.processors.contains(stage)) mux.addProcessor(0, stage)
        }
    }

    private fun resumeGame() {
        inputService.isCursorCatched = true
        (Gdx.input.inputProcessor as? InputMultiplexer)?.removeProcessor(stage)
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
        stage.dispose()
        skin.dispose()
    }
}
