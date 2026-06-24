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
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle
import com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldStyle
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.viewport.ScreenViewport
import org.kraft.client.KraftGame
import org.kraft.client.player.Player
import org.kraft.client.player.PlayerController
import org.kraft.input.GdxInputService
import org.kraft.rendering.WorldRenderer
import org.kraft.server.GameServer
import org.kraft.world.World
import org.kraft.world.generator.NoiseTerrainGenerator

/**
 * Visual main menu screen displaying a 3D rotating panorama background of the voxel world.
 * Provides intuitive options to start singleplayer, host a local server, or connect to a remote IP address.
 */
class MainMenuScreen(
    private val game: KraftGame,
    private var errorMessage: String? = null
) : Screen {
    private lateinit var stage: Stage
    private lateinit var skin: Skin
    private lateinit var mainTable: Table

    // 3D Panorama Background
    private lateinit var dummyWorld: World
    private lateinit var dummyCamera: PerspectiveCamera
    private lateinit var dummyPlayer: Player
    private lateinit var dummyController: PlayerController
    private lateinit var worldRenderer: WorldRenderer
    private var cameraYaw = 0f

    override fun show() {
        stage = Stage(ScreenViewport())
        Gdx.input.inputProcessor = stage

        // Set up 3D Background
        dummyWorld = World(NoiseTerrainGenerator())
        dummyWorld.loadChunksAround(0f, 0f, radius = 2)

        dummyCamera = PerspectiveCamera(67f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat()).apply {
            near = 0.1f
            far = 200f
        }
        dummyPlayer = Player()
        dummyController = PlayerController(dummyPlayer, dummyWorld, dummyCamera, GdxInputService())
        worldRenderer = WorldRenderer(dummyWorld)

        // Set up UI Skin
        skin = Skin()
        val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888).apply {
            setColor(Color.WHITE)
            fill()
        }
        skin.add("white", Texture(pixmap))
        pixmap.dispose()

        // Load fonts and force NEAREST texture filtering so they are pixel-perfect and not blurry!
        val font = BitmapFont()
        for (i in 0 until font.regions.size) {
            font.regions.get(i).texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        }
        font.data.setScale(1.0f) // Native scale for perfect sharpness
        skin.add("default", font)

        val titleFont = BitmapFont()
        for (i in 0 until titleFont.regions.size) {
            titleFont.regions.get(i).texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        }
        titleFont.data.setScale(3.0f) // 3x scale with Nearest filter for a crisp retro title
        skin.add("title", titleFont)

        // Create modular panel background with 2px cyan border
        val borderPixmap = Pixmap(12, 12, Pixmap.Format.RGBA8888).apply {
            setColor(Color(0.04f, 0.04f, 0.06f, 0.75f))
            fill()
            
            setColor(Color(0f, 0.8f, 0.8f, 0.9f))
            drawRectangle(0, 0, 12, 12)
            drawRectangle(1, 1, 10, 10)
        }
        val borderTexture = Texture(borderPixmap)
        borderPixmap.dispose()
        val patch = NinePatch(borderTexture, 3, 3, 3, 3)
        skin.add("card_bg", patch)

        // Create button up texture (dark grey background, thin border)
        val btnPixmap = Pixmap(12, 12, Pixmap.Format.RGBA8888).apply {
            setColor(Color(0.08f, 0.08f, 0.1f, 0.85f))
            fill()
            setColor(Color(0.2f, 0.2f, 0.25f, 0.9f))
            drawRectangle(0, 0, 12, 12)
        }
        val btnTexture = Texture(btnPixmap)
        btnPixmap.dispose()
        val btnPatch = NinePatch(btnTexture, 2, 2, 2, 2)
        skin.add("btn_up", btnPatch)

        // Create button hover texture (dark grey background, cyan border)
        val btnOverPixmap = Pixmap(12, 12, Pixmap.Format.RGBA8888).apply {
            setColor(Color(0.12f, 0.12f, 0.15f, 0.9f))
            fill()
            setColor(Color(0f, 0.8f, 0.8f, 0.9f))
            drawRectangle(0, 0, 12, 12)
        }
        val btnOverTexture = Texture(btnOverPixmap)
        btnOverPixmap.dispose()
        val btnOverPatch = NinePatch(btnOverTexture, 2, 2, 2, 2)
        skin.add("btn_over", btnOverPatch)

        // Create button pressed texture (dark background, dark cyan border)
        val btnDownPixmap = Pixmap(12, 12, Pixmap.Format.RGBA8888).apply {
            setColor(Color(0.04f, 0.04f, 0.05f, 1.0f))
            fill()
            setColor(Color(0f, 0.5f, 0.5f, 0.9f))
            drawRectangle(0, 0, 12, 12)
        }
        val btnDownTexture = Texture(btnDownPixmap)
        btnDownPixmap.dispose()
        val btnDownPatch = NinePatch(btnDownTexture, 2, 2, 2, 2)
        skin.add("btn_down", btnDownPatch)

        // Create text field texture (dark background, thin border)
        val tfPixmap = Pixmap(12, 12, Pixmap.Format.RGBA8888).apply {
            setColor(Color(0.05f, 0.05f, 0.06f, 0.9f))
            fill()
            setColor(Color(0.15f, 0.15f, 0.18f, 0.9f))
            drawRectangle(0, 0, 12, 12)
        }
        val tfTexture = Texture(tfPixmap)
        tfPixmap.dispose()
        val tfPatch = NinePatch(tfTexture, 2, 2, 2, 2)
        skin.add("tf_bg", tfPatch)
        
        // Create text field focused texture (dark background, cyan border)
        val tfFocusedPixmap = Pixmap(12, 12, Pixmap.Format.RGBA8888).apply {
            setColor(Color(0.08f, 0.08f, 0.1f, 0.95f))
            fill()
            setColor(Color(0f, 0.8f, 0.8f, 0.9f))
            drawRectangle(0, 0, 12, 12)
        }
        val tfFocusedTexture = Texture(tfFocusedPixmap)
        tfFocusedPixmap.dispose()
        val tfFocusedPatch = NinePatch(tfFocusedTexture, 2, 2, 2, 2)
        skin.add("tf_focused_bg", tfFocusedPatch)

        // UI Styles
        val textButtonStyle = TextButtonStyle().apply {
            up = skin.getDrawable("btn_up")
            over = skin.getDrawable("btn_over")
            down = skin.getDrawable("btn_down")
            this.font = font
            fontColor = Color.WHITE
            overFontColor = Color(0f, 0.8f, 0.8f, 1f)
        }
        skin.add("default", textButtonStyle)

        val textFieldStyle = TextFieldStyle().apply {
            background = skin.getDrawable("tf_bg")
            focusedBackground = skin.getDrawable("tf_focused_bg")
            this.font = font
            fontColor = Color.WHITE
            cursor = skin.newDrawable("white", Color(0f, 0.8f, 0.8f, 1f))
            selection = skin.newDrawable("white", Color(0f, 0.5f, 0.5f, 0.5f))
        }
        skin.add("default", textFieldStyle)

        val labelStyle = LabelStyle().apply {
            this.font = font
            fontColor = Color.WHITE
        }
        skin.add("default", labelStyle)

        val titleLabelStyle = LabelStyle().apply {
            this.font = titleFont
            fontColor = Color(0f, 0.8f, 0.8f, 1f)
        }
        skin.add("title", titleLabelStyle)

        mainTable = Table()
        mainTable.setFillParent(true)
        stage.addActor(mainTable)

        buildMainMenu()
    }

    private fun buildMainMenu() {
        mainTable.clear()

        val cardTable = Table().apply {
            background = this@MainMenuScreen.skin.getDrawable("card_bg")
            pad(35f)
        }

        val titleLabel = Label("KRAFT", skin, "title")
        cardTable.add(titleLabel).padBottom(30f).row()

        if (errorMessage != null) {
            val errorLabel = Label(errorMessage, skin).apply {
                setColor(Color(1f, 0.3f, 0.3f, 1f)) // nice soft red for errors
            }
            cardTable.add(errorLabel).padBottom(15f).row()
            errorMessage = null
        }

        val btnSingleplayer = TextButton("Singleplayer", skin).apply {
            addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    game.setScreen(GameplayScreen(game, null))
                }
            })
        }
        cardTable.add(btnSingleplayer).size(240f, 40f).padBottom(12f).row()

        val btnHost = TextButton("Host Server", skin).apply {
            addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    Thread({
                        try {
                            GameServer(25565).start()
                        } catch (e: Exception) {
                            println("Local server port 25565 already in use or error: ${e.message}")
                        }
                    }, "LocalServer-Thread").start()

                    // Let the server socket bind before attempting to connect
                    try {
                        Thread.sleep(150)
                    } catch (e: InterruptedException) {
                        Thread.currentThread().interrupt()
                    }

                    game.setScreen(GameplayScreen(game, "127.0.0.1"))
                }
            })
        }
        cardTable.add(btnHost).size(240f, 40f).padBottom(12f).row()

        val btnConnectMenu = TextButton("Connect to Server", skin).apply {
            addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    buildConnectMenu()
                }
            })
        }
        cardTable.add(btnConnectMenu).size(240f, 40f).padBottom(12f).row()

        val btnExit = TextButton("Exit Game", skin).apply {
            addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    Gdx.app.exit()
                }
            })
        }
        cardTable.add(btnExit).size(240f, 40f).row()

        mainTable.add(cardTable).width(340f)
    }

    private fun buildConnectMenu() {
        mainTable.clear()

        val cardTable = Table().apply {
            background = this@MainMenuScreen.skin.getDrawable("card_bg")
            pad(35f)
        }

        val titleLabel = Label("KRAFT", skin, "title")
        cardTable.add(titleLabel).padBottom(20f).row()

        val ipLabel = Label("Server IP Address:", skin)
        cardTable.add(ipLabel).padBottom(10f).row()

        val ipField = TextField("127.0.0.1", skin)
        cardTable.add(ipField).size(240f, 40f).padBottom(20f).row()

        val btnConnect = TextButton("Connect", skin).apply {
            addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    val host = ipField.text.trim()
                    if (host.isNotEmpty()) {
                        game.setScreen(GameplayScreen(game, host))
                    }
                }
            })
        }
        cardTable.add(btnConnect).size(240f, 40f).padBottom(12f).row()

        val btnBack = TextButton("Back", skin).apply {
            addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    buildMainMenu()
                }
            })
        }
        cardTable.add(btnBack).size(240f, 40f).row()

        mainTable.add(cardTable).width(340f)
    }

    override fun render(delta: Float) {
        // Render 3D Panorama Background
        cameraYaw += delta * 6f
        dummyCamera.position.set(
            12f * MathUtils.cosDeg(cameraYaw),
            22f + 2f * MathUtils.sinDeg(cameraYaw * 0.5f),
            12f * MathUtils.sinDeg(cameraYaw)
        )
        dummyCamera.lookAt(0f, 15f, 0f)
        dummyCamera.up.set(0f, 1f, 0f)
        dummyCamera.update()

        Gdx.gl.glClearColor(0.5f, 0.7f, 1.0f, 1.0f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

        worldRenderer.render(dummyCamera, dummyPlayer, dummyController, emptyList())

        // Render 2D UI Stage
        stage.act(delta)
        stage.draw()
    }

    override fun resize(width: Int, height: Int) {
        stage.viewport.update(width, height, true)
        dummyCamera.viewportWidth = width.toFloat()
        dummyCamera.viewportHeight = height.toFloat()
        dummyCamera.update()
    }

    override fun pause() {}

    override fun resume() {}

    override fun hide() {
        Gdx.input.inputProcessor = null
    }

    override fun dispose() {
        stage.dispose()
        skin.dispose()
        worldRenderer.dispose()
    }
}
