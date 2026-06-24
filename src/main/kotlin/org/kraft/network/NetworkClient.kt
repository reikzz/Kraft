package org.kraft.network

import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Handles TCP client connection to the dedicated game server.
 * Reads incoming packets asynchronously on a background thread and queues them for main-thread execution.
 */
class NetworkClient(private val host: String, private val port: Int = 25565) {
    private var socket: Socket? = null
    private var out: DataOutputStream? = null
    private var inp: DataInputStream? = null
    
    @Volatile
    var running = false

    /**
     * Thread-safe queue containing received packets to be processed on the main game loop thread.
     */
    val packetQueue = ConcurrentLinkedQueue<Packet>()

    /**
     * Starts the client connection and spawns the receiving listener thread.
     */
    fun start() {
        socket = Socket(host, port)
        out = DataOutputStream(socket!!.getOutputStream())
        inp = DataInputStream(socket!!.getInputStream())
        running = true

        Thread({
            try {
                while (running) {
                    val packet = Packet.read(inp!!)
                    packetQueue.add(packet)
                }
            } catch (e: Exception) {
                println("Connection closed: ${e.message}")
                running = false
            } finally {
                stop()
            }
        }, "NetworkClient-Receiver").start()
    }

    /**
     * Sends a packet to the server. Safe to call from any thread.
     */
    fun sendPacket(packet: Packet) {
        synchronized(this) {
            try {
                if (running) {
                    packet.write(out!!)
                    out!!.flush()
                }
            } catch (e: Exception) {
                println("Failed to send packet: ${e.message}")
                running = false
            }
        }
    }

    /**
     * Closes socket connections and stops the receiving thread.
     */
    fun stop() {
        running = false
        try {
            socket?.close()
        } catch (e: Exception) {
            // Ignore
        }
    }
}
