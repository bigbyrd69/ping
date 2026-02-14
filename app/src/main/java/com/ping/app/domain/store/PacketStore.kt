package com.ping.app.domain.store

import com.ping.app.domain.model.MeshPacket

class PacketStore {
    private val seenIds = LinkedHashSet<String>()
    private val pending = ArrayDeque<MeshPacket>()

    fun enqueue(packet: MeshPacket): Boolean {
        if (packet.id in seenIds || packet.isExpired()) {
            return false
        }

        seenIds += packet.id
        pending += packet
        return true
    }

    fun nextPending(): MeshPacket? = pending.removeFirstOrNull()

    fun hasSeen(packetId: String): Boolean = packetId in seenIds

    fun requeue(packet: MeshPacket) {
        if (!packet.isExpired()) {
            pending += packet
        }
    }

    fun snapshotQueue(): List<MeshPacket> = pending.toList()
}
