package com.ping.app.domain.transport

import com.ping.app.domain.model.MeshPacket
import com.ping.app.domain.model.Peer

interface MeshTransport {
    val name: String

    fun discoverPeers(): List<Peer>

    fun send(packet: MeshPacket, peer: Peer): Boolean
}
