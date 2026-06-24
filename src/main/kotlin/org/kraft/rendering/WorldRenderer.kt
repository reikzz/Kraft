package org.kraft.rendering

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
import org.kraft.math.VoxelRaycaster
import org.kraft.world.Chunk
import org.kraft.world.ChunkCoordinate
import org.kraft.world.World
import org.kraft.world.WorldListener
import org.kraft.world.BlockType

class WorldRenderer(
    private val world: World,
) : WorldListener, Disposable {
    private val modelBatch = ModelBatch()
    private val blockMaterials = BlockMaterialRegistry()
    private val chunkMeshBuilder = ChunkMeshBuilder(blockMaterials, world)
    private val renderedChunks = mutableMapOf<ChunkCoordinate, RenderedChunk>()
    private val shapeRenderer = ShapeRenderer()
    private val environment = Environment().apply {
        set(ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f))
        add(DirectionalLight().set(0.8f, 0.8f, 0.8f, -0.5f, -1f, -0.3f))
    }

    init {
        world.addListener(this)
        world.loadedChunks.forEach { chunk ->
            buildChunkMesh(ChunkCoordinate(chunk.chunkX, chunk.chunkZ))
        }
    }

    override fun onBlockChanged(x: Int, y: Int, z: Int, oldType: BlockType, newType: BlockType) {
        val chunkX = Math.floorDiv(x, Chunk.WIDTH)
        val chunkZ = Math.floorDiv(z, Chunk.DEPTH)
        rebuildChunkWithNeighbors(ChunkCoordinate(chunkX, chunkZ))
    }

    override fun onChunkLoaded(chunkX: Int, chunkZ: Int) {
        rebuildChunkWithNeighbors(ChunkCoordinate(chunkX, chunkZ))
    }

    private fun rebuildChunkWithNeighbors(coordinate: ChunkCoordinate) {
        val targets = listOf(
            coordinate,
            ChunkCoordinate(coordinate.x + 1, coordinate.z),
            ChunkCoordinate(coordinate.x - 1, coordinate.z),
            ChunkCoordinate(coordinate.x, coordinate.z + 1),
            ChunkCoordinate(coordinate.x, coordinate.z - 1)
        )
        targets.forEach { buildChunkMesh(it) }
    }

    private fun buildChunkMesh(coordinate: ChunkCoordinate) {
        val chunk = world.getChunk(coordinate) ?: return
        renderedChunks.remove(coordinate)?.dispose()
        val model = chunkMeshBuilder.build(chunk)
        renderedChunks[coordinate] = RenderedChunk(model, ModelInstance(model))
    }

    fun render(camera: PerspectiveCamera, targetBlock: VoxelRaycaster.Result? = null) {
        modelBatch.begin(camera)
        renderedChunks.values.forEach { renderedChunk ->
            modelBatch.render(renderedChunk.instance, environment)
        }
        modelBatch.end()

        // Draw 3D wireframe selection outline around targeted block
        if (targetBlock != null) {
            shapeRenderer.projectionMatrix = camera.combined
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
            shapeRenderer.color = Color.BLACK
            
            // Add a small offset to prevent Z-fighting with block geometry faces
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
            shapeRenderer.end()
        }

        // Draw 2D crosshair in the center of the screen when cursor is caught
        if (Gdx.input.isCursorCatched) {
            shapeRenderer.projectionMatrix = shapeRenderer.projectionMatrix.setToOrtho2D(
                0f, 0f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat()
            )
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
            shapeRenderer.color = Color.WHITE
            
            val centerX = Gdx.graphics.width / 2f
            val centerY = Gdx.graphics.height / 2f
            val size = 10f
            
            shapeRenderer.line(centerX - size, centerY, centerX + size, centerY)
            shapeRenderer.line(centerX, centerY - size, centerX, centerY + size)
            shapeRenderer.end()
        }
    }

    override fun dispose() {
        world.removeListener(this)
        modelBatch.dispose()
        renderedChunks.values.forEach(RenderedChunk::dispose)
        blockMaterials.dispose()
        shapeRenderer.dispose()
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
