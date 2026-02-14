package com.ping.app.domain.transport

import com.ping.app.domain.model.MeshPacket
import com.ping.app.domain.model.Peer

class BluetoothMeshTransport : MeshTransport {
    override val name: String = "Bluetooth"

    override fun discoverPeers(): List<Peer> {
        return emptyList()
    }

    override fun send(packet: MeshPacket, peer: Peer): Boolean {
        return true
    }
}
