package com.ping.app.domain.model

data class Peer(
    val id: String,
    val displayName: String,
    val transport: String,
    val lastSeenEpochMs: Long,
    val hopDistance: Int = 1,
    val isReachable: Boolean = true,
)
