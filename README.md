# Ping (Android Offline Emergency Mesh)

Ping is an Android-first emergency communication app designed for disaster conditions where the internet is unavailable.

## MVP scope implemented in this scaffold

- Multi-transport abstraction for:
  - Wi-Fi Direct
  - Bluetooth
  - Nearby Connections (optional adapter)
- Mesh packet model with:
  - TTL
  - hop count
  - dedup-ready unique message IDs
  - delivery status tracking
- Store-and-forward pipeline in a `MeshNodeService`
- Basic routing table abstraction for multi-hop forwarding
- UI screens for:
  - SOS broadcast
  - location share trigger
  - text/broadcast messaging
  - peer list
  - delivery status visibility

## Architecture (high-level)

- `domain/model`: packet and peer models
- `domain/store`: deduplication + pending queue
- `domain/routing`: routing table and forward selection
- `domain/transport`: transport adapters (stubs now)
- `domain/service`: node-level mesh orchestration
- `data/repository`: app-facing API and state flows
- `ui/screens`: Compose UI for SOS/messages/peers

## Next steps to productionize

1. Replace transport stubs with real Wi-Fi Direct and Bluetooth implementations.
2. Persist packet and peer state in a local DB (Room) for crash-safe forwarding.
3. Add cryptographic signing and optional encryption for packet authenticity/privacy.
4. Add ACK/retry behavior and route-quality scoring.
5. Integrate GPS provider and permission flows.
6. Add foreground service for resilient background operation in emergency mode.
