package com.ping.app.domain.routing

import com.ping.app.domain.model.Peer

class RoutingTable {
    private val directPeers = mutableMapOf<String, Peer>()
    private val nextHopByDestination = mutableMapOf<String, String>()

    fun upsertPeer(peer: Peer) {
        directPeers[peer.id] = peer
    }

    fun removePeer(peerId: String) {
        directPeers.remove(peerId)
        nextHopByDestination.entries.removeAll { it.key == peerId || it.value == peerId }
    }

    fun registerRoute(destinationId: String, viaPeerId: String) {
        if (viaPeerId in directPeers) {
            nextHopByDestination[destinationId] = viaPeerId
        }
    }

    fun resolveNextHop(destinationId: String?): Peer? {
        if (destinationId == null) return null
        val direct = directPeers[destinationId]
        if (direct != null) return direct

        val viaId = nextHopByDestination[destinationId] ?: return null
        return directPeers[viaId]
    }

    fun peers(): List<Peer> = directPeers.values.toList()
}
