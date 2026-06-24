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
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.viewport.ScreenViewport
import org.kraft.client.KraftGame
import org.kraft.client.player.Player
import org.kraft.client.player.RemotePlayer
import org.kraft.client.player.PlayerController
import org.kraft.input.GameAction
import org.kraft.input.GdxInputService
import org.kraft.input.InputService
import org.kraft.network.*
import org.kraft.physics.VoxelPhysicsEngine
import org.kraft.rendering.WorldRenderer
import org.kraft.world.VoxelWorld
import org.kraft.world.World
import org.kraft.world.BlockType
import org.kraft.world.generator.NoiseTerrainGenerator

/**
 * Game screen handling the active gameplay loop, physics simulation, chunk loading, and frame rendering.
 * Supports both singleplayer mode and TCP multiplayer client mode.
 * Displays a custom pause menu when the cursor is freed via ESC.
 */
class GameplayScreen(private val game: KraftGame, private val serverHost: String? = null) : Screen {
    private lateinit var camera: PerspectiveCamera
    private lateinit var world: VoxelWorld
    private lateinit var worldRenderer: WorldRenderer
    private lateinit var player: Player
    private lateinit var playerController: PlayerController
    private lateinit var inputService: InputService
    private lateinit var physicsEngine: VoxelPhysicsEngine

    private var networkClient: NetworkClient? = null
    private var localPlayerId = -1
    private val remotePlayers = mutableMapOf<Int, RemotePlayer>()
    private var lastSendTime = 0f

    private val chunkLoadRadius = 2

    // In-game Pause Menu UI
    private lateinit var stage: Stage
    private lateinit var skin: Skin

    override fun show() {
        camera = PerspectiveCamera(67f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        camera.near = 0.1f
        camera.far = 300f
        camera.update()

        inputService = GdxInputService()
        inputService.isCursorCatched = true

        if (serverHost != null) {
            val client = NetworkClient(serverHost)
            networkClient = client
            world = World(terrainGenerator = null)
            player = Player()
            
            try {
                client.start()
            } catch (e: Exception) {
                println("Failed to connect to server: ${e.message}")
                game.setScreen(MainMenuScreen(game, "Failed to connect: ${e.message}"))
                return
            }

            playerController = PlayerController(player, world, camera, inputService, onBlockInteracted = { x, y, z, type ->
                client.sendPacket(BlockChangePacket(x, y, z, type.id))
            })
        } else {
            world = World(NoiseTerrainGenerator())
            player = Player()
            playerController = PlayerController(player, world, camera, inputService)
        }

        worldRenderer = WorldRenderer(world)
        physicsEngine = VoxelPhysicsEngine(world)

        // Set up Pause Menu
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

        // Crisp pixel-perfect font setup
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

        // NinePatch background panel (2px cyan border)
        val borderPixmap = Pixmap(12, 12, Pixmap.Format.RGBA8888).apply {
            setColor(Color(0.04f, 0.04f, 0.06f, 0.85f)) // Slightly higher opacity for in-game overlay
            fill()
            setColor(Color(0f, 0.8f, 0.8f, 0.9f))
            drawRectangle(0, 0, 12, 12)
            drawRectangle(1, 1, 10, 10)
        }
        val borderTexture = Texture(borderPixmap)
        borderPixmap.dispose()
        skin.add("card_bg", NinePatch(borderTexture, 3, 3, 3, 3))

        // Custom Buttons (grey border -> cyan on hover)
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

        // Build the layout
        val rootTable = Table().apply {
            setFillParent(true)
        }
        val cardTable = Table().apply {
            background = this@GameplayScreen.skin.getDrawable("card_bg")
            pad(35f)
        }

        val titleLabel = Label("GAME PAUSED", skin, "title")
        cardTable.add(titleLabel).padBottom(25f).row()

        val btnResume = TextButton("Resume Game", skin).apply {
            addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    resumeGame()
                }
            })
        }
        cardTable.add(btnResume).size(240f, 40f).padBottom(12f).row()

        val btnQuit = TextButton("Save and Quit", skin).apply {
            addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    quitToTitle()
                }
            })
        }
        cardTable.add(btnQuit).size(240f, 40f).row()

        rootTable.add(cardTable).width(340f)
        stage.addActor(rootTable)
    }

    private fun pauseGame() {
        inputService.isCursorCatched = false
        Gdx.input.inputProcessor = stage
    }

    private fun resumeGame() {
        inputService.isCursorCatched = true
        Gdx.input.inputProcessor = null
    }

    private fun quitToTitle() {
        networkClient?.stop()
        game.setScreen(MainMenuScreen(game))
    }

    override fun render(delta: Float) {
        networkClient?.let { client ->
            if (!client.running) {
                game.setScreen(MainMenuScreen(game, "Disconnected from server."))
                return
            }
            while (client.packetQueue.isNotEmpty()) {
                val packet = client.packetQueue.poll()
                handleServerPacket(packet)
            }
        }

        // Toggle pause menu using ESC
        if (inputService.isActionJustPressed(GameAction.TOGGLE_CURSOR)) {
            if (inputService.isCursorCatched) {
                pauseGame()
            } else {
                resumeGame()
            }
        }

        val isPaused = !inputService.isCursorCatched

        // Only update local simulation states if not paused
        if (!isPaused) {
            playerController.updateInput(delta)
            physicsEngine.update(player, player.moveInput, delta)
            playerController.updateCameraAndInteraction()
            
            // Replicate player positions at 20Hz if online
            networkClient?.let { client ->
                if (localPlayerId != -1) {
                    lastSendTime += delta
                    if (lastSendTime >= 0.05f) {
                        val yaw = MathUtils.atan2(camera.direction.z, camera.direction.x) * MathUtils.radiansToDegrees
                        client.sendPacket(PlayerPositionPacket(localPlayerId, player.position.x, player.position.y, player.position.z, yaw))
                        lastSendTime = 0f
                    }
                }
            }

            if (serverHost == null) {
                world.loadChunksAround(player.x, player.z, chunkLoadRadius, maxLoadsPerFrame = 1)
            }
        } else {
            // Apply physics/gravity even when paused (stops player movement input, keeps world drop simulation running)
            physicsEngine.update(player, player.moveInput.setZero(), delta)
        }

        // Render 3D World (Still drawn in background when paused)
        Gdx.gl.glClearColor(0.5f, 0.7f, 1.0f, 1.0f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)
        worldRenderer.render(camera, player, playerController, remotePlayers.values)

        // Draw 2D Pause UI Overlay
        if (isPaused) {
            stage.act(delta)
            stage.draw()
        }
    }

    private fun handleServerPacket(packet: Packet) {
        when (packet) {
            is HandshakePacket -> {
                localPlayerId = packet.playerId
                player.position.set(packet.x, packet.y, packet.z)
                println("Joined server as player $localPlayerId at (${packet.x}, ${packet.y}, ${packet.z})")
            }
            is SpawnPlayerPacket -> {
                remotePlayers[packet.playerId] = RemotePlayer(packet.playerId).apply {
                    position.set(packet.x, packet.y, packet.z)
                }
                println("Player ${packet.playerId} joined.")
            }
            is DestroyPlayerPacket -> {
                remotePlayers.remove(packet.playerId)
                println("Player ${packet.playerId} left.")
            }
            is PlayerPositionPacket -> {
                remotePlayers[packet.playerId]?.let { remote ->
                    remote.position.set(packet.x, packet.y, packet.z)
                    remote.yaw = packet.yaw
                }
            }
            is BlockChangePacket -> {
                world.setBlockAt(packet.x, packet.y, packet.z, BlockType.fromId(packet.typeId))
            }
            is ChunkDataPacket -> {
                (world as? World)?.handleChunkData(packet.chunkX, packet.chunkZ, packet.blocks)
            }
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
        networkClient?.stop()
        worldRenderer.dispose()
        stage.dispose()
        skin.dispose()
    }
}
