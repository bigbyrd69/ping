#!/usr/bin/env python3
"""AirGap Bridge sender: text -> framed bits -> ultrasonic BFSK audio."""

from __future__ import annotations

import argparse
import sys
import time

import sounddevice as sd

from utils import (
    FALLBACK_FREQS,
    ModemConfig,
    PRIMARY_FREQS,
    bits_to_bfsk_waveform,
    frame_payload,
    maybe_encrypt_aes,
)


def build_parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(description="AirGap Bridge BFSK sender")
    p.add_argument("message", nargs="?", help="Message text (UTF-8). If omitted, read stdin.")
    p.add_argument("--bit-duration", type=float, default=0.1, help="Bit duration in seconds (default 0.1).")
    p.add_argument("--sample-rate", type=int, default=44_100, help="Audio sample rate (default 44100).")
    p.add_argument("--volume", type=float, default=0.6, help="Wave amplitude 0.0-1.0 (default 0.6).")
    p.add_argument("--aes-key", help="Optional passphrase for AES-GCM encryption.")
    p.add_argument("--fallback", action="store_true", help="Use fallback frequencies 16k/17k.")
    return p


def get_message(args: argparse.Namespace) -> str:
    if args.message is not None:
        return args.message
    data = sys.stdin.read().strip()
    if not data:
        raise ValueError("No message provided. Pass text argument or pipe stdin.")
    return data


def main() -> int:
    args = build_parser().parse_args()
    message = get_message(args)
    freq0, freq1 = FALLBACK_FREQS if args.fallback else PRIMARY_FREQS

    cfg = ModemConfig(
        sample_rate=args.sample_rate,
        bit_duration=args.bit_duration,
        freq0=freq0,
        freq1=freq1,
        amplitude=args.volume,
    )

    message_bytes = message.encode("utf-8")
    tx_payload = maybe_encrypt_aes(message_bytes, args.aes_key)

    # Framing protocol: add START marker + payload bits + checksum byte + END marker.
    frame_bits = frame_payload(tx_payload)
    waveform = bits_to_bfsk_waveform(frame_bits, cfg)

    total_bits = len(frame_bits)
    tx_seconds = total_bits * cfg.bit_duration
    print("=== AirGap Bridge Sender ===")
    print(f"Message bytes (UTF-8): {len(message_bytes)}")
    print(f"Transmitted payload bytes: {len(tx_payload)}")
    print(f"Frequencies: 0->{cfg.freq0} Hz, 1->{cfg.freq1} Hz")
    print(f"Bit duration: {cfg.bit_duration:.3f}s | Bitrate: {cfg.bitrate:.1f} bps")
    print(f"Total bits (with framing+checksum): {total_bits}")
    print(f"Estimated TX time: {tx_seconds:.2f}s")

    sd.play(waveform, samplerate=cfg.sample_rate, blocking=False)

    # User-facing progress estimate while hardware plays audio.
    start = time.time()
    while sd.get_stream().active:
        elapsed = time.time() - start
        progress = min(100.0, (elapsed / tx_seconds) * 100.0) if tx_seconds else 100.0
        print(f"\rTransmitting... {progress:5.1f}%", end="", flush=True)
        time.sleep(0.1)

    sd.wait()
    print("\nTransmission complete.")
    print(f"Raw bitstream: {frame_bits}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
