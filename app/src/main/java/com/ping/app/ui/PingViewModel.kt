package com.ping.app.ui

import androidx.lifecycle.ViewModel
import com.ping.app.data.repository.PingRepository
import com.ping.app.domain.model.LocationPayload

class PingViewModel(
    private val repository: PingRepository = PingRepository(),
) : ViewModel() {
    val peers = repository.peers
    val messages = repository.messages

    fun refreshPeers() = repository.refreshPeers()

    fun sendMessage(text: String) = repository.sendText(text)

    fun sendSos() = repository.sendSos()

    fun shareLocation(latitude: Double, longitude: Double) {
        repository.shareLocation(LocationPayload(latitude, longitude))
    }
}
