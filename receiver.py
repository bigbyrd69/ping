#!/usr/bin/env python3
"""AirGap Bridge receiver: ultrasonic BFSK audio -> framed payload -> text."""

from __future__ import annotations

import argparse
import collections
from typing import Optional

import matplotlib.pyplot as plt
import numpy as np
import sounddevice as sd

from utils import (
    END_MARKER,
    FALLBACK_FREQS,
    ModemConfig,
    PRIMARY_FREQS,
    START_MARKER,
    auto_calibrate,
    classify_frequency,
    deframe_payload,
    detect_dominant_frequency,
    maybe_decrypt_aes,
)


class LiveSpectrum:
    """Minimal live plot for FFT spectrum and received bitstream text."""

    def __init__(self, enabled: bool, max_bits: int = 120):
        self.enabled = enabled
        self.max_bits = max_bits
        self.tail_bits = collections.deque(maxlen=max_bits)
        if not enabled:
            return
        plt.ion()
        self.fig, (self.ax_spec, self.ax_bits) = plt.subplots(2, 1, figsize=(10, 6))
        self.line, = self.ax_spec.plot([], [], lw=1)
        self.ax_spec.set_title("Live Frequency Spectrum")
        self.ax_spec.set_xlabel("Frequency (Hz)")
        self.ax_spec.set_ylabel("Magnitude")

        self.text_artist = self.ax_bits.text(0.01, 0.5, "", fontsize=12, family="monospace")
        self.ax_bits.set_title("Scrolling bitstream")
        self.ax_bits.axis("off")

    def update(self, freqs: np.ndarray, mag: np.ndarray, bit: Optional[str]):
        if bit in {"0", "1"}:
            self.tail_bits.append(bit)
        if not self.enabled:
            return
        self.line.set_data(freqs, mag)
        self.ax_spec.set_xlim(0, 22_050)
        self.ax_spec.set_ylim(0, max(float(np.max(mag)) * 1.1, 1.0))
        self.text_artist.set_text("".join(self.tail_bits))
        self.fig.canvas.draw_idle()
        self.fig.canvas.flush_events()
        plt.pause(0.001)


def build_parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(description="AirGap Bridge BFSK receiver")
    p.add_argument("--bit-duration", type=float, default=0.1, help="Bit duration in seconds (default 0.1).")
    p.add_argument("--sample-rate", type=int, default=44_100, help="Audio sample rate (default 44100).")
    p.add_argument("--fallback", action="store_true", help="Use fallback frequencies 16k/17k.")
    p.add_argument("--aes-key", help="Optional passphrase for AES-GCM decryption.")
    p.add_argument("--calibrate", action="store_true", help="Auto-calibrate two dominant ultrasonic carriers.")
    p.add_argument("--no-plot", action="store_true", help="Disable live matplotlib visualization.")
    return p


def extract_frame_bits(bit_buffer: str) -> tuple[Optional[str], str]:
    """Framing protocol detector with START/END marker extraction."""
    start_idx = bit_buffer.find(START_MARKER)
    if start_idx < 0:
        # Keep short suffix in case marker spans chunk boundaries.
        keep = max(0, len(bit_buffer) - len(START_MARKER))
        return None, bit_buffer[keep:]

    after_start = start_idx + len(START_MARKER)
    end_idx = bit_buffer.find(END_MARKER, after_start)
    if end_idx < 0:
        return None, bit_buffer[start_idx:]

    frame_payload_bits = bit_buffer[after_start:end_idx]
    remainder = bit_buffer[end_idx + len(END_MARKER) :]
    return frame_payload_bits, remainder


def main() -> int:
    args = build_parser().parse_args()
    freq0, freq1 = FALLBACK_FREQS if args.fallback else PRIMARY_FREQS
    cfg = ModemConfig(
        sample_rate=args.sample_rate,
        bit_duration=args.bit_duration,
        freq0=freq0,
        freq1=freq1,
    )

    print("=== AirGap Bridge Receiver ===")
    print(f"Configured frequencies: 0->{cfg.freq0} Hz, 1->{cfg.freq1} Hz")
    print(f"Tolerance: ±{cfg.tolerance_hz} Hz | Chunk: {cfg.bit_samples} samples")

    stream = sd.InputStream(samplerate=cfg.sample_rate, channels=1, dtype="float32", blocksize=cfg.bit_samples)
    stream.start()

    try:
        if args.calibrate:
            print("Calibrating frequencies from live audio...")

            def _reader(frames: int):
                data, overflowed = stream.read(frames)
                if overflowed:
                    print("[warn] overflow during calibration")
                return data[:, 0]

            c0, c1 = auto_calibrate(_reader, cfg.sample_rate)
            cfg = ModemConfig(
                sample_rate=cfg.sample_rate,
                bit_duration=cfg.bit_duration,
                freq0=c0,
                freq1=c1,
                tolerance_hz=cfg.tolerance_hz,
                amplitude=cfg.amplitude,
            )
            print(f"Calibration result: 0->{cfg.freq0} Hz, 1->{cfg.freq1} Hz")

        viz = LiveSpectrum(enabled=not args.no_plot)
        bit_buffer = ""
        print("Listening... (Ctrl+C to stop)")

        while True:
            chunk, overflowed = stream.read(cfg.bit_samples)
            if overflowed:
                print("[warn] audio overflow detected")

            chunk = chunk[:, 0]
            dom_freq, freqs, mag = detect_dominant_frequency(chunk, cfg.sample_rate)
            bit = classify_frequency(dom_freq, cfg)

            # Error handling: ignore noisy chunks that don't match either carrier.
            if bit is not None:
                bit_buffer += bit

            viz.update(freqs, mag, bit)
            print(
                f"\rDominant: {dom_freq:8.1f} Hz | bit={bit if bit else '-'} | buffered={len(bit_buffer):5d}",
                end="",
                flush=True,
            )

            frame_bits, bit_buffer = extract_frame_bits(bit_buffer)
            if frame_bits is None:
                continue

            print("\nFrame detected, decoding...")
            if len(frame_bits) % 8 != 0:
                print("[error] Corrupt frame length (not byte-aligned). Discarding frame.")
                continue

            payload, ok = deframe_payload(frame_bits)
            if not ok:
                print("[error] Checksum mismatch. Frame discarded.")
                continue

            try:
                decoded = maybe_decrypt_aes(payload, args.aes_key).decode("utf-8")
            except Exception as exc:
                print(f"[error] Decryption/UTF-8 decode failed: {exc}")
                continue

            print("[ok] Checksum valid.")
            print(f"[message] {decoded}")

    except KeyboardInterrupt:
        print("\nStopped by user.")
    finally:
        stream.stop()
        stream.close()

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
