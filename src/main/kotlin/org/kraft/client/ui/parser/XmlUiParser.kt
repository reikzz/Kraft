package org.kraft.client.ui.parser

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Cell
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.XmlReader

class XmlUiParser(private val skin: Skin) {
    private val builders = mutableMapOf<String, (XmlReader.Element, Skin) -> Actor>().apply {
        put("Table") { el, skin -> Table().apply {
            el.getAttribute("background", null)?.let { bgName ->
                this.background = skin.getDrawable(bgName)
            }
        } }
        put("Label") { el, skin ->
            Label(el.getAttribute("text", ""), skin, el.getAttribute("style", "default"))
        }
        put("TextButton") { el, skin ->
            TextButton(el.getAttribute("text", ""), skin, el.getAttribute("style", "default"))
        }
    }

    private val alignmentMap = mapOf(
        "center" to Align.center,
        "top" to Align.top,
        "bottom" to Align.bottom,
        "left" to Align.left,
        "right" to Align.right,
        "topleft" to Align.topLeft,
        "topright" to Align.topRight,
        "bottomleft" to Align.bottomLeft,
        "bottomright" to Align.bottomRight
    )

    fun parse(fileHandle: FileHandle): XmlUiResult {
        val rootElement = XmlReader().parse(fileHandle)
        val idMap = mutableMapOf<String, Actor>()
        val rootActor = parseElement(rootElement, idMap)
            ?: throw IllegalArgumentException("Unable to parse root element in ${fileHandle.name()}")

        return XmlUiResult(rootActor, idMap)
    }

    private fun parseElement(element: XmlReader.Element, idMap: MutableMap<String, Actor>): Actor? {
        val builder = builders[element.name] ?: return null
        val actor = builder(element, skin)

        element.getAttribute("id", null)?.let { id ->
            actor.name = id
            idMap[id] = actor
        }

        actor.applyBaseAttributes(element)

        for (i in 0 until element.childCount) {
            val childElement = element.getChild(i)
            val childActor = parseElement(childElement, idMap) ?: continue

            addChild(actor, childActor, childElement)
        }

        return actor
    }

    private fun addChild(parent: Actor, child: Actor, childElement: XmlReader.Element) {
        when (parent) {
            is Table -> parent.add(child).applyCellAttributes(childElement)
            is Group -> parent.addActor(child)
        }
    }

    private fun Actor.applyBaseAttributes(element: XmlReader.Element) {
        element.getAttribute("width", null)?.toFloatOrNull()?.let { this.width = it }
        element.getAttribute("height", null)?.toFloatOrNull()?.let { this.height = it }
        element.getAttribute("x", null)?.toFloatOrNull()?.let { this.x = it }
        element.getAttribute("y", null)?.toFloatOrNull()?.let { this.y = it }

        element.getAttribute("color", null)?.let { colorStr ->
            this.color = parseColor(colorStr)
        }

        if (this is Table) {
            if (element.getSafeBooleanAttribute("fillParent")) this.setFillParent(true)

            element.getAttribute("pad", null)?.toFloatOrNull()?.let { this.pad(it) }
            element.getAttribute("align", null)?.let { this.align(parseAlign(it)) }
        }
    }

    private fun Cell<*>.applyCellAttributes(element: XmlReader.Element) {
        element.getAttribute("pad", null)?.toFloatOrNull()?.let { this.pad(it) }
        element.getAttribute("space", null)?.toFloatOrNull()?.let { this.space(it) }
        element.getAttribute("width", null)?.toFloatOrNull()?.let { this.width(it) }
        element.getAttribute("height", null)?.toFloatOrNull()?.let { this.height(it) }

        element.getAttribute("padBottom", null)?.toFloatOrNull()?.let { this.padBottom(it) }

        if (element.getSafeBooleanAttribute("fillX")) this.fillX()
        if (element.getSafeBooleanAttribute("fillY")) this.fillY()
        if (element.getSafeBooleanAttribute("expandX")) this.expandX()
        if (element.getSafeBooleanAttribute("expandY")) this.expandY()

        element.getAttribute("align", null)?.let { this.align(parseAlign(it)) }

        if (element.getSafeBooleanAttribute("row")) this.row()
    }

    private fun XmlReader.Element.getSafeBooleanAttribute(name: String): Boolean =
        this.getAttribute(name, "false").toBoolean()

    private fun parseAlign(alignStr: String): Int =
        alignmentMap[alignStr.lowercase()] ?: Align.center

    private fun parseColor(colorStr: String): Color {
        val tokens = colorStr.split(";")
        if (tokens.size == 4) {
            return Color(
                tokens[0].toFloatOrNull() ?: 1f,
                tokens[1].toFloatOrNull() ?: 1f,
                tokens[2].toFloatOrNull() ?: 1f,
                tokens[3].toFloatOrNull() ?: 1f
            )
        }
        return Color.WHITE
    }
}