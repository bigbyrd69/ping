package com.ping.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ping.app.ui.PingViewModel

@Composable
internal fun SosScreen(viewModel: PingViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "Emergency Actions", fontWeight = FontWeight.Bold)

        Button(
            onClick = { viewModel.sendSos() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Send SOS Broadcast")
        }

        Button(
            onClick = { viewModel.shareLocation(latitude = 0.0, longitude = 0.0) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Share Location")
        }
    }
}
