package org.kraft.client

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.g3d.utils.FirstPersonCameraController
import org.kraft.rendering.WorldRenderer
import org.kraft.world.World
import org.kraft.world.generator.NoiseTerrainGenerator

class KraftGame : ApplicationAdapter() {
    private lateinit var camera: PerspectiveCamera
    private lateinit var camController: FreeLookCameraController
    private lateinit var world: World
    private lateinit var worldRenderer: WorldRenderer
    private lateinit var player: Player
    private lateinit var playerController: PlayerController

    private val chunkLoadRadius = 2

    override fun create() {
        camera = PerspectiveCamera(67f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        camera.near = 0.1f
        camera.far = 300f
        camera.update()

        camController = FreeLookCameraController(camera)
        camController.setVelocity(0f)
        camController.setDegreesPerPixel(0.3f)
        Gdx.input.inputProcessor = camController
        Gdx.input.isCursorCatched = true

        world = World(NoiseTerrainGenerator())
        worldRenderer = WorldRenderer(world)
        
        player = Player(world)
        playerController = PlayerController(player, world, camera)
    }

    override fun render() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.input.isCursorCatched = !Gdx.input.isCursorCatched
        }

        camController.update()

        playerController.updateInput(Gdx.graphics.deltaTime)

        player.update(Gdx.graphics.deltaTime)

        playerController.updateCameraAndInteraction()

        world.loadChunksAround(player.x, player.z, chunkLoadRadius)

        Gdx.gl.glClearColor(0.5f, 0.7f, 1.0f, 1.0f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

        worldRenderer.render(camera, playerController.targetBlock)
    }

    override fun resize(width: Int, height: Int) {
        camera.viewportWidth = width.toFloat()
        camera.viewportHeight = height.toFloat()
        camera.update()
    }

    override fun dispose() {
        worldRenderer.dispose()
    }
}
