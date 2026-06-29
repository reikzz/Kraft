package org.kraft.server

import org.kraft.network.*
import org.kraft.world.BlockChangedEvent
import org.kraft.world.World
import org.kraft.world.Chunk
import org.kraft.world.ChunkCoordinate
import org.kraft.world.BlockType
import org.kraft.world.storage.DiskChunkStorage
import org.kraft.world.generator.NoiseTerrainGenerator
import org.kraft.world.subscribe
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.floor

/**
 * Dedicated server hosting the voxel world simulation and handling multi-client TCP connections.
 *
 * Block-change replication is now driven by the world's [BlockChangedEvent] instead of being
 * manually broadcast inside each [ClientHandler]. This means any future system that modifies
 * blocks (physics, redstone, explosions) will automatically replicate to all clients without
 * additional wiring.
 */
class GameServer(private val port: Int = 25565) {
    private val serverWorld = World(NoiseTerrainGenerator(), DiskChunkStorage("server"))
    private val clients = ConcurrentHashMap<Int, ClientHandler>()
    private val playerIdGenerator = AtomicInteger(1)
    private var serverSocket: ServerSocket? = null

    @Volatile
    private var running = false

    init {
        serverWorld.subscribe<BlockChangedEvent> { event ->
            broadcast(BlockChangePacket(event.x, event.y, event.z, event.newType.id))
        }
    }

    /**
     * Starts the server TCP socket and enters the connection accept loop.
     */
    fun start() {
        serverSocket = ServerSocket(port)
        running = true
        println("Kraft dedicated server started on port $port")

        while (running) {
            try {
                val clientSocket = serverSocket!!.accept()
                val playerId = playerIdGenerator.getAndIncrement()
                val handler = ClientHandler(playerId, clientSocket)
                clients[playerId] = handler
                handler.start()
            } catch (e: Exception) {
                if (running) println("Server socket error: ${e.message}")
            }
        }
    }

    /**
     * Shuts down the server, closing all active client sockets.
     */
    fun stop() {
        running = false
        try {
            serverSocket?.close()
        } catch (e: Exception) {}
        clients.values.forEach { it.close() }
        clients.clear()
        println("Server stopped.")
    }

    private fun broadcast(packet: Packet, excludePlayerId: Int = -1) {
        clients.forEach { (id, handler) ->
            if (id != excludePlayerId) {
                handler.sendPacket(packet)
            }
        }
    }

    /**
     * Manages networking and state replication for a single connected client.
     */
    inner class ClientHandler(val playerId: Int, private val socket: Socket) : Thread("ClientHandler-$playerId") {
        private val inp = DataInputStream(socket.getInputStream())
        private val out = DataOutputStream(socket.getOutputStream())
        private val sentChunks = mutableSetOf<ChunkCoordinate>()

        @Volatile
        private var connected = true

        var posX = 8f
        var posY = (Chunk.HEIGHT / 2 + 5).toFloat()
        var posZ = 8f
        var yaw = 0f

        override fun run() {
            try {
                println("Player $playerId joined from ${socket.remoteSocketAddress}")

                sendPacket(HandshakePacket(playerId, posX, posY, posZ))

                clients.forEach { (otherId, other) ->
                    if (otherId != playerId) {
                        sendPacket(SpawnPlayerPacket(otherId, other.posX, other.posY, other.posZ))
                    }
                }

                broadcast(SpawnPlayerPacket(playerId, posX, posY, posZ), excludePlayerId = playerId)

                sendChunksAround(posX, posZ, radius = 2)

                while (connected) {
                    val packet = Packet.read(inp)
                    handlePacket(packet)
                }
            } catch (e: Exception) {
                println("Player $playerId disconnected: ${e.message}")
            } finally {
                close()
            }
        }

        private fun handlePacket(packet: Packet) {
            when (packet) {
                is PlayerPositionPacket -> {
                    posX = packet.x
                    posY = packet.y
                    posZ = packet.z
                    yaw = packet.yaw

                    sendChunksAround(posX, posZ, radius = 2)

                    broadcast(PlayerPositionPacket(playerId, posX, posY, posZ, yaw), excludePlayerId = playerId)
                }
                is BlockChangePacket -> {
                    val blockType = BlockType.fromId(packet.typeId)
                    serverWorld.setBlockAt(packet.x, packet.y, packet.z, blockType)
                }
                else -> {}
            }
        }

        private fun sendChunksAround(x: Float, z: Float, radius: Int) {
            val centerChunkX = Math.floorDiv(floor(x).toInt(), Chunk.WIDTH)
            val centerChunkZ = Math.floorDiv(floor(z).toInt(), Chunk.DEPTH)

            serverWorld.loadChunksAround(x, z, radius)

            for (cx in centerChunkX - radius..centerChunkX + radius) {
                for (cz in centerChunkZ - radius..centerChunkZ + radius) {
                    val coord = ChunkCoordinate(cx, cz)
                    if (coord !in sentChunks) {
                        val chunk = serverWorld.getChunk(coord)
                        if (chunk != null) {
                            sendPacket(ChunkDataPacket(cx, cz, chunk.getBlocksData()))
                            sentChunks.add(coord)
                        }
                    }
                }
            }
        }

        /**
         * Sends a packet to this client socket.
         */
        fun sendPacket(packet: Packet) {
            synchronized(this) {
                try {
                    if (connected) {
                        packet.write(out)
                        out.flush()
                    }
                } catch (e: Exception) {
                    connected = false
                }
            }
        }

        /**
         * Disconnects the socket and notifies other players.
         */
        fun close() {
            if (connected) {
                connected = false
                try {
                    socket.close()
                } catch (e: Exception) {}
                clients.remove(playerId)
                broadcast(DestroyPlayerPacket(playerId))
                println("Player $playerId disconnected and cleaned up.")
            }
        }
    }
}
