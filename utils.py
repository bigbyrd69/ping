"""Utility functions and protocol helpers for AirGap Bridge.

This module centralizes framing, checksum, encryption, and signal-processing
helpers so both sender and receiver stay small and consistent.
"""

from __future__ import annotations

from dataclasses import dataclass
from typing import Optional

import numpy as np
from scipy.fft import rfft, rfftfreq


START_MARKER = "11111110"
END_MARKER = "01111111"

DEFAULT_SAMPLE_RATE = 44_100
DEFAULT_BIT_DURATION = 0.1

PRIMARY_FREQS = (18_000, 19_000)
FALLBACK_FREQS = (16_000, 17_000)


@dataclass(frozen=True)
class ModemConfig:
    sample_rate: int = DEFAULT_SAMPLE_RATE
    bit_duration: float = DEFAULT_BIT_DURATION
    freq0: int = PRIMARY_FREQS[0]
    freq1: int = PRIMARY_FREQS[1]
    tolerance_hz: int = 300
    amplitude: float = 0.6

    @property
    def bit_samples(self) -> int:
        return int(self.sample_rate * self.bit_duration)

    @property
    def bitrate(self) -> float:
        return 1.0 / self.bit_duration


def xor_checksum(data: bytes) -> int:
    """Compute a simple 1-byte XOR checksum for payload integrity checks."""
    check = 0
    for b in data:
        check ^= b
    return check


def bytes_to_bits(data: bytes) -> str:
    return "".join(f"{byte:08b}" for byte in data)


def bits_to_bytes(bits: str) -> bytes:
    if len(bits) % 8 != 0:
        raise ValueError("Bitstream length must be a multiple of 8.")
    return bytes(int(bits[i : i + 8], 2) for i in range(0, len(bits), 8))


def frame_payload(payload: bytes) -> str:
    """Frame payload as START + payload + checksum + END."""
    checksum_byte = bytes([xor_checksum(payload)])
    body_bits = bytes_to_bits(payload + checksum_byte)
    return f"{START_MARKER}{body_bits}{END_MARKER}"


def deframe_payload(frame_bits: str) -> tuple[bytes, bool]:
    """Split payload/checksum and validate checksum.

    Returns payload bytes and checksum validity flag.
    """
    raw = bits_to_bytes(frame_bits)
    if len(raw) < 1:
        raise ValueError("Received frame is empty.")
    payload, rx_checksum = raw[:-1], raw[-1]
    return payload, xor_checksum(payload) == rx_checksum


def sine_tone(frequency_hz: float, duration_s: float, sample_rate: int, amplitude: float) -> np.ndarray:
    """Generate a phase-continuous sine tone for one symbol/bit.

    The sender concatenates many of these fixed-duration chunks to create
    a BFSK waveform where each chunk frequency carries one bit.
    """
    t = np.arange(int(sample_rate * duration_s), dtype=np.float32) / sample_rate
    return (amplitude * np.sin(2 * np.pi * frequency_hz * t)).astype(np.float32)


def bits_to_bfsk_waveform(bits: str, cfg: ModemConfig) -> np.ndarray:
    chunks = [
        sine_tone(cfg.freq1 if bit == "1" else cfg.freq0, cfg.bit_duration, cfg.sample_rate, cfg.amplitude)
        for bit in bits
    ]
    if not chunks:
        return np.array([], dtype=np.float32)
    return np.concatenate(chunks).astype(np.float32)


def detect_dominant_frequency(chunk: np.ndarray, sample_rate: int) -> tuple[float, np.ndarray, np.ndarray]:
    """Run FFT and return dominant frequency and spectrum for visualization.

    Receiver uses this to classify each time chunk as bit 0/1 based on which
    configured carrier lies within tolerance of the dominant bin.
    """
    if chunk.ndim > 1:
        chunk = np.mean(chunk, axis=1)
    window = np.hanning(len(chunk))
    fft_mag = np.abs(rfft(chunk * window))
    freqs = rfftfreq(len(chunk), d=1.0 / sample_rate)
    dom_freq = freqs[int(np.argmax(fft_mag))]
    return float(dom_freq), freqs, fft_mag


def classify_frequency(freq: float, cfg: ModemConfig) -> Optional[str]:
    if abs(freq - cfg.freq0) <= cfg.tolerance_hz:
        return "0"
    if abs(freq - cfg.freq1) <= cfg.tolerance_hz:
        return "1"
    return None


def auto_calibrate(
    stream_reader,
    sample_rate: int,
    seconds: float = 2.0,
    min_freq: int = 15_000,
) -> tuple[int, int]:
    """Estimate two dominant ultrasonic carriers from ambient capture.

    `stream_reader` should be a callable(frames)->np.ndarray. We capture several
    chunks, average their spectra, and choose the two strongest peaks above
    `min_freq` as candidate BFSK frequencies.
    """
    frames = int(sample_rate * seconds)
    audio = stream_reader(frames)
    if audio.ndim > 1:
        audio = np.mean(audio, axis=1)
    window = np.hanning(len(audio))
    spectrum = np.abs(rfft(audio * window))
    freqs = rfftfreq(len(audio), d=1.0 / sample_rate)
    mask = freqs >= min_freq
    if np.count_nonzero(mask) < 2:
        raise RuntimeError("Not enough frequency bins for calibration.")
    masked_spectrum = spectrum[mask]
    masked_freqs = freqs[mask]
    top_idx = np.argpartition(masked_spectrum, -2)[-2:]
    candidates = sorted(int(round(masked_freqs[i])) for i in top_idx)
    if candidates[0] == candidates[1]:
        raise RuntimeError("Calibration failed to identify two distinct frequencies.")
    return candidates[0], candidates[1]


def maybe_encrypt_aes(plaintext: bytes, key_text: Optional[str]) -> bytes:
    """Optional AES-GCM encryption.

    Output layout: nonce(12) + tag(16) + ciphertext.
    """
    if not key_text:
        return plaintext
    try:
        from Crypto.Cipher import AES
        from Crypto.Hash import SHA256
    except ImportError as exc:  # pragma: no cover - dependency/runtime guard
        raise RuntimeError("AES requested but pycryptodome is not installed.") from exc

    key = SHA256.new(key_text.encode("utf-8")).digest()
    cipher = AES.new(key, AES.MODE_GCM)
    ciphertext, tag = cipher.encrypt_and_digest(plaintext)
    return cipher.nonce + tag + ciphertext


def maybe_decrypt_aes(payload: bytes, key_text: Optional[str]) -> bytes:
    if not key_text:
        return payload
    try:
        from Crypto.Cipher import AES
        from Crypto.Hash import SHA256
    except ImportError as exc:  # pragma: no cover
        raise RuntimeError("AES key provided, but pycryptodome is not installed.") from exc

    if len(payload) < 28:
        raise ValueError("Encrypted payload too short.")
    nonce, tag, ciphertext = payload[:12], payload[12:28], payload[28:]
    key = SHA256.new(key_text.encode("utf-8")).digest()
    cipher = AES.new(key, AES.MODE_GCM, nonce=nonce)
    return cipher.decrypt_and_verify(ciphertext, tag)
