#!/usr/bin/env python3
"""Generate self-authored Zone Zero chainsaw sounds and pixel chain animation.
All output is procedural; no third-party samples or Minecraft textures are used.
Requires Python 3 standard library and ffmpeg (libvorbis encoder).
"""
import math
import random
import shutil
import struct
import subprocess
import tempfile
import wave
import zlib

from generate_chainsaw_art import generate as generate_original_textures
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / "src/main/resources/assets/zonezero_chainsaw"
SOUNDS = ROOT / "sounds"
TEXTURES = ROOT / "textures/item"
SAMPLE_RATE = 22050
SOUNDS.mkdir(parents=True, exist_ok=True)
TEXTURES.mkdir(parents=True, exist_ok=True)


def synth(kind, duration, seed):
    rng = random.Random(seed)
    count = round(SAMPLE_RATE * duration)
    output = bytearray()
    phase = 0.0
    prev = 0.0
    for i in range(count):
        t = i / SAMPLE_RATE
        u = t / duration
        if kind == "start":
            hz = 37.0 + 59.0 * min(1.0, u * 1.65)
            envelope = min(1.0, u * 5.0) * (1.0 if u < .82 else max(0, (1-u)/.18))
            chuff = 1.0 + .17 * math.sin(2*math.pi*(14*t)*(1.0 + u))
        elif kind == "stop":
            hz = 95.0 - 71.0 * u**.65
            envelope = max(0, 1-u)**1.2
            chuff = 1.0 + .16 * math.sin(2*math.pi*(13*t)*(1.0-u*.6))
        elif kind == "cut":
            hz = 112.5
            envelope = min(1.0, u*12) * min(1.0, (1-u)*12)
            chuff = 1.0 + .18 * math.sin(2*math.pi*60*t)
        else:
            # Exact whole cycles per .64 sec = gap-free periodic loop.
            hz = 87.5
            envelope = min(1.0, u*30) * min(1.0, (1-u)*30)
            chuff = 1.0 + .1 * math.sin(2*math.pi*25*t)
        phase += 2.0 * math.pi * hz / SAMPLE_RATE
        harmonic = math.sin(phase) + .39*math.sin(2*phase+.21) + .21*math.sin(3*phase+.16)
        tick = (rng.random() - rng.random())
        prev = prev*.84 + tick*.16
        harsh = .21*prev + .07*tick
        if kind == "cut":
            harsh *= 3.1
        signal = (.34*harmonic + harsh)*envelope*chuff
        level = int(max(-32767, min(32767, signal*15500)))
        output.extend(struct.pack("<h", level))
    return output


def audio():
    ffmpeg = shutil.which("ffmpeg")
    if not ffmpeg:
        raise SystemExit("ffmpeg with libvorbis is required to generate original chainsaw audio")
    specs = [("start", .90, 2027), ("engine", .64, 332), ("cut", .48, 619), ("stop", 1.8, 904)]
    with tempfile.TemporaryDirectory() as tmp:
        for name, duration, seed in specs:
            wav = Path(tmp) / (name + ".wav")
            with wave.open(str(wav), "wb") as out:
                out.setnchannels(1)
                out.setsampwidth(2)
                out.setframerate(SAMPLE_RATE)
                out.writeframes(synth(name, duration, seed))
            target = SOUNDS / (name + ".ogg")
            subprocess.run([ffmpeg, "-hide_banner", "-loglevel", "error", "-nostdin",
                            "-y", "-i", str(wav), "-c:a", "libvorbis", "-q:a", "4",
                            str(target)], check=True)
            if target.stat().st_size < 1000:
                raise RuntimeError("Unexpectedly empty generated audio: " + name)


def png_chunk(name, payload):
    name = name.encode("ascii")
    from binascii import crc32
    return struct.pack(">I", len(payload)) + name + payload + struct.pack(
        ">I", crc32(name + payload) & 0xffffffff)


def chain_frame(frame):
    size = 64
    rgba = bytearray(size * size * 4)
    # 8-pixel chain links move one link across each of the four frames.
    for y in range(size):
        for x in range(size):
            offset = (x + frame * 2) % 8
            seam = offset <= 1
            hinge = offset in (3, 4) and (y % 8 in (3,4,5))
            band = y % 8 in (0, 7)
            if seam:
                color = (22, 26, 29, 255)
            elif hinge:
                color = (193, 191, 179, 255)
            elif band:
                color = (84, 89, 94, 255)
            elif y % 8 in (1, 6):
                color = (62, 69, 74, 255)
            else:
                color = (42, 48, 54, 255)
            i = (y * size + x) * 4
            rgba[i:i+4] = bytes(color)
    return bytes(rgba)


def texture():
    width = 64
    height = 256
    rgba = b"".join(chain_frame(frame) for frame in range(4))
    scanlines = b"".join(b"\x00" + rgba[y*width*4:(y+1)*width*4] for y in range(height))
    png = (b"\x89PNG\r\n\x1a\n" +
           png_chunk("IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)) +
           png_chunk("IDAT", zlib.compress(scanlines, 9)) +
           png_chunk("IEND", b""))
    for name, ticks in [("chain_running", 2), ("chain_coast", 6)]:
        target = TEXTURES / (name + ".png")
        target.write_bytes(png)
        import json
        target.with_suffix(".png.mcmeta").write_text(
            json.dumps({"animation":{"frametime":ticks,"interpolate":False}}) + "\n",
            encoding="utf-8")
        assert target.read_bytes()[:8] == b"\x89PNG\r\n\x1a\n"


if __name__ == "__main__":
    audio()
    texture()
    generate_original_textures(TEXTURES)
    print("Generated original OGG effects, animated chain textures, and 24 unique pixel-art materials.")
