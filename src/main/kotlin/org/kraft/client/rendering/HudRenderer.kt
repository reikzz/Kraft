package org.kraft.client.rendering

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.Disposable
import org.kraft.client.player.Player
import org.kraft.client.player.PlayerController
import org.kraft.world.BlockType

/**
 * Renders the 2D Heads-Up Display (HUD) overlay, including the hotbar slots, items, selection highlight, and crosshair.
 */
class HudRenderer(
    private val blockMaterials: BlockMaterialRegistry
) : Disposable {
    private val shapeRenderer = ShapeRenderer()
    private val spriteBatch = SpriteBatch()

    private var activeSlotGlow = 0f

    /**
     * Renders the HUD overlay on the screen.
     * Only renders when the cursor is captured to prevent UI draw calls in menu states.
     */
    fun render(
        player: Player,
        controller: PlayerController,
        deltaTime: Float
    ) {
        if (!Gdx.input.isCursorCatched) return

        val screenWidth = Gdx.graphics.width.toFloat()
        val screenHeight = Gdx.graphics.height.toFloat()

        activeSlotGlow = MathUtils.lerp(activeSlotGlow, 1.0f, 25f * deltaTime)

        shapeRenderer.projectionMatrix = shapeRenderer.projectionMatrix.setToOrtho2D(0f, 0f, screenWidth, screenHeight)
        spriteBatch.projectionMatrix = shapeRenderer.projectionMatrix

        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        val slotSize = 36f
        val spacing = 5f
        val panelWidth = 9 * slotSize + 10 * spacing
        val panelHeight = slotSize + 2 * spacing
        val panelX = (screenWidth - panelWidth) / 2f
        val panelY = 16f

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.color = Color(0.035f, 0.035f, 0.043f, 0.95f)
        drawRoundedRect(shapeRenderer, panelX, panelY, panelWidth, panelHeight, 4f)

        shapeRenderer.color = Color(0.094f, 0.094f, 0.105f, 1.0f)
        for (i in 0 until 9) {
            val slotX = panelX + spacing + i * (slotSize + spacing)
            val slotY = panelY + spacing
            drawRoundedRect(shapeRenderer, slotX, slotY, slotSize, slotSize, 3f)
        }
        shapeRenderer.end()

        spriteBatch.begin()
        for (i in 0 until 9) {
            val blockType = if (i < controller.hotbar.size) controller.hotbar[i] else BlockType.AIR
            if (blockType != BlockType.AIR) {
                val slotX = panelX + spacing + i * (slotSize + spacing)
                val slotY = panelY + spacing
                val region = blockMaterials.textureRegionFor(blockType)
                
                val padding = 4f
                spriteBatch.draw(
                    region,
                    slotX + padding,
                    slotY + padding,
                    slotSize - 2 * padding,
                    slotSize - 2 * padding
                )
            }
        }
        spriteBatch.end()

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
        
        shapeRenderer.color = Color(0.15f, 0.15f, 0.16f, 1.0f)
        drawRoundedRectOutline(shapeRenderer, panelX, panelY, panelWidth, panelHeight, 4f)

        for (i in 0 until 9) {
            val slotX = panelX + spacing + i * (slotSize + spacing)
            val slotY = panelY + spacing
            shapeRenderer.color = Color(0.15f, 0.15f, 0.16f, 0.8f)
            drawRoundedRectOutline(shapeRenderer, slotX, slotY, slotSize, slotSize, 3f)
        }

        val selX = panelX + spacing + controller.selectedSlot * (slotSize + spacing)
        val selY = panelY + spacing
        shapeRenderer.color = Color(0.98f, 0.98f, 0.98f, activeSlotGlow)
        drawRoundedRectOutline(shapeRenderer, selX, selY, slotSize, slotSize, 3f)

        shapeRenderer.color = Color(0.98f, 0.98f, 0.98f, 0.7f)
        val centerX = screenWidth / 2f
        val centerY = screenHeight / 2f
        val crosshairSize = 2f
        
        shapeRenderer.line(centerX - crosshairSize, centerY, centerX + crosshairSize, centerY)
        shapeRenderer.line(centerX, centerY - crosshairSize, centerX, centerY + crosshairSize)
        
        shapeRenderer.end()
        Gdx.gl.glDisable(GL20.GL_BLEND)
    }

    private fun drawRoundedRect(
        shapeRenderer: ShapeRenderer,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        radius: Float
    ) {
        shapeRenderer.rect(x + radius, y, width - 2 * radius, height)
        shapeRenderer.rect(x, y + radius, width, height - 2 * radius)
        shapeRenderer.arc(x + radius, y + radius, radius, 180f, 90f, 8)
        shapeRenderer.arc(x + width - radius, y + radius, radius, 270f, 90f, 8)
        shapeRenderer.arc(x + width - radius, y + height - radius, radius, 0f, 90f, 8)
        shapeRenderer.arc(x + radius, y + height - radius, radius, 90f, 90f, 8)
    }

    private fun drawRoundedRectOutline(
        shapeRenderer: ShapeRenderer,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        radius: Float
    ) {
        shapeRenderer.line(x + radius, y, x + width - radius, y)
        shapeRenderer.line(x + width, y + radius, x + width, y + height - radius)
        shapeRenderer.line(x + radius, y + height, x + width - radius, y + height)
        shapeRenderer.line(x, y + radius, x, y + height - radius)

        shapeRenderer.arc(x + radius, y + radius, radius, 180f, 90f, 8)
        shapeRenderer.arc(x + width - radius, y + radius, radius, 270f, 90f, 8)
        shapeRenderer.arc(x + width - radius, y + height - radius, radius, 0f, 90f, 8)
        shapeRenderer.arc(x + radius, y + height - radius, radius, 90f, 90f, 8)
    }

    override fun dispose() {
        shapeRenderer.dispose()
        spriteBatch.dispose()
    }
}
