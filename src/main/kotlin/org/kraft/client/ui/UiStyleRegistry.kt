package org.kraft.client.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable

object UiStyleRegistry {
    fun createBasicSkin(): Skin {
        val skin = Skin()

        val defaultFont = BitmapFont()
        skin.add("default", defaultFont)

        val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888).apply {
            setColor(Color.WHITE)
            fill()
        }
        val texture = Texture(pixmap)
        pixmap.dispose()

        val textureRegion = TextureRegion(texture)

        skin.add("white", textureRegion)

        val cardColor = Color(0.15f, 0.15f, 0.18f, 0.9f)
        val cardDrawable = TextureRegionDrawable(textureRegion).tint(cardColor)

        skin.add("card_bg", cardDrawable, com.badlogic.gdx.scenes.scene2d.utils.Drawable::class.java)

        val defaultLabelStyle = Label.LabelStyle(defaultFont, Color.WHITE)
        skin.add("default", defaultLabelStyle)
        skin.add("title", defaultLabelStyle)

        val defaultButtonStyle = TextButton.TextButtonStyle().apply {
            up = TextureRegionDrawable(textureRegion).tint(Color.DARK_GRAY)
            down = TextureRegionDrawable(textureRegion).tint(Color.LIGHT_GRAY)
            font = defaultFont
            fontColor = Color.WHITE
        }
        skin.add("default", defaultButtonStyle)

        return skin
    }
}