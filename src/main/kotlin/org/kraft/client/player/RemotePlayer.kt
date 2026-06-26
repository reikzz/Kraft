package org.kraft.client.player

import com.badlogic.gdx.math.Vector3

/**
 * Represents the position and orientation of a remote player connected to the same server.
 */
class RemotePlayer(val id: Int) {
    val position = Vector3()
    var yaw = 0f
}
