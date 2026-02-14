package com.ping.app.data.repository

import com.ping.app.domain.model.DeliveryStatus
import com.ping.app.domain.model.LocationPayload
import com.ping.app.domain.model.MeshPacket
import com.ping.app.domain.model.PacketType
import com.ping.app.domain.model.Peer
import com.ping.app.domain.model.Priority
import com.ping.app.domain.service.MeshNodeService
import com.ping.app.domain.transport.BluetoothMeshTransport
import com.ping.app.domain.transport.NearbyConnectionsTransport
import com.ping.app.domain.transport.WifiDirectTransport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PingRepository(
    private val nodeId: String = "local-node",
) {
    private val meshService = MeshNodeService(
        nodeId = nodeId,
        transports = listOf(
            WifiDirectTransport(),
            BluetoothMeshTransport(),
            NearbyConnectionsTransport(),
        )
    )

    private val _peers = MutableStateFlow<List<Peer>>(emptyList())
    val peers: StateFlow<List<Peer>> = _peers.asStateFlow()

    private val _messages = MutableStateFlow<List<MeshPacket>>(emptyList())
    val messages: StateFlow<List<MeshPacket>> = _messages.asStateFlow()

    fun refreshPeers() {
        _peers.value = meshService.discoverPeers()
    }

    fun sendText(text: String, destinationId: String? = null) {
        val packet = MeshPacket(
            sourceId = nodeId,
            destinationId = destinationId,
            type = if (destinationId == null) PacketType.BROADCAST else PacketType.TEXT,
            body = text,
        )
        emit(meshService.broadcast(packet))
    }

    fun sendSos(location: LocationPayload? = null) {
        val packet = MeshPacket(
            sourceId = nodeId,
            type = PacketType.SOS,
            priority = Priority.EMERGENCY,
            body = "SOS",
            location = location,
            ttl = 16,
        )
        emit(meshService.broadcast(packet))
    }

    fun shareLocation(locationPayload: LocationPayload) {
        val packet = MeshPacket(
            sourceId = nodeId,
            type = PacketType.LOCATION,
            priority = Priority.HIGH,
            location = locationPayload,
        )
        emit(meshService.broadcast(packet))
    }

    private fun emit(packet: MeshPacket) {
        val normalized = if (packet.deliveryStatus == DeliveryStatus.QUEUED) {
            packet.copy(deliveryStatus = DeliveryStatus.FORWARDED)
        } else {
            packet
        }
        _messages.value = listOf(normalized) + _messages.value
    }
}
