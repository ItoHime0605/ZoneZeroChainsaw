#!/usr/bin/env python3
"""Reproducibly generate original sounds, saw motion, item art and HUD."""
import struct
import zlib
from pathlib import Path

from generate_chainsaw_sounds import create as generate_original_audio
from generate_chainsaw_art import generate as generate_original_textures
from generate_chainsaw_hud import generate as generate_original_hud
from generate_chain_motion import generate as generate_chain_motion
from generate_machine_art import make as generate_machine_art

ROOT = Path(__file__).resolve().parents[1] / "src/main/resources/assets/zonezero_chainsaw"
SOUNDS = ROOT / "sounds"
TEXTURES = ROOT / "textures/item"
SOUNDS.mkdir(parents=True, exist_ok=True)
TEXTURES.mkdir(parents=True, exist_ok=True)

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
    generate_original_audio(SOUNDS)
    texture()  # Keeps legacy chain_coast.png available for older asset references.
    generate_chain_motion(TEXTURES)  # Replaces running atlas with 8 original moving frames.
    generate_original_textures(TEXTURES)
    generate_original_hud(ROOT / 'textures/gui')
    generate_machine_art(ROOT / 'textures/block')
    print("Generated six original OGG effects, 24 material sprites, two HUDs and 12 moving chain atlases.")
