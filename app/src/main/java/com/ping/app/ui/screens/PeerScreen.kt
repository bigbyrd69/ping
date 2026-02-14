package com.ping.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ping.app.domain.model.Peer
import com.ping.app.ui.PingViewModel

@Composable
internal fun PeerScreen(viewModel: PingViewModel) {
    val peers by viewModel.peers.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(onClick = viewModel::refreshPeers, modifier = Modifier.fillMaxWidth()) {
            Text(text = "Refresh Nearby Peers")
        }

        LazyColumn(
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(peers) { peer ->
                PeerCard(peer = peer)
            }
        }
    }
}

@Composable
private fun PeerCard(peer: Peer) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = peer.displayName)
            Text(text = "id=${peer.id}")
            Text(text = "transport=${peer.transport}")
            Text(text = "hop=${peer.hopDistance}")
        }
    }
}
