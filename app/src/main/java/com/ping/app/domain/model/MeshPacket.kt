package com.ping.app.domain.model

import java.util.UUID

enum class PacketType {
    TEXT,
    SOS,
    LOCATION,
    BROADCAST,
    ACK
}

enum class Priority {
    NORMAL,
    HIGH,
    EMERGENCY
}

enum class DeliveryStatus {
    QUEUED,
    FORWARDED,
    DELIVERED,
    EXPIRED,
    FAILED
}

data class LocationPayload(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float? = null,
)

data class MeshPacket(
    val id: String = UUID.randomUUID().toString(),
    val sourceId: String,
    val destinationId: String? = null,
    val type: PacketType,
    val priority: Priority = Priority.NORMAL,
    val body: String? = null,
    val location: LocationPayload? = null,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val ttl: Int = 8,
    val hops: Int = 0,
    val deliveryStatus: DeliveryStatus = DeliveryStatus.QUEUED,
) {
    fun isExpired(): Boolean = hops >= ttl

    fun nextHop(status: DeliveryStatus = DeliveryStatus.FORWARDED): MeshPacket {
        return copy(hops = hops + 1, deliveryStatus = status)
    }
}
