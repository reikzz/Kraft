package org.kraft.client.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import org.kraft.client.ui.parser.XmlUiParser
import org.kraft.client.ui.parser.XmlUiResult

/**
 * Responsible for loading, inflating, and managing Scene2D user interfaces
 * defined in XML layout files.
 *
 * @property skin The [Skin] instance used to style the inflated UI components.
 */
class UiBuilder(val skin: Skin) {

    private val parser = XmlUiParser(skin)

    /**
     * The active [Stage] where views will be injected and displayed.
     * Must be set before calling [changeView].
     */
    var targetStage: Stage? = null

    private var currentView: Actor? = null

    /**
     * Inflates an XML layout file into an [Actor] hierarchy without attaching it to any stage.
     *
     * @param layoutPath Internal path to the XML layout file.
     * @param binder A lambda block to bind logic or look up views inside the inflated result.
     * @return The root [Actor] of the inflated layout.
     */
    fun loadView(layoutPath: String, binder: XmlUiResult.() -> Unit): Actor? {
        val fileHandle = Gdx.files.internal(layoutPath)

        if (!fileHandle.exists()) {
            Gdx.app.error("UiBuilder", "Layout file not found: $layoutPath")
            return null
        }

        return parser.parse(fileHandle).apply {
            binder()
        }.root
    }

    /**
     * Replaces the currently active view on the [targetStage] with a new one.
     * This operation is safely scheduled on the LibGDX rendering thread via [Gdx.app.postRunnable].
     *
     * @param layoutPath Internal path to the new XML layout file.
     * @param binder A lambda block to bind logic to the newly inflated views.
     * @throws IllegalStateException If [targetStage] has not been initialized.
     */
    fun changeView(layoutPath: String, binder: XmlUiResult.() -> Unit) {
        val stage = requireNotNull(targetStage) {
            "Before calling changeView() you need to initialize targetStage"
        }

        Gdx.app.postRunnable {
            val newView = loadView("assets/ui/$layoutPath", binder)

            if (newView != null) {
                currentView?.remove()
                currentView = newView
                stage.addActor(newView)
            }
        }
    }

    /**
     * Traverses the current [targetStage] hierarchy to find actors with non-null names
     * and triggers the [binder] block to re-bind or update their state.
     *
     * If no [targetStage] is set, this method returns silently.
     *
     * @param binder A lambda block executed with the re-mapped actor IDs.
     */
    fun updateView(binder: XmlUiResult.() -> Unit) {
        val stage = targetStage ?: return

        val idMap = mutableMapOf<String, Actor>()

        fun collectIds(actor: Actor) {
            actor.name?.let { idMap[it] = actor }
            if (actor is Group) {
                val children = actor.children
                for (i in 0 until children.size) {
                    collectIds(children[i])
                }
            }
        }

        collectIds(stage.root)

        XmlUiResult(stage.root, idMap).binder()
    }

    fun closeCurrentView() {
        val stage = targetStage ?: return

        Gdx.app.postRunnable {
            stage.clear()
            currentView = null
        }
    }
}