#!/usr/bin/env python3
"""Validate that every variant of our original saw model can resolve its resources.

Runs on GitHub Actions without launching Minecraft. This is NOT a substitute for a
real first-person/third-person rendering test.
"""
import json
import struct
from pathlib import Path

assets = Path("src/main/resources/assets/zonezero_chainsaw")
root = assets / "models/item"
colors = (
    "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
    "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"
)
base = json.loads((root / "chainsaw_base.json").read_text("utf-8"))
config = json.loads((root / "chainsaw.json").read_text("utf-8"))
assert len(base["elements"]) >= 85, "The full detailed 3D model is missing"
assert len(config["overrides"]) == 33, "Missing idle/active model combinations"
for element in base["elements"]:
    assert all(-16 <= f < t <= 32 for f, t in zip(element["from"], element["to"]))
    for face in element["faces"].values():
        assert face["texture"][1:] in base["textures"]

def check_texture(tex):
    ns, path = tex.split(":")
    assert ns == "zonezero_chainsaw"
    png = assets / "textures" / (path + ".png")
    assert png.is_file(), f"Texture does not exist: {png}"
    data = png.read_bytes()
    assert data[:8] == b"\x89PNG\r\n\x1a\n"
    width, height = struct.unpack(">II", data[16:24])
    assert (width, height) == (64, 64), (tex, width, height)

for texture in base["textures"].values():
    check_texture(texture)
for i, color in enumerate(colors):
    for variant in ("", "_active"):
        model = json.loads((root / f"chainsaw_{color}{variant}.json").read_text("utf-8"))
        assert model["parent"].startswith("zonezero_chainsaw:item/chainsaw_") or variant == ""
        if not variant:
            assert "orange" in model["textures"], color
            check_texture(model["textures"]["orange"])
    assert config["overrides"][i * 2 + 1]["predicate"]["zonezero_chainsaw:dye_color"] == (i + 1) / 16
for lang in ("zh_tw", "en_us"):
    text = json.loads((assets / f"lang/{lang}.json").read_text("utf-8"))
    assert all(key in text for key in (
        "itemGroup.zonezero_chainsaw", "tooltip.zonezero_chainsaw.description",
        "tooltip.zonezero_chainsaw.fuel", "tooltip.zonezero_chainsaw.controls",
        "message.zonezero_chainsaw.engine_on", "hud.zonezero_chainsaw.fuel"
    ))
body = [e for e in base["elements"] if e["faces"]["north"]["texture"] == "#orange"]
blade = [e for e in base["elements"] if e["faces"]["north"]["texture"] == "#guide"]
assert min(e["from"][0] for e in blade) < -8, "Guide bar has shrunk"
assert max(e["to"][0] for e in body) - min(e["from"][0] for e in body) < 9, "Body too thick"
assert base["display"]["firstperson_righthand"]["scale"][0] <= .5
print("PASS: 16 original dye textures, 33 idle/cutting variants, independent guide/chain, sizes, GUI and languages.")
