package org.kraft.rendering

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.utils.Disposable
import org.kraft.world.BlockType

class BlockMaterialRegistry : Disposable {
    private val textures = BlockType.entries
        .filter { !it.isAir }
        .associateWith { Texture("assets/${it.textureName}") }

    private val materials = textures.mapValues { (_, texture) ->
        Material(
            TextureAttribute.createDiffuse(texture),
            IntAttribute.createCullFace(GL20.GL_NONE),
        )
    }

    fun materialFor(blockType: BlockType): Material {
        if (blockType.isAir) {
            throw IllegalArgumentException("AIR blocks do not have physical materials")
        }
        return materials.getValue(blockType)
    }

    override fun dispose() {
        textures.values.forEach(Texture::dispose)
    }
}
