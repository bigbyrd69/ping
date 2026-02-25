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
  - message flow visualisation panel
- Hacker-style dark theme (black + neon green)

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

---

## AirGap Bridge (Ultrasonic data transfer prototype)

`AirGap Bridge` is a standalone Python prototype that transmits UTF-8 text via ultrasonic BFSK audio (no network stack).

### Files

- `sender.py` — text/message framing and BFSK ultrasonic transmission.
- `receiver.py` — chunked FFT-based BFSK decode, framing detection, checksum verification.
- `utils.py` — shared protocol, checksum, optional AES-GCM, calibration, and DSP helpers.

### BFSK mapping

- Bit `0` → **18000 Hz** (or 16000 Hz in fallback mode)
- Bit `1` → **19000 Hz** (or 17000 Hz in fallback mode)
- Default bit duration: **0.1 s** (configurable)
- Default sample rate: **44100 Hz**

### Setup

```bash
python3 -m venv .venv
source .venv/bin/activate
pip install numpy scipy sounddevice matplotlib pycryptodome
```

> Linux users may need PortAudio packages first (e.g., `sudo apt install portaudio19-dev`).

### Run receiver (Laptop B)

```bash
python receiver.py
```

Optional receiver modes:

```bash
python receiver.py --calibrate           # auto-pick two dominant ultrasonic carriers
python receiver.py --fallback            # use 16k/17k carrier pair
python receiver.py --aes-key "secret"    # decrypt AES-GCM payload
python receiver.py --no-plot             # disable live matplotlib spectrum
```

### Run sender (Laptop A)

```bash
python sender.py "Hello from AirGap Bridge"
```

Optional sender modes:

```bash
python sender.py "Top secret" --aes-key "secret"
python sender.py "Legacy mode" --fallback
python sender.py "Faster symbols" --bit-duration 0.08
```

### Transmission protocol

1. Sender encodes message to UTF-8 bytes.
2. Optional AES-GCM encryption is applied (`nonce + tag + ciphertext`).
3. A 1-byte XOR checksum is appended to payload.
4. Bits are framed as: `START_MARKER + payload_bits + checksum_bits + END_MARKER`.
   - `START_MARKER = 11111110`
   - `END_MARKER   = 01111111`
5. Each bit is emitted as a fixed-duration sine tone using BFSK frequencies.

### Receiver decoding flow

1. Record audio continuously in one-bit windows.
2. FFT each chunk to estimate dominant frequency.
3. Classify bit using ±300 Hz tolerance around configured carriers.
4. Search stream for START/END markers.
5. Convert framed bits back to bytes.
6. Validate XOR checksum; if valid, decrypt (optional) and decode UTF-8 text.

### Practical tips

- Keep speaker volume moderate to avoid clipping.
- Use a quiet room and keep laptops 0.5–2 m apart.
- If decode quality is poor, try `--calibrate` or `--fallback`.
- Consumer mics/speakers vary; some may attenuate >18 kHz strongly.
