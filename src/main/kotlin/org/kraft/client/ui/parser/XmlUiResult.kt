package org.kraft.client.ui.parser

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener

class XmlUiResult(val root: Actor, private val idMap: Map<String, Actor>) {
    @Suppress("UNCHECKED_CAST")
    fun <T : Actor> find(id: String): T? = idMap[id] as? T

    fun onClick(id: String, action: (event: InputEvent) -> Unit) {
        find<Actor>(id)?.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent, x: Float, y: Float) {
                action(event)
            }
        })
    }
}