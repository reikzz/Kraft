package org.kraft.client.rendering

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.utils.Disposable
import org.kraft.world.BlockType

/**
 * Manages game textures by packaging individual block textures into a single texture atlas strip.
 * Prevents multiple texture bindings during chunk rendering, reducing GPU draw calls.
 */
class BlockMaterialRegistry : Disposable {
    private val solidBlockTypes = BlockType.entries.filter { !it.isAir }
    val atlasTexture: Texture
    val atlasMaterial: Material
    private val textureRegions: Map<BlockType, TextureRegion>

    init {
        val pixmaps = solidBlockTypes.associateWith { Pixmap(Gdx.files.internal("assets/${it.textureName}")) }
        val firstPixmap = pixmaps.values.first()
        val texWidth = firstPixmap.width
        val texHeight = firstPixmap.height

        val atlasPixmap = Pixmap(texWidth * solidBlockTypes.size, texHeight, firstPixmap.format)
        solidBlockTypes.forEachIndexed { index, blockType ->
            val pm = pixmaps.getValue(blockType)
            atlasPixmap.drawPixmap(pm, index * texWidth, 0)
            pm.dispose()
        }

        atlasTexture = Texture(atlasPixmap)
        atlasTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        atlasPixmap.dispose()

        atlasMaterial = Material(
            TextureAttribute.createDiffuse(atlasTexture),
            IntAttribute.createCullFace(GL20.GL_NONE)
        )

        textureRegions = solidBlockTypes.associateWith { blockType ->
            val index = solidBlockTypes.indexOf(blockType)
            TextureRegion(atlasTexture, index * texWidth, 0, texWidth, texHeight)
        }
    }

    /**
     * Retrieves the 3D material shared by all blocks.
     * @throws IllegalArgumentException if the block type is AIR.
     */
    fun materialFor(blockType: BlockType): Material {
        require(!blockType.isAir) { "AIR blocks do not have physical materials" }
        return atlasMaterial
    }

    /**
     * Retrieves the 2D texture region mapping to the specified block type.
     * @throws IllegalArgumentException if the block type is AIR.
     */
    fun textureRegionFor(blockType: BlockType): TextureRegion {
        require(!blockType.isAir) { "AIR blocks do not have texture regions" }
        return textureRegions.getValue(blockType)
    }

    /**
     * Calculates the U-axis coordinate mapping range for a block type within the texture atlas.
     * @return a FloatArray containing [uMin, uMax].
     */
    fun getUVRange(blockType: BlockType): FloatArray {
        val index = solidBlockTypes.indexOf(blockType)
        if (index == -1) return floatArrayOf(0f, 1f)
        val uMin = index.toFloat() / solidBlockTypes.size
        val uMax = (index + 1).toFloat() / solidBlockTypes.size
        return floatArrayOf(uMin, uMax)
    }

    override fun dispose() {
        atlasTexture.dispose()
    }
}
