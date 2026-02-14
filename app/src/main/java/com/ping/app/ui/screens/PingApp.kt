package com.ping.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ping.app.domain.model.MeshPacket
import com.ping.app.domain.model.PacketType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ping.app.domain.model.MeshPacket
import com.ping.app.domain.model.Peer
import com.ping.app.ui.PingViewModel

@Composable
fun PingApp(viewModel: PingViewModel = viewModel()) {
    val tabs = listOf("SOS", "Messages", "Peers")
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title) })
                }
            }

            when (selectedTab) {
                0 -> SosScreen(viewModel)
                1 -> MessageScreen(viewModel)
                2 -> PeerScreen(viewModel)
            }
        }
    }
}

@Composable
private fun SosScreen(viewModel: PingViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Emergency Actions", fontWeight = FontWeight.Bold)
        Text("Emergency Actions")
        Button(onClick = { viewModel.sendSos() }, modifier = Modifier.fillMaxWidth()) {
            Text("Send SOS Broadcast")
        }
        Button(onClick = { viewModel.shareLocation(0.0, 0.0) }, modifier = Modifier.fillMaxWidth()) {
            Text("Share Location")
        }
    }
}

@Composable
private fun MessageScreen(viewModel: PingViewModel) {
    val messages by viewModel.messages.collectAsState()
    var draft by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                label = { Text("Message") },
                modifier = Modifier.weight(1f)
            )
            Button(onClick = {
                if (draft.isNotBlank()) {
                    viewModel.sendMessage(draft)
                    draft = ""
                }
            }) { Text("Send") }
        }

        MessageVisualization(messages = messages)
        MessageList(messages)
    }
}

@Composable
private fun MessageVisualization(messages: List<MeshPacket>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Mesh Visualisation", fontWeight = FontWeight.Bold)
            Text("Recent packet flow (TTL vs hops)", style = MaterialTheme.typography.bodySmall)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .border(1.dp, MaterialTheme.colorScheme.primary)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (messages.isEmpty()) return@Canvas
                    val recent = messages.take(8)
                    val rowHeight = size.height / recent.size
                    val maxTtl = recent.maxOf { it.ttl }.coerceAtLeast(1)

                    recent.forEachIndexed { index, packet ->
                        val y = rowHeight * index + rowHeight / 2
                        val startX = 18f
                        val endX = size.width - 18f

                        drawLine(
                            color = Color(0xFF1EBE62),
                            start = Offset(startX, y),
                            end = Offset(endX, y),
                            strokeWidth = 4f,
                            cap = StrokeCap.Round
                        )

                        val ttlRatio = packet.ttl.toFloat() / maxTtl.toFloat()
                        val hopRatio = (packet.hops.coerceAtLeast(0).toFloat() / packet.ttl.coerceAtLeast(1).toFloat())
                        val ttlX = startX + (endX - startX) * ttlRatio
                        val hopX = startX + (endX - startX) * hopRatio

                        drawCircle(
                            color = Color(0xFF00FF66),
                            radius = 8f,
                            center = Offset(ttlX, y),
                            style = Stroke(width = 3f)
                        )
                        drawCircle(
                            color = Color(0xFF00FF66),
                            radius = 7f,
                            center = Offset(hopX, y),
                        )
                    }
                }
            }

            val sosCount = messages.count { it.type == PacketType.SOS }
            val total = messages.size
            Text("Packets: $total  |  SOS: $sosCount")
        }
    }
}

@Composable
private fun PeerScreen(viewModel: PingViewModel) {
    val peers by viewModel.peers.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(onClick = viewModel::refreshPeers, modifier = Modifier.fillMaxWidth()) {
            Text("Refresh Nearby Peers")
        }
        LazyColumn(contentPadding = PaddingValues(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(peers) { peer ->
                PeerCard(peer)
            }
        }
    }
}

@Composable
private fun MessageList(messages: List<MeshPacket>) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(messages) { packet ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Type: ${packet.type}")
                    Text("Status: ${packet.deliveryStatus}")
                    Text("TTL/Hops: ${packet.ttl}/${packet.hops}")
                    Text(packet.body ?: packet.location?.toString().orEmpty())
                }
            }
        }
    }
}

@Composable
private fun PeerCard(peer: Peer) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(peer.displayName)
            Text("id=${peer.id}")
            Text("transport=${peer.transport}")
            Text("hop=${peer.hopDistance}")
        }
    }
}
