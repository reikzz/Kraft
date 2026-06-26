package org.kraft.network

import java.io.DataInputStream
import java.io.DataOutputStream

/**
 * Base class for all network packets sent between Client and Server.
 */
sealed class Packet {
    abstract val id: Byte
    abstract fun write(out: DataOutputStream)

    companion object {
        /**
         * Reads the next packet from the stream.
         */
        fun read(inp: DataInputStream): Packet {
            return when (val packetId = inp.readByte()) {
                1.toByte() -> HandshakePacket.read(inp)
                2.toByte() -> SpawnPlayerPacket.read(inp)
                3.toByte() -> DestroyPlayerPacket.read(inp)
                4.toByte() -> PlayerPositionPacket.read(inp)
                5.toByte() -> BlockChangePacket.read(inp)
                6.toByte() -> ChunkDataPacket.read(inp)
                else -> throw IllegalArgumentException("Unknown packet ID: $packetId")
            }
        }
    }
}

data class HandshakePacket(val playerId: Int, val x: Float, val y: Float, val z: Float) : Packet() {
    override val id: Byte = 1
    override fun write(out: DataOutputStream) {
        out.writeByte(id.toInt())
        out.writeInt(playerId)
        out.writeFloat(x)
        out.writeFloat(y)
        out.writeFloat(z)
    }
    companion object {
        fun read(inp: DataInputStream) = HandshakePacket(inp.readInt(), inp.readFloat(), inp.readFloat(), inp.readFloat())
    }
}

data class SpawnPlayerPacket(val playerId: Int, val x: Float, val y: Float, val z: Float) : Packet() {
    override val id: Byte = 2
    override fun write(out: DataOutputStream) {
        out.writeByte(id.toInt())
        out.writeInt(playerId)
        out.writeFloat(x)
        out.writeFloat(y)
        out.writeFloat(z)
    }
    companion object {
        fun read(inp: DataInputStream) = SpawnPlayerPacket(inp.readInt(), inp.readFloat(), inp.readFloat(), inp.readFloat())
    }
}

data class DestroyPlayerPacket(val playerId: Int) : Packet() {
    override val id: Byte = 3
    override fun write(out: DataOutputStream) {
        out.writeByte(id.toInt())
        out.writeInt(playerId)
    }
    companion object {
        fun read(inp: DataInputStream) = DestroyPlayerPacket(inp.readInt())
    }
}

data class PlayerPositionPacket(val playerId: Int, val x: Float, val y: Float, val z: Float, val yaw: Float) : Packet() {
    override val id: Byte = 4
    override fun write(out: DataOutputStream) {
        out.writeByte(id.toInt())
        out.writeInt(playerId)
        out.writeFloat(x)
        out.writeFloat(y)
        out.writeFloat(z)
        out.writeFloat(yaw)
    }
    companion object {
        fun read(inp: DataInputStream) = PlayerPositionPacket(inp.readInt(), inp.readFloat(), inp.readFloat(), inp.readFloat(), inp.readFloat())
    }
}

data class BlockChangePacket(val x: Int, val y: Int, val z: Int, val typeId: Byte) : Packet() {
    override val id: Byte = 5
    override fun write(out: DataOutputStream) {
        out.writeByte(id.toInt())
        out.writeInt(x)
        out.writeInt(y)
        out.writeInt(z)
        out.writeByte(typeId.toInt())
    }
    companion object {
        fun read(inp: DataInputStream) = BlockChangePacket(inp.readInt(), inp.readInt(), inp.readInt(), inp.readByte())
    }
}

data class ChunkDataPacket(val chunkX: Int, val chunkZ: Int, val blocks: ByteArray) : Packet() {
    override val id: Byte = 6
    override fun write(out: DataOutputStream) {
        out.writeByte(id.toInt())
        out.writeInt(chunkX)
        out.writeInt(chunkZ)
        out.writeInt(blocks.size)
        out.write(blocks)
    }
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ChunkDataPacket) return false
        return chunkX == other.chunkX && chunkZ == other.chunkZ && blocks.contentEquals(other.blocks)
    }
    override fun hashCode(): Int {
        var result = chunkX
        result = 31 * result + chunkZ
        result = 31 * result + blocks.contentHashCode()
        return result
    }
    companion object {
        fun read(inp: DataInputStream): ChunkDataPacket {
            val cx = inp.readInt()
            val cz = inp.readInt()
            val size = inp.readInt()
            val bytes = ByteArray(size)
            inp.readFully(bytes)
            return ChunkDataPacket(cx, cz, bytes)
        }
    }
}
