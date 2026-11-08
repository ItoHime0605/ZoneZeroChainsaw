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
assert len(config["overrides"]) == 67, "Missing idle/running/cutting/coast model combinations"
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
    for variant in ("", "_active", "_running", "_coast", "_cutting"):
        model = json.loads((root / f"chainsaw_{color}{variant}.json").read_text("utf-8"))
        assert model["parent"].startswith("zonezero_chainsaw:item/chainsaw_"), (color, variant)
        if not variant:
            assert "orange" in model["textures"], color
            check_texture(model["textures"]["orange"])
    assert config["overrides"][3 + i * 4]["predicate"]["zonezero_chainsaw:dye_color"] == (i + 1) / 16
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
# All states MUST share the same first-person transform. The held
# input smoothly advances the PoseStack instead; never jump models per hit.
firstperson = ("firstperson_righthand", "firstperson_lefthand")
for hand in firstperson:
    pose = base["display"][hand]
    assert pose["rotation"] == ([8, -65, -5] if hand.endswith("righthand") else [8, 65, 5])
    assert 3.5 <= pose["translation"][1] <= 5.0
    assert .55 <= pose["scale"][0] <= .65
    assert 0 < abs(pose["translation"][0]) <= 5
for mode in ("chainsaw_active", "chainsaw_running"):
    model = json.loads((root / (mode + ".json")).read_text("utf-8"))
    for hand in firstperson:
        assert model["display"][hand] == base["display"][hand], (mode, hand)
for color in colors:
    active = json.loads((root / f"chainsaw_{color}_active.json").read_text("utf-8"))
    for hand in firstperson:
        assert active["display"][hand] == base["display"][hand], (color, hand)
    # Dyed running variants inherit the color base without overriding
    # firstperson: they must NOT reintroduce a sudden camera-space jump.
    running = json.loads((root / f"chainsaw_{color}_running.json").read_text("utf-8"))
    assert "display" not in running or all(
        running["display"].get(hand, base["display"][hand]) == base["display"][hand]
        for hand in firstperson), color
for name in ("chain_running", "chain_coast"):
    png = assets / "textures/item" / (name + ".png")
    assert png.is_file(), f"Missing animated texture: {name}"
    data = png.read_bytes()
    assert data[:8] == b"\x89PNG\r\n\x1a\n"
    assert struct.unpack(">II", data[16:24]) == (64, 256)
    meta = json.loads((assets / "textures/item" / (name + ".png.mcmeta")).read_text("utf-8"))
    assert meta["animation"]["frametime"] > 0

for name in ("start", "engine", "cut", "stop"):
    path = assets / "sounds" / (name + ".ogg")
    assert path.is_file() and path.read_bytes()[:4] == b"OggS"

# All held models use the same third-person physical grip anchor.
# No position/scale snap when R starts or when continuous cutting begins.
thirdperson = ("thirdperson_righthand", "thirdperson_lefthand")
for hand in thirdperson:
    display = base["display"][hand]
    assert display["rotation"] == ([0,-90,0] if hand.endswith("righthand") else [0,90,0])
    assert display["scale"] == [.59,.59,.59]
    assert display["translation"][2] == -4.6
for mode in ("chainsaw_running","chainsaw_active"):
    model = json.loads((root / (mode + ".json")).read_text("utf-8"))
    for hand in thirdperson:
        assert model["display"][hand] == base["display"][hand], (mode,hand)
for color in colors:
    for state in ("running","active"):
        model = json.loads((root / f"chainsaw_{color}_{state}.json").read_text("utf-8"))
        for hand in thirdperson:
            assert model["display"][hand] == base["display"][hand], (color,state,hand)
# Model geometry was designed with a U at x≈7.33 and rear handle at
# x≈17.96. Both remain present; the support arm is forward of the main arm.
rears = [e for e in base["elements"] if e["faces"]["north"]["texture"] == "#rubber"
         and e["from"][0] > 17]
fronts = [e for e in base["elements"] if e["faces"]["north"]["texture"] == "#rubber"
          and 6.7 < e["from"][0] < 8.0 and e["to"][1] > 13.3]
assert rears and fronts, "Missing either rear or front rubber grip"
# Compile-time integration with the uploaded playerAnimator API is mandatory.
gradle = Path("build.gradle").read_text("utf-8")
toml = Path("src/main/resources/META-INF/neoforge.mods.toml").read_text("utf-8")
assert "player-animation-lib-forge:2.0.4+1.21.1" in gradle
assert 'modId="playeranimator"' in toml
assert Path("src/main/java/tw/zonezero/chainsaw/ChainsawPlayerAnimator.java").is_file()

print("PASS: 16 dye colors, 67 states, animated chain, OGG sounds, held poses and playerAnimator dependency.")
