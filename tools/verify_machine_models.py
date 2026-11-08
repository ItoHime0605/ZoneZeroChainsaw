#!/usr/bin/env python3
"""Verify the shipped ORIGINAL fermenter and upgrade-station voxel models.

Catches missing/purple-black textures, broken active state, invalid item
parents, zero-thickness/oversized parts, and accidental vanilla textures.
Runs before Gradle compiles (generated PNG files must already exist).
"""
from pathlib import Path
import json
import struct
import zlib

ROOT=Path("src/main/resources/assets/zonezero_chainsaw")
BLOCK=ROOT/"models/block"
ITEM=ROOT/"models/item"
TEXTURES=ROOT/"textures/block"
STATES=ROOT/"blockstates"

def load(path):
    return json.loads(Path(path).read_text("utf-8"))

models={
    "fermentation_barrel":load(BLOCK/"fermentation_barrel.json"),
    "fermentation_barrel_idle":load(BLOCK/"fermentation_barrel_idle.json"),
    "fermentation_barrel_active":load(BLOCK/"fermentation_barrel_active.json"),
    "chainsaw_upgrade_station":load(BLOCK/"chainsaw_upgrade_station.json"),
}
expected_materials=set()
for name,model in models.items():
    assert model["parent"]=="minecraft:block/block", name
    assert model["ambientocclusion"] is True
    textures=model["textures"]
    assert textures["particle"].startswith("zonezero_chainsaw:block/"), name
    count=len(model["elements"])
    minimum=75 if name.startswith("fermentation") else 90
    assert count>=minimum,(name,count,minimum)
    for i,element in enumerate(model["elements"]):
        assert set(element)=={"from","to","faces"},(name,i)
        a,b=element["from"],element["to"]
        assert len(a)==3 and len(b)==3
        assert all(0<=v<w<=16 for v,w in zip(a,b)),(name,i,a,b)
        assert set(element["faces"])=={"north","south","east","west","up","down"}
        for face in element["faces"].values():
            key=face["texture"]
            assert key.startswith("#") and key[1:] in textures,(name,i,key)
            if "uv" in face:
                assert len(face["uv"])==4
                assert all(0<=x<=16 for x in face["uv"])
    for key,path in textures.items():
        assert path.startswith("zonezero_chainsaw:block/"),(name,key,path)
        expected_materials.add(path.split("/")[-1])
    print(name,":",count,"real 3D parts")

# Exactly the same shape for running and idle (no world mesh popping).
idle=models["fermentation_barrel_idle"]
running=models["fermentation_barrel_active"]
assert idle["elements"]==running["elements"]
assert models["fermentation_barrel"]==idle
for key in ("glass","gauge","lamp","liquid"):
    assert idle["textures"][key].endswith("_idle")
    assert running["textures"][key].endswith("_active")
for key in set(idle["textures"])-{"glass","gauge","lamp","liquid"}:
    assert idle["textures"][key]==running["textures"][key]

for name in ("fermentation_barrel","chainsaw_upgrade_station"):
    item=load(ITEM/(name+".json"))
    parent=(
        "zonezero_chainsaw:block/fermentation_barrel_idle"
        if name=="fermentation_barrel"
        else "zonezero_chainsaw:block/chainsaw_upgrade_station"
    )
    assert item["parent"]==parent,(name,item["parent"])
    assert 0.55<=item["display"]["gui"]["scale"][0]<=0.8
    states=load(STATES/(name+".json"))["variants"]
    for facing,angle in (("north",0),("east",90),("south",180),("west",270)):
        for active in ((False,True) if name=="fermentation_barrel" else (None,)):
            key="facing="+facing+(
                ",active="+("true" if active else "false") if active is not None else ""
            )
            assert key in states,(name,key)
            entry=states[key]
            expected=name
            if active is not None:
                expected+="_active" if active else "_idle"
            assert entry["model"]=="zonezero_chainsaw:block/"+expected,(name,key)
            assert entry["y"]==angle,(name,key)

def image_pixels(path):
    raw=path.read_bytes()
    assert raw.startswith(b"\x89PNG\r\n\x1a\n"),path
    width,height=struct.unpack(">II",raw[16:24])
    assert (width,height)==(64,64),(path,width,height)
    assert (raw[24],raw[25])==(8,6),path
    i=8
    chunks=bytearray()
    while i<len(raw):
        n=struct.unpack(">I",raw[i:i+4])[0]
        kind=raw[i+4:i+8]
        if kind==b"IDAT":chunks+=raw[i+8:i+8+n]
        i+=12+n
    pixels=zlib.decompress(chunks)
    assert len(pixels)==64*(1+64*4)
    assert all(pixels[y*(1+256)]==0 for y in range(64))
    return [tuple(pixels[(y*257+1+x*4):(y*257+1+x*4+4)])
            for y in range(64) for x in range(64)]

# Broad hand-painted surfaces, no random point-speckle patterns.
all_names={p.name.removesuffix(".png") for p in TEXTURES.glob("*.png")}
for name in expected_materials:
    assert name in all_names, "Purple-black missing texture: "+name
    p=TEXTURES/(name+".png")
    pixels=image_pixels(p)
    palette=set(pixels)
    assert 2<=len(palette)<70, (name,"Unintended noise or blank image",len(palette))
    assert all(color[3]==255 for color in pixels),name
assert "ferment_v2_glass_idle" in expected_materials
assert "ferment_v2_glass_active" in expected_materials
assert "station_v2_red" in expected_materials
assert "station_v2_screen" in expected_materials
assert models["fermentation_barrel_idle"]["textures"]["wood"] != "minecraft:block/barrel_side"

# UI still references the same block registration, no assets or ID rename.
java=(Path("src/main/java/tw/zonezero/chainsaw/ZoneZeroChainsaw.java")
       .read_text("utf-8"))
assert 'BLOCKS.register("fermentation_barrel"' in java
assert 'BLOCKS.register("chainsaw_upgrade_station"' in java
generator=Path("tools/generate_machine_art.py").read_text("utf-8")
assert "random" not in generator and "noise(" not in generator
assert "ferment_v2_wood" in generator and "station_v2_steel" in generator
print("PASS: clean authored 64x64 art, valid states, model geometry and",
      len(expected_materials),"referenced machine materials")
