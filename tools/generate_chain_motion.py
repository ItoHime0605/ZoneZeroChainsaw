#!/usr/bin/env python3
"""Original 8-frame chainsaw chain animation.

Creates moving forged links in opposite directions along the TOP and BOTTOM
runners and a separate rotating nose sprocket. The animated PNG atlases use
vanilla Minecraft 1.21.1 .png.mcmeta and are pure Python, no third-party art.

Minecraft texture animation is UV-based: the individual 3D tooth cuboids are
stationary. The moving metal links and animated sprocket create the illusion
of continuous motion; phase changes are time-driven by the item property.
"""
from pathlib import Path
import json
import math
import struct
import zlib

SIZE = 64
FRAMES = 8
SPEEDS = {
    "running": 1,
    "coast_fast": 2,
    "coast_medium": 4,
    "coast_slow": 8,
}

PALETTE = {
    "dark": (24, 28, 34, 255),
    "base": (47, 55, 62, 255),
    "steel": (108, 117, 121, 255),
    "light": (191, 195, 185, 255),
    "silver": (231, 224, 206, 255),
}

def pixel(rgba, x, y, value):
    if 0 <= x < SIZE and 0 <= y < SIZE:
        i = (y * SIZE + x) * 4
        rgba[i:i+4] = bytes(value)

def rect(rgba, x, y, w, h, value):
    for yy in range(y, y+h):
        for xx in range(x, x+w):
            pixel(rgba, xx, yy, value)

def straight(frame, reverse):
    rgba = bytearray(bytes(PALETTE["base"]) * SIZE * SIZE)
    for y in range(0, SIZE, 8):
        rect(rgba, 0, y, SIZE, 2, PALETTE["dark"])
        rect(rgba, 0, y+2, SIZE, 1, PALETTE["steel"])
        rect(rgba, 0, y+6, SIZE, 1, PALETTE["light"])
    displacement = (-frame * 2 if reverse else frame * 2) % 16
    for x in range(-32 + displacement, SIZE + 24, 16):
        rect(rgba, x, 0, 3, SIZE, PALETTE["dark"])
        rect(rgba, x+3, 0, 2, SIZE, PALETTE["steel"])
        rect(rgba, x+7, 0, 1, SIZE, PALETTE["light"])
        for y in range(5, SIZE, 16):
            rect(rgba, x+7, y, 5, 5, PALETTE["dark"])
            rect(rgba, x+8, y+1, 3, 3, PALETTE["light"])
            rect(rgba, x+9, y+2, 1, 1, PALETTE["silver"])
    return bytes(rgba)

def sprocket(frame):
    rgba = bytearray(bytes(PALETTE["base"]) * SIZE * SIZE)
    # The nose gear turns clockwise, matching chain travel.
    angle = frame * math.tau / FRAMES
    cx, cy = 32, 32
    for y in range(SIZE):
        for x in range(SIZE):
            dx, dy = x - cx, y - cy
            r = math.hypot(dx, dy)
            a = (math.atan2(dy, dx) - angle) % math.tau
            tooth = int(a * 12 / math.tau)
            ring = r > 17 and r < 25
            if r > 25:
                col = PALETTE["base"] if tooth % 2 == 0 else PALETTE["steel"]
            elif ring:
                col = PALETTE["light"] if tooth % 2 == 0 else PALETTE["dark"]
            elif r > 8:
                col = PALETTE["steel"] if tooth % 2 == 0 else PALETTE["base"]
            elif r > 4:
                col = PALETTE["silver"]
            else:
                col = PALETTE["dark"]
            pixel(rgba, x, y, col)
    return bytes(rgba)

def chunk(kind, data):
    return (struct.pack(">I", len(data)) + kind + data +
            struct.pack(">I", zlib.crc32(kind + data) & 0xffffffff))

def atlas(frames):
    assert len(frames) == FRAMES
    raw = bytearray()
    for frame in frames:
        for y in range(SIZE):
            raw.append(0)
            raw.extend(frame[y*SIZE*4:(y+1)*SIZE*4])
    return (b"\x89PNG\r\n\x1a\n" +
            chunk(b"IHDR", struct.pack(">IIBBBBB",
                 SIZE, SIZE * FRAMES, 8, 6, 0, 0, 0)) +
            chunk(b"IDAT", zlib.compress(raw, 9)) +
            chunk(b"IEND", b""))

def generate(directory):
    directory = Path(directory)
    directory.mkdir(parents=True, exist_ok=True)
    layers = {
        "chain": [straight(i, False) for i in range(FRAMES)],
        "chain_reverse": [straight(i, True) for i in range(FRAMES)],
        "chain_nose": [sprocket(i) for i in range(FRAMES)],
    }
    for speed, frametime in SPEEDS.items():
        for layer, frames in layers.items():
            texture_name = layer + "_" + speed
            png = directory / (texture_name + ".png")
            png.write_bytes(atlas(frames))
            png.with_suffix(".png.mcmeta").write_text(
                json.dumps({"animation": {"frametime": frametime,
                                           "interpolate": False}}) + "\n",
                encoding="utf-8")
    return len(SPEEDS) * len(layers)

if __name__ == "__main__":
    folder = Path(__file__).resolve().parents[1] / (
        "src/main/resources/assets/zonezero_chainsaw/textures/item")
    print("Original chainsaw motion textures generated:", generate(folder))
