package org.kraft.client.rendering

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.math.Vector3
import org.kraft.client.player.Player
import org.kraft.client.player.PlayerController
import org.kraft.client.player.RemotePlayer
import org.kraft.world.Chunk
import org.kraft.world.ChunkCoordinate
import org.kraft.world.VoxelWorld
import org.kraft.world.subscribe
import org.kraft.event.ChunkUnloadedEvent
import org.kraft.event.Subscription
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.VertexAttributes.Usage
import org.kraft.event.BlockChangedEvent

/**
 * Handles the rendering of the 3D voxel world, targeted block highlights, and hud.
 *
 * Chunk mesh rebuilding is batched per-frame using [Chunk.isDirty] flags rather than
 * being triggered synchronously inside event callbacks. This prevents frame hitches when
 * many blocks change in one tick (e.g. structure generation, explosions).
 *
 * At most [MAX_REBUILDS_PER_FRAME] chunks are rebuilt per frame to spread the cost.
 */
class WorldRenderer(
    private val world: VoxelWorld,
) : Disposable {
    private val modelBatch = ModelBatch()
    private val blockMaterials = BlockMaterialRegistry()
    private val chunkMeshBuilder = ChunkMeshBuilder(blockMaterials, world)
    private val renderedChunks = mutableMapOf<ChunkCoordinate, RenderedChunk>()
    private val shapeRenderer = ShapeRenderer()
    private val hudRenderer = HudRenderer(blockMaterials)
    private val environment = Environment().apply {
        set(ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f))
        add(DirectionalLight().set(0.8f, 0.8f, 0.8f, -0.5f, -1f, -0.3f))
    }
    private val subscriptions = mutableListOf<Subscription>()

    private val playerModel: Model
    private val playerInstance: ModelInstance

    companion object {
        /** Maximum chunk meshes rebuilt per render frame to avoid frame-time spikes. */
        private const val MAX_REBUILDS_PER_FRAME = 4
    }

    init {
        val modelBuilder = ModelBuilder()
        modelBuilder.begin()
        val attributes = (Usage.Position or Usage.Normal).toLong()

        // Head (Skin color)
        modelBuilder.node().apply {
            id = "head"
            translation.set(0f, 1.5f, 0f)
        }
        modelBuilder.part("headBox", GL20.GL_TRIANGLES, attributes, Material(ColorAttribute.createDiffuse(Color(0.95f, 0.8f, 0.65f, 1f))))
            .box(0.4f, 0.4f, 0.4f)

        // Torso (Cyan/Teal shirt)
        modelBuilder.node().apply {
            id = "torso"
            translation.set(0f, 0.95f, 0f)
        }
        modelBuilder.part("torsoBox", GL20.GL_TRIANGLES, attributes, Material(ColorAttribute.createDiffuse(Color(0f, 0.7f, 0.7f, 1f))))
            .box(0.5f, 0.7f, 0.25f)

        // Left Leg (Blue pants)
        modelBuilder.node().apply {
            id = "leftLeg"
            translation.set(-0.15f, 0.3f, 0f)
        }
        modelBuilder.part("leftLegBox", GL20.GL_TRIANGLES, attributes, Material(ColorAttribute.createDiffuse(Color(0.2f, 0.3f, 0.8f, 1f))))
            .box(0.2f, 0.6f, 0.2f)

        // Right Leg (Blue pants)
        modelBuilder.node().apply {
            id = "rightLeg"
            translation.set(0.15f, 0.3f, 0f)
        }
        modelBuilder.part("rightLegBox", GL20.GL_TRIANGLES, attributes, Material(ColorAttribute.createDiffuse(Color(0.2f, 0.3f, 0.8f, 1f))))
            .box(0.2f, 0.6f, 0.2f)

        // Left Arm (Cyan/Teal sleeve)
        modelBuilder.node().apply {
            id = "leftArm"
            translation.set(-0.35f, 0.95f, 0f)
        }
        modelBuilder.part("leftArmBox", GL20.GL_TRIANGLES, attributes, Material(ColorAttribute.createDiffuse(Color(0f, 0.7f, 0.7f, 1f))))
            .box(0.15f, 0.7f, 0.15f)

        // Right Arm (Cyan/Teal sleeve)
        modelBuilder.node().apply {
            id = "rightArm"
            translation.set(0.35f, 0.95f, 0f)
        }
        modelBuilder.part("rightArmBox", GL20.GL_TRIANGLES, attributes, Material(ColorAttribute.createDiffuse(Color(0f, 0.7f, 0.7f, 1f))))
            .box(0.15f, 0.7f, 0.15f)

        playerModel = modelBuilder.end()
        playerInstance = ModelInstance(playerModel)

        // When a block changes, mark that chunk and its four horizontal neighbours dirty
        // so border faces between chunks are re-evaluated (face culling at chunk seams).
        subscriptions.add(world.subscribe<BlockChangedEvent> { event ->
            val chunkX = Math.floorDiv(event.x, Chunk.WIDTH)
            val chunkZ = Math.floorDiv(event.z, Chunk.DEPTH)
            val coord = ChunkCoordinate(chunkX, chunkZ)
            world.getChunk(coord)?.isDirty = true
            markNeighboursDirty(coord)
        })

        // When a chunk is unloaded, drop its rendered mesh and mark neighbours dirty.
        subscriptions.add(world.subscribe<ChunkUnloadedEvent> { event ->
            val coordinate = ChunkCoordinate(event.chunkX, event.chunkZ)
            renderedChunks.remove(coordinate)?.dispose()
            // Mark neighbours dirty so their border faces are re-evaluated.
            markNeighboursDirty(coordinate)
        })

        // Build initial meshes for any already-loaded chunks (shouldn't be any on first
        // construction, but handles hot-reload scenarios gracefully).
        world.loadedChunks.forEach { chunk ->
            buildChunkMesh(ChunkCoordinate(chunk.chunkX, chunk.chunkZ))
        }
    }

    /**
     * Marks the four horizontal neighbours of [coordinate] as dirty so their border faces
     * are rebuilt on the next frame (needed when a chunk appears or disappears).
     */
    private fun markNeighboursDirty(coordinate: ChunkCoordinate) {
        listOf(
            ChunkCoordinate(coordinate.x + 1, coordinate.z),
            ChunkCoordinate(coordinate.x - 1, coordinate.z),
            ChunkCoordinate(coordinate.x, coordinate.z + 1),
            ChunkCoordinate(coordinate.x, coordinate.z - 1)
        ).forEach { neighbour ->
            world.getChunk(neighbour)?.isDirty = true
        }
    }

    private fun buildChunkMesh(coordinate: ChunkCoordinate) {
        val chunk = world.getChunk(coordinate) ?: return
        renderedChunks.remove(coordinate)?.dispose()
        val model = chunkMeshBuilder.build(chunk)
        renderedChunks[coordinate] = RenderedChunk(model, ModelInstance(model))
        chunk.isDirty = false
    }

    /**
     * Rebuilds up to [MAX_REBUILDS_PER_FRAME] dirty chunk meshes each frame, spreading
     * the cost across multiple frames instead of spiking on a single one.
     */
    private fun rebuildDirtyChunks() {
        var rebuilt = 0
        for (chunk in world.loadedChunks) {
            if (rebuilt >= MAX_REBUILDS_PER_FRAME) break
            if (chunk.isDirty) {
                buildChunkMesh(ChunkCoordinate(chunk.chunkX, chunk.chunkZ))
                rebuilt++
            }
        }
    }

    /**
     * Renders the 3D voxel chunks, block selection outline, and delegates to [HudRenderer].
     */
    fun render(
        camera: PerspectiveCamera,
        player: Player,
        controller: PlayerController,
        remotePlayers: Collection<RemotePlayer> = emptyList()
    ) {
        rebuildDirtyChunks()

        modelBatch.begin(camera)
        renderedChunks.forEach { (coord, renderedChunk) ->
            val centerX = coord.x * Chunk.WIDTH + (Chunk.WIDTH / 2f)
            val centerY = Chunk.HEIGHT / 2f
            val centerZ = coord.z * Chunk.DEPTH + (Chunk.DEPTH / 2f)
            
            val halfWidth = Chunk.WIDTH / 2f
            val halfHeight = Chunk.HEIGHT / 2f
            val halfDepth = Chunk.DEPTH / 2f
            
            if (camera.frustum.boundsInFrustum(centerX, centerY, centerZ, halfWidth, halfHeight, halfDepth)) {
                modelBatch.render(renderedChunk.instance, environment)
            }
        }
        remotePlayers.forEach { remotePlayer ->
            playerInstance.transform.setToTranslation(
                remotePlayer.position.x,
                remotePlayer.position.y,
                remotePlayer.position.z
            ).rotate(Vector3.Y, remotePlayer.yaw)
            modelBatch.render(playerInstance, environment)
        }
        modelBatch.end()

        val targetBlock = controller.targetBlock
        if (targetBlock != null) {
            shapeRenderer.projectionMatrix = camera.combined
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
            
            // Draw normal outline
            shapeRenderer.color = Color.BLACK
            val offset = 0.005f
            val outlineSize = 1f + 2f * offset
            shapeRenderer.box(
                targetBlock.hitX.toFloat() - offset,
                targetBlock.hitY.toFloat() - offset,
                targetBlock.hitZ.toFloat() + 1f + offset,
                outlineSize,
                outlineSize,
                outlineSize
            )
            
            // Draw mining progress box
            if (controller.miningProgress > 0f) {
                shapeRenderer.color = Color.RED
                val progressSize = controller.miningProgress * 1.05f
                val pOffset = (1f - progressSize) / 2f
                shapeRenderer.box(
                    targetBlock.hitX.toFloat() + pOffset,
                    targetBlock.hitY.toFloat() + pOffset,
                    targetBlock.hitZ.toFloat() + 1f - pOffset,
                    progressSize,
                    progressSize,
                    progressSize
                )
            }
            shapeRenderer.end()
        }

        hudRenderer.render(player, controller, Gdx.graphics.deltaTime)
    }

    override fun dispose() {
        subscriptions.forEach { it.unsubscribe() }
        subscriptions.clear()
        modelBatch.dispose()
        renderedChunks.values.forEach(RenderedChunk::dispose)
        playerModel.dispose()
        blockMaterials.dispose()
        shapeRenderer.dispose()
        hudRenderer.dispose()
    }

    private class RenderedChunk(
        val model: Model,
        val instance: ModelInstance,
    ) : Disposable {
        override fun dispose() {
            model.dispose()
        }
    }
}
