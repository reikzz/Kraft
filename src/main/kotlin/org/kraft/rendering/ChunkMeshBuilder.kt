package org.kraft.rendering

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.VertexAttributes.Usage
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder.VertexInfo
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Vector3
import org.kraft.world.BlockType
import org.kraft.world.Chunk
import org.kraft.world.VoxelWorld

/**
 * Builds 3D meshes for individual world chunks using face-culling and texture atlasing.
 * Generates geometry only for visible faces adjacent to transparent blocks (AIR).
 * Calculates vertex-level Ambient Occlusion (AO) based on neighbor block density to provide soft shadows.
 */
class ChunkMeshBuilder(
    private val materials: BlockMaterialRegistry,
    private val world: VoxelWorld,
) {
    private val vertexAttributes = (Usage.Position or Usage.Normal or Usage.TextureCoordinates or Usage.ColorUnpacked).toLong()

    /**
     * Builds a 3D model for the specified chunk.
     */
    fun build(chunk: Chunk): Model {
        val modelBuilder = ModelBuilder()
        modelBuilder.begin()

        val partBuilder = modelBuilder.part(
            "chunk",
            GL20.GL_TRIANGLES,
            vertexAttributes,
            materials.atlasMaterial,
        )

        for (x in 0 until Chunk.WIDTH) {
            for (y in 0 until Chunk.HEIGHT) {
                for (z in 0 until Chunk.DEPTH) {
                    val blockType = chunk.getBlock(x, y, z)
                    if (blockType != BlockType.AIR) {
                        addVisibleFaces(partBuilder, chunk, x, y, z, blockType)
                    }
                }
            }
        }

        return modelBuilder.end()
    }

    private fun addVisibleFaces(
        partBuilder: MeshPartBuilder,
        chunk: Chunk,
        x: Int,
        y: Int,
        z: Int,
        blockType: BlockType
    ) {
        val uvRange = materials.getUVRange(blockType)
        val uMin = uvRange[0]
        val uMax = uvRange[1]

        val globalX = chunk.chunkX * Chunk.WIDTH + x
        val globalY = y
        val globalZ = chunk.chunkZ * Chunk.DEPTH + z

        for (face in BlockFace.entries) {
            val neighborX = globalX + face.offsetX
            val neighborY = globalY + face.offsetY
            val neighborZ = globalZ + face.offsetZ

            if (world.getBlockAt(neighborX, neighborY, neighborZ) != BlockType.AIR) continue

            val worldX = globalX.toFloat()
            val worldY = globalY.toFloat()
            val worldZ = globalZ.toFloat()

            val ao0 = getAOValue(globalX, globalY, globalZ, face, 0)
            val ao1 = getAOValue(globalX, globalY, globalZ, face, 1)
            val ao2 = getAOValue(globalX, globalY, globalZ, face, 2)
            val ao3 = getAOValue(globalX, globalY, globalZ, face, 3)

            face.addTo(partBuilder, worldX, worldY, worldZ, uMin, uMax, ao0, ao1, ao2, ao3)
        }
    }

    private fun getAOValue(blockX: Int, blockY: Int, blockZ: Int, face: BlockFace, vertexIndex: Int): Float {
        val vertex = face.vertices[vertexIndex]
        val vx = vertex.x.toInt()
        val vy = vertex.y.toInt()
        val vz = vertex.z.toInt()

        val nx = face.offsetX
        val ny = face.offsetY
        val nz = face.offsetZ

        val s1x: Int
        val s1y: Int
        val s1z: Int
        val s2x: Int
        val s2y: Int
        val s2z: Int

        if (ny != 0) {
            s1x = if (vx == 0) -1 else 1
            s1y = 0
            s1z = 0
            s2x = 0
            s2y = 0
            s2z = if (vz == 0) -1 else 1
        } else if (nz != 0) {
            s1x = if (vx == 0) -1 else 1
            s1y = 0
            s1z = 0
            s2x = 0
            s2y = if (vy == 0) -1 else 1
            s2z = 0
        } else {
            s1x = 0
            s1y = if (vy == 0) -1 else 1
            s1z = 0
            s2x = 0
            s2y = 0
            s2z = if (vz == 0) -1 else 1
        }

        val side1Solid = world.getBlockAt(blockX + nx + s1x, blockY + ny + s1y, blockZ + nz + s1z).isSolid
        val side2Solid = world.getBlockAt(blockX + nx + s2x, blockY + ny + s2y, blockZ + nz + s2z).isSolid
        val cornerSolid = world.getBlockAt(blockX + nx + s1x + s2x, blockY + ny + s1y + s2y, blockZ + nz + s1z + s2z).isSolid

        if (side1Solid && side2Solid) return 0.4f
        var sum = 0
        if (side1Solid) sum++
        if (side2Solid) sum++
        if (cornerSolid) sum++
        return when (sum) {
            0 -> 1.0f
            1 -> 0.8f
            2 -> 0.6f
            else -> 0.4f
        }
    }

    private enum class BlockFace(
        val offsetX: Int,
        val offsetY: Int,
        val offsetZ: Int,
        val normal: Vector3,
        val vertices: Array<Vector3>,
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

        fun addTo(
            partBuilder: MeshPartBuilder,
            x: Float,
            y: Float,
            z: Float,
            uMin: Float,
            uMax: Float,
            ao0: Float,
            ao1: Float,
            ao2: Float,
            ao3: Float
        ) {
            val bottomLeft = vertexInfoAt(x, y, z, 0, uMin, 1f, ao0)
            val bottomRight = vertexInfoAt(x, y, z, 1, uMax, 1f, ao1)
            val topRight = vertexInfoAt(x, y, z, 2, uMax, 0f, ao2)
            val topLeft = vertexInfoAt(x, y, z, 3, uMin, 0f, ao3)

            val index0 = partBuilder.vertex(bottomLeft)
            val index1 = partBuilder.vertex(bottomRight)
            val index2 = partBuilder.vertex(topRight)
            val index3 = partBuilder.vertex(topLeft)

            partBuilder.index(index0, index1, index2)
            partBuilder.index(index2, index3, index0)
        }

        private fun vertexInfoAt(x: Float, y: Float, z: Float, index: Int, u: Float, v: Float, ao: Float): VertexInfo {
            val vertex = vertices[index]
            val color = Color(ao, ao, ao, 1f)
            return VertexInfo()
                .setPos(x + vertex.x, y + vertex.y, z + vertex.z)
                .setNor(normal)
                .setCol(color)
                .setUV(u, v)
        }
    }
}
