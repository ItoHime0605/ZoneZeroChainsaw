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
assert len(base["elements"]) == 120, "The full redesigned 120-piece model is missing"
assert len(config["overrides"]) == 101, "Missing 16-dye / running / 3-stage coast / cutting models"
for element in base["elements"]:
    assert all(-16 <= f < t <= 32 for f, t in zip(element["from"], element["to"]))
    for face in element["faces"].values():
        assert face["texture"][1:] in base["textures"]
        if "uv" in face:
            assert len(face["uv"]) == 4 and all(0 <= n <= 16 for n in face["uv"])


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
    for variant in ("", "_active", "_running", "_coast", "_cutting",
                    "_coast_fast", "_coast_medium", "_coast_slow"):
        model = json.loads((root / f"chainsaw_{color}{variant}.json").read_text("utf-8"))
        assert model["parent"].startswith("zonezero_chainsaw:item/chainsaw_"), (color, variant)
        if not variant:
            assert "orange" in model["textures"], color
            check_texture(model["textures"]["orange"])
        if variant in ("_running", "_cutting", "_coast_fast",
                       "_coast_medium", "_coast_slow"):
            assert model["textures"]["orange"] == f"zonezero_chainsaw:item/housing_{color}"
    assert config["overrides"][5 + i * 6]["predicate"]["zonezero_chainsaw:dye_color"] == (i + 1) / 16
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
# Preserve the calibrated animation hand anchors while replacing the mesh.
# The new front transverse U is visibly squared and braced rearward.
rear = [e for e in base["elements"] if e["faces"]["north"]["texture"] == "#rubber"
        and e["from"][0] > 17.4 and e["from"][1] <= 6.5]
front = [e for e in base["elements"] if e["faces"]["north"]["texture"] == "#rubber"
         and 6.7 < e["from"][0] < 8 and e["to"][1] >= 13.55]
guards = [e for e in base["elements"] if e["faces"]["north"]["texture"] == "#rubber"
          and 9.2 < e["from"][0] < 10.0 and e["to"][1] >= 13.0]
assert rear and front and guards, "Rear grip or extended front U handle missing"
assert (min(e["from"][0] for e in blade) < -8.5
        and max(e["to"][0] for e in blade) > 6), "Guide bar too short"

# Every color uses the original cast-panel art, while the guide, chain,
# rubber, vent and rivets have their own independently drawn materials.
original_materials = ("guide_blood_v3", "chain_graphite_v3", "metal_cast_v3",
    "rubber_grip_v3", "vent_cast_v3", "cap_steel_v3",
    "frame_black_v3", "bolt_steel_v3")
assert len(original_materials) == 8
for name in original_materials:
    check_texture("zonezero_chainsaw:item/" + name)
for color in colors:
    check_texture("zonezero_chainsaw:item/housing_" + color)
assert Path("tools/generate_chainsaw_art.py").is_file()

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
# Animations use eight equal 64x64 frames (64x512 atlas) and three
# controlled coast speeds. Opposite link travel and nose rotation are authored
# independently rather than swapping one generic animated texture.
assert Path("tools/generate_chain_motion.py").is_file()
import zlib
def read_atlas(file):
    png=file.read_bytes()
    assert png[:8] == b"\x89PNG\r\n\x1a\n", file
    assert struct.unpack(">II",png[16:24]) == (64,512), file
    assert png[24:26] == bytes([8,6]), file
    at=8
    data=bytearray()
    while at < len(png):
        n=struct.unpack(">I",png[at:at+4])[0]
        k=png[at+4:at+8]
        if k==b"IDAT": data+=png[at+8:at+8+n]
        at+=n+12
    raw=zlib.decompress(data)
    assert len(raw)==(64*4+1)*512
    frame_size=(64*4+1)*64
    return [raw[i*frame_size:(i+1)*frame_size] for i in range(8)]

for speed, frametime in (("running",1),("coast_fast",2),
                         ("coast_medium",4),("coast_slow",8)):
    layers={}
    for layer in ("chain","chain_reverse","chain_nose"):
        name=layer+"_"+speed
        png=assets / "textures/item" / (name+".png")
        assert png.is_file(), name
        frames=read_atlas(png)
        assert frames[0] != frames[1], name
        assert len(set(frames)) == 8, "Identical frame in animation: "+name
        mcmeta=json.loads((png.with_suffix(".png.mcmeta")).read_text("utf-8"))
        assert mcmeta["animation"]["frametime"] == frametime, name
        layers[layer]=frames
    assert layers["chain"][1] != layers["chain_reverse"][1], "Upper/lower links same direction"
    for name in (speed,):
        model=json.loads((root / ("chainsaw_"+name+".json")).read_text("utf-8"))
        assert model["textures"]["chain"] == "zonezero_chainsaw:item/chain_"+speed
        assert model["textures"]["chain_reverse"] == "zonezero_chainsaw:item/chain_reverse_"+speed
        assert model["textures"]["chain_nose"] == "zonezero_chainsaw:item/chain_nose_"+speed
# Each of 16 dye variants inherits from the SAME moving parent, preserving
# all first-person/third-person camera transforms.
for color in colors:
    for speed in ("running","coast_fast","coast_medium","coast_slow"):
        m=json.loads((root / f"chainsaw_{color}_{speed}.json").read_text("utf-8"))
        assert m["parent"] == "zonezero_chainsaw:item/chainsaw_"+speed

# Inspect override priority at speed thresholds. Cutting must win if active.
overrides=config["overrides"]
assert [v["predicate"]["zonezero_chainsaw:engine"] for v in overrides[:4]] == [.25,.5,.75,1.0]
assert overrides[4]["predicate"]["zonezero_chainsaw:cutting"] == 1.0
for i,color in enumerate(colors):
    group=overrides[5+i*6:5+(i+1)*6]
    assert len(group)==6
    dye=(i+1)/16
    for v in group: assert v["predicate"]["zonezero_chainsaw:dye_color"]==dye
    assert [v["predicate"].get("zonezero_chainsaw:engine") for v in group[1:5]] == [.25,.5,.75,1.0]
    assert group[5]["predicate"]["zonezero_chainsaw:cutting"]==1.0


for name in ("start", "engine", "cut", "contact", "stop", "stall"):
    path = assets / "sounds" / (name + ".ogg")
    assert path.is_file() and path.read_bytes()[:4] == b"OggS"
assert Path("tools/generate_chainsaw_sounds.py").is_file()
assert Path("tools/verify_audio.py").is_file()

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
        # Running now inherits the shared model transform and color separately.
        inherited = json.loads((root / ("chainsaw_"+state+".json")).read_text("utf-8"))
        for hand in thirdperson:
            assert model.get("display", inherited["display"]).get(hand) == base["display"][hand], (color,state,hand)
# Model geometry was designed with a U at x≈7.33 and rear handle at
# x≈17.96. Both remain present; the support arm is forward of the main arm.
rears = [e for e in base["elements"] if e["faces"]["north"]["texture"] == "#rubber"
         and e["from"][0] > 17]
fronts = [e for e in base["elements"] if e["faces"]["north"]["texture"] == "#rubber"
          and 6.7 < e["from"][0] < 8.0 and e["to"][1] > 13.3]
assert rears and fronts, "Missing either rear or front rubber grip"
# The custom HUD has NO dark panel: two transparent, independently drawn
# original 112x40 guide-bar images. Text NEVER changes color on R toggle.
import zlib
hud_sprites={}
for state in ("idle", "running"):
    name = assets / "textures/gui" / ("guide_" + state + ".png")
    assert name.is_file(), f"Missing original HUD sprite: {name}"
    png = name.read_bytes()
    assert png[:8] == b"\x89PNG\r\n\x1a\n"
    assert struct.unpack(">II", png[16:24]) == (112,40)
    assert png[24] == 8 and png[25] == 6, "HUD sprite must use RGBA"
    pointer=8
    compressed=bytearray()
    while pointer<len(png):
        length=struct.unpack(">I",png[pointer:pointer+4])[0]
        kind=png[pointer+4:pointer+8]
        if kind==b"IDAT":
            compressed+=png[pointer+8:pointer+8+length]
        pointer+=12+length
    raw=zlib.decompress(compressed)
    assert len(raw)==(112*4+1)*40
    assert all(raw[y*(112*4+1)]==0 for y in range(40))
    pixels=[tuple(raw[(y*(112*4+1)+1)+x*4:(y*(112*4+1)+1)+x*4+4])
            for y in range(40) for x in range(112)]
    assert sum(p[3]==0 for p in pixels)>1200, "HUD became a solid panel"
    assert sum(p[3]==255 for p in pixels)>1200, "HUD icon missing"
    hud_sprites[state]=[p for p in pixels if p[3]==255]
idle=hud_sprites["idle"]
red=hud_sprites["running"]
assert sum(p[0]+p[1]+p[2] for p in idle)/len(idle)>420, "Idle guide not bright silver"
assert sum(p[0]-p[1] for p in red)/len(red)>60, "Running guide not red"
assert Path("tools/generate_chainsaw_hud.py").is_file()
hud_java=Path("src/main/java/tw/zonezero/chainsaw/ChainsawHud.java").read_text("utf-8")
hud_style=Path("src/main/java/tw/zonezero/chainsaw/logic/ChainsawHudStyle.java").read_text("utf-8")
assert "running ? BAR_RUNNING : BAR_IDLE" in hud_java
assert "ChainsawHudStyle.TEXT_COLOR" in hud_java
assert "ChainsawHudStyle.TEXT_OUTLINE" in hud_java
assert "g.fill(" not in hud_java, "Unwanted intrusive dark HUD panel"
assert "0xfff0f4f2" in hud_style

# Compile-time integration with the uploaded playerAnimator API is mandatory.
gradle = Path("build.gradle").read_text("utf-8")
toml = Path("src/main/resources/META-INF/neoforge.mods.toml").read_text("utf-8")
assert "player-animation-lib-forge:2.0.4+1.21.1" in gradle
assert 'modId="playeranimator"' in toml
assert Path("src/main/java/tw/zonezero/chainsaw/ChainsawPlayerAnimator.java").is_file()

print("PASS: 16 dye colors, 67 states, animated chain, OGG sounds, held poses and playerAnimator dependency.")
