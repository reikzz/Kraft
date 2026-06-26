package org.kraft.client

import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.math.MathUtils
import org.kraft.client.player.Player
import org.kraft.client.player.PlayerController
import org.kraft.client.player.RemotePlayer
import org.kraft.client.input.InputService
import org.kraft.network.*
import org.kraft.physics.VoxelPhysicsEngine
import org.kraft.world.BlockType
import org.kraft.world.Chunk
import org.kraft.world.VoxelWorld
import org.kraft.world.World
import org.kraft.world.storage.DiskChunkStorage
import org.kraft.world.generator.NoiseTerrainGenerator

/**
 * Owns the active game simulation state: world, player, physics, networking, and chunk loading.
 * @param serverHost  If non-null, connects to a remote server instead of running locally.
 */
class GameSession(
    serverHost: String?,
    inputService: InputService,
    camera: PerspectiveCamera,
) {
    val world: VoxelWorld
    val player: Player = Player()
    val playerController: PlayerController
    val physicsEngine: VoxelPhysicsEngine
    val remotePlayers = mutableMapOf<Int, RemotePlayer>()

    val networkClient: NetworkClient?
    var localPlayerId: Int = -1
        private set

    private val chunkLoadRadius = 2
    private var lastSendTime = 0f
    private val isMultiplayer: Boolean = serverHost != null

    init {
        if (serverHost != null) {
            networkClient = NetworkClient(serverHost)
            world = World(terrainGenerator = null, chunkStorage = null)
            playerController = PlayerController(player, world, camera, inputService, onBlockInteracted = { x, y, z, type ->
                networkClient.sendPacket(BlockChangePacket(x, y, z, type.id))
            })
        } else {
            networkClient = null
            world = World(NoiseTerrainGenerator(), DiskChunkStorage("singleplayer"))
            playerController = PlayerController(player, world, camera, inputService)
            // Find actual surface height at spawn column and place player just above it
            val spawnX = 8
            val spawnZ = 8
            val surfaceY = findSurfaceY(world, spawnX, spawnZ)
            player.position.set(spawnX + 0.5f, surfaceY + 1f, spawnZ + 0.5f)
        }

        physicsEngine = VoxelPhysicsEngine(world)
    }

    /**
     * Starts the network client connection if in multiplayer mode.
     * @throws Exception if the connection fails.
     */
    fun connect() {
        networkClient?.start()
    }

    /**
     * Advances the simulation by one frame. Should be called every render tick
     * when the game is **not** paused (input updates and chunk streaming).
     */
    fun update(delta: Float) {
        playerController.updateInput(delta)
        physicsEngine.update(player, player.moveInput, delta)
        playerController.updateCameraAndInteraction()

        // Replicate player position to server at ~20 Hz
        networkClient?.let { client ->
            if (localPlayerId != -1) {
                lastSendTime += delta
                if (lastSendTime >= 0.05f) {
                    val yaw = MathUtils.atan2(camera.direction.z, camera.direction.x) * MathUtils.radiansToDegrees
                    client.sendPacket(PlayerPositionPacket(localPlayerId, player.position.x, player.position.y, player.position.z, yaw))
                    lastSendTime = 0f
                }
            }
        }

        // Load/unload chunks around the player when in singleplayer
        if (!isMultiplayer) {
            world.loadChunksAround(player.x, player.z, chunkLoadRadius, maxLoadsPerFrame = 1)
        }
    }

    /**
     * Advances physics even while paused (gravity keeps applying, no movement input).
     */
    fun updatePaused(delta: Float) {
        physicsEngine.update(player, player.moveInput.setZero(), delta)
    }

    /**
     * Drains the network packet queue and applies received packets to the game state.
     * Should be called every frame regardless of pause state.
     * @return true if still connected, false if the session should end.
     */
    fun processNetworkPackets(): Boolean {
        val client = networkClient ?: return true
        if (!client.running) return false

        while (client.packetQueue.isNotEmpty()) {
            handleServerPacket(client.packetQueue.poll())
        }
        return true
    }

    private val camera: PerspectiveCamera = camera

    private fun handleServerPacket(packet: Packet) {
        when (packet) {
            is HandshakePacket -> {
                localPlayerId = packet.playerId
                player.position.set(packet.x, packet.y, packet.z)
                println("Joined server as player $localPlayerId at (${packet.x}, ${packet.y}, ${packet.z})")
            }
            is SpawnPlayerPacket -> {
                remotePlayers[packet.playerId] = RemotePlayer(packet.playerId).apply {
                    position.set(packet.x, packet.y, packet.z)
                }
                println("Player ${packet.playerId} joined.")
            }
            is DestroyPlayerPacket -> {
                remotePlayers.remove(packet.playerId)
                println("Player ${packet.playerId} left.")
            }
            is PlayerPositionPacket -> {
                remotePlayers[packet.playerId]?.let { remote ->
                    remote.position.set(packet.x, packet.y, packet.z)
                    remote.yaw = packet.yaw
                }
            }
            is BlockChangePacket -> {
                world.setBlockAt(packet.x, packet.y, packet.z, BlockType.fromId(packet.typeId))
            }
            is ChunkDataPacket -> {
                (world as? World)?.handleChunkData(packet.chunkX, packet.chunkZ, packet.blocks)
            }
        }
    }

    /**
     * Scans downward from the top of the chunk to find the highest solid block
     * at the given world X/Z, and returns the Y coordinate just above it.
     */
    private fun findSurfaceY(world: VoxelWorld, worldX: Int, worldZ: Int): Float {
        for (y in Chunk.HEIGHT - 1 downTo 0) {
            if (world.getBlockAt(worldX, y, worldZ).isSolid) {
                return y.toFloat() + 1f
            }
        }
        return (Chunk.HEIGHT / 2).toFloat()
    }

    /**
     * Saves the world to disk (singleplayer only) and closes any network connections.
     */
    fun dispose() {
        if (!isMultiplayer) {
            (world as? World)?.saveAll()
        }
        networkClient?.stop()
    }
}
