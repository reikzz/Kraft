package org.kraft.client

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.g3d.utils.FirstPersonCameraController
import com.badlogic.gdx.math.Vector3

class FreeLookCameraController(camera: Camera) : FirstPersonCameraController(camera) {

    override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
        if (Gdx.input.isCursorCatched) {
            val deltaX = -Gdx.input.deltaX * degreesPerPixel
            val deltaY = -Gdx.input.deltaY * degreesPerPixel

            camera.direction.rotate(camera.up, deltaX)
            tmp.set(camera.direction).crs(camera.up).nor()
            camera.direction.rotate(tmp, deltaY)
            return true
        }
        return false
    }
}
