package com.ping.app.domain.transport

import com.ping.app.domain.model.MeshPacket
import com.ping.app.domain.model.Peer

class NearbyConnectionsTransport : MeshTransport {
    override val name: String = "Nearby Connections"

    override fun discoverPeers(): List<Peer> {
        return emptyList()
    }

    override fun send(packet: MeshPacket, peer: Peer): Boolean {
        return true
    }
}
