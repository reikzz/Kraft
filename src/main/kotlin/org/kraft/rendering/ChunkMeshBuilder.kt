package org.kraft.rendering

import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.VertexAttributes.Usage
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder.VertexInfo
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Vector3
import org.kraft.world.BlockType
import org.kraft.world.Chunk
import org.kraft.world.World

/**
 * Builds 3D meshes for individual world chunks.
 * 
 * Optimizes vertex count via face culling: a block face is generated only if
 * the adjacent block in the direction of the face's normal is transparent (AIR).
 * 
 * Generates geometry sequentially by block type to work around LibGDX's internal
 * mesh building material interleaving issue.
 */
class ChunkMeshBuilder(
    private val materials: BlockMaterialRegistry,
    private val world: World,
) {
    private val vertexAttributes = (Usage.Position or Usage.Normal or Usage.TextureCoordinates).toLong()

    fun build(chunk: Chunk): Model {
        val modelBuilder = ModelBuilder()
        modelBuilder.begin()

        for (blockType in BlockType.entries) {
            if (blockType == BlockType.AIR) continue

            var hasBlock = false
            for (x in 0 until Chunk.WIDTH) {
                for (y in 0 until Chunk.HEIGHT) {
                    for (z in 0 until Chunk.DEPTH) {
                        if (chunk.getBlock(x, y, z) == blockType) {
                            hasBlock = true
                            break
                        }
                    }
                    if (hasBlock) break
                }
                if (hasBlock) break
            }

            if (!hasBlock) continue

            val partBuilder = modelBuilder.part(
                blockType.name.lowercase(),
                GL20.GL_TRIANGLES,
                vertexAttributes,
                materials.materialFor(blockType),
            )

            for (x in 0 until Chunk.WIDTH) {
                for (y in 0 until Chunk.HEIGHT) {
                    for (z in 0 until Chunk.DEPTH) {
                        if (chunk.getBlock(x, y, z) == blockType) {
                            addVisibleFaces(partBuilder, chunk, x, y, z)
                        }
                    }
                }
            }
        }

        return modelBuilder.end()
    }

    private fun addVisibleFaces(partBuilder: MeshPartBuilder, chunk: Chunk, x: Int, y: Int, z: Int) {
        for (face in BlockFace.entries) {
            val globalX = chunk.chunkX * Chunk.WIDTH + x + face.offsetX
            val globalY = y + face.offsetY
            val globalZ = chunk.chunkZ * Chunk.DEPTH + z + face.offsetZ

            if (world.getBlockAt(globalX, globalY, globalZ) != BlockType.AIR) continue

            val worldX = (chunk.chunkX * Chunk.WIDTH + x).toFloat()
            val worldY = y.toFloat()
            val worldZ = (chunk.chunkZ * Chunk.DEPTH + z).toFloat()

            face.addTo(partBuilder, worldX, worldY, worldZ)
        }
    }

    private enum class BlockFace(
        val offsetX: Int,
        val offsetY: Int,
        val offsetZ: Int,
        val normal: Vector3,
        private val vertices: Array<Vector3>,
    ) {
        Top(
            0,
            1,
            0,
            Vector3.Y,
            arrayOf(Vector3(0f, 1f, 1f), Vector3(1f, 1f, 1f), Vector3(1f, 1f, 0f), Vector3(0f, 1f, 0f)),
        ),
        Bottom(
            0,
            -1,
            0,
            Vector3.Y.cpy().scl(-1f),
            arrayOf(Vector3(0f, 0f, 0f), Vector3(1f, 0f, 0f), Vector3(1f, 0f, 1f), Vector3(0f, 0f, 1f)),
        ),
        North(
            0,
            0,
            -1,
            Vector3.Z.cpy().scl(-1f),
            arrayOf(Vector3(1f, 0f, 0f), Vector3(0f, 0f, 0f), Vector3(0f, 1f, 0f), Vector3(1f, 1f, 0f)),
        ),
        South(
            0,
            0,
            1,
            Vector3.Z,
            arrayOf(Vector3(0f, 0f, 1f), Vector3(1f, 0f, 1f), Vector3(1f, 1f, 1f), Vector3(0f, 1f, 1f)),
        ),
        East(
            1,
            0,
            0,
            Vector3.X,
            arrayOf(Vector3(1f, 0f, 1f), Vector3(1f, 0f, 0f), Vector3(1f, 1f, 0f), Vector3(1f, 1f, 1f)),
        ),
        West(
            -1,
            0,
            0,
            Vector3.X.cpy().scl(-1f),
            arrayOf(Vector3(0f, 0f, 0f), Vector3(0f, 0f, 1f), Vector3(0f, 1f, 1f), Vector3(0f, 1f, 0f)),
        );

        fun addTo(partBuilder: MeshPartBuilder, x: Float, y: Float, z: Float) {
            val bottomLeft = vertexInfoAt(x, y, z, 0, 0f, 1f)
            val bottomRight = vertexInfoAt(x, y, z, 1, 1f, 1f)
            val topRight = vertexInfoAt(x, y, z, 2, 1f, 0f)
            val topLeft = vertexInfoAt(x, y, z, 3, 0f, 0f)

            val index0 = partBuilder.vertex(bottomLeft)
            val index1 = partBuilder.vertex(bottomRight)
            val index2 = partBuilder.vertex(topRight)
            val index3 = partBuilder.vertex(topLeft)

            partBuilder.index(index0, index1, index2)
            partBuilder.index(index2, index3, index0)
        }

        private fun vertexInfoAt(x: Float, y: Float, z: Float, index: Int, u: Float, v: Float): VertexInfo {
            val vertex = vertices[index]
            return VertexInfo()
                .setPos(x + vertex.x, y + vertex.y, z + vertex.z)
                .setNor(normal)
                .setUV(u, v)
        }
    }
}
