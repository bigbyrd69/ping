package com.ping.app.domain.routing

import com.ping.app.domain.model.DeliveryStatus
import com.ping.app.domain.model.MeshPacket
import com.ping.app.domain.model.Peer

class MeshRouter(
    private val routingTable: RoutingTable,
) {
    fun selectForwardPeers(packet: MeshPacket): List<Peer> {
        if (packet.isExpired()) {
            return emptyList()
        }

        val destinationPeer = routingTable.resolveNextHop(packet.destinationId)
        if (destinationPeer != null) {
            return listOf(destinationPeer)
        }

        return routingTable.peers().filter { it.isReachable }
    }

    fun markForwarded(packet: MeshPacket): MeshPacket {
        return if (packet.isExpired()) {
            packet.copy(deliveryStatus = DeliveryStatus.EXPIRED)
        } else {
            packet.nextHop()
        }
    }
}
