package com.ping.app.domain.service

import com.ping.app.domain.model.DeliveryStatus
import com.ping.app.domain.model.MeshPacket
import com.ping.app.domain.model.Peer
import com.ping.app.domain.routing.MeshRouter
import com.ping.app.domain.routing.RoutingTable
import com.ping.app.domain.store.PacketStore
import com.ping.app.domain.transport.MeshTransport

class MeshNodeService(
    private val nodeId: String,
    private val transports: List<MeshTransport>,
    private val packetStore: PacketStore = PacketStore(),
    private val routingTable: RoutingTable = RoutingTable(),
    private val router: MeshRouter = MeshRouter(routingTable),
) {
    fun discoverPeers(): List<Peer> {
        val peers = transports.flatMap { it.discoverPeers() }
        peers.forEach { routingTable.upsertPeer(it) }
        return routingTable.peers()
    }

    fun receive(packet: MeshPacket): MeshPacket {
        if (!packetStore.enqueue(packet)) {
            return packet.copy(deliveryStatus = DeliveryStatus.FAILED)
        }

        return if (packet.destinationId == nodeId || packet.destinationId == null) {
            packet.copy(deliveryStatus = DeliveryStatus.DELIVERED)
        } else {
            forward(packet)
        }
    }

    fun broadcast(packet: MeshPacket): MeshPacket {
        packetStore.enqueue(packet)
        return forward(packet)
    }

    private fun forward(packet: MeshPacket): MeshPacket {
        val forwardPeers = router.selectForwardPeers(packet)
        if (forwardPeers.isEmpty()) {
            return packet.copy(deliveryStatus = DeliveryStatus.FAILED)
        }

        val forwarded = router.markForwarded(packet)
        val sentAny = forwardPeers.any { peer ->
            transports.any { transport -> transport.send(forwarded, peer) }
        }

        return if (sentAny) forwarded else packet.copy(deliveryStatus = DeliveryStatus.FAILED)
    }
}
