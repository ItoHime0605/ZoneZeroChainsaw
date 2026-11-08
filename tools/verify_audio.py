#!/usr/bin/env python3
"""CI sound quality sanity checks (not subjective listening validation)."""
import hashlib
import json
import math
from pathlib import Path
import shutil
import struct
import subprocess

ROOT = Path("src/main/resources/assets/zonezero_chainsaw")
EXPECTED = {
    "start": .90,
    "engine": .60,
    "cut": .40,
    "contact": .22,
    "stop": 1.80,
    "stall": .70,
}
assert shutil.which("ffmpeg") and shutil.which("ffprobe"), "Audio encoder and probe missing"
sound_events = json.loads((ROOT / "sounds.json").read_text("utf-8"))
assert set(sound_events) == set(EXPECTED)
digest = set()
for name,duration in EXPECTED.items():
    path = ROOT / "sounds" / (name+".ogg")
    payload = path.read_bytes()
    assert payload.startswith(b"OggS") and len(payload) > 1200, name
    digest.add(hashlib.sha256(payload).digest())
    assert sound_events[name]["subtitle"] == "subtitles.zonezero_chainsaw."+name
    assert sound_events[name]["sounds"][0]["name"] == "zonezero_chainsaw:"+name
    info = json.loads(subprocess.check_output([
        "ffprobe","-v","error","-show_entries",
        "format=duration:stream=sample_rate,channels,codec_name",
        "-of","json",str(path)], text=True))
    assert len(info["streams"]) == 1, name
    stream = info["streams"][0]
    assert stream["codec_name"] == "vorbis", name
    assert int(stream["sample_rate"]) == 24000, name
    assert int(stream["channels"]) == 1, name
    assert abs(float(info["format"]["duration"]) - duration) < .035, (name,info["format"]["duration"])
    # Test decoded levels: clipping, silences and synthesized loop boundaries.
    output = subprocess.check_output([
        "ffmpeg","-v","error","-nostdin","-i",str(path),
        "-f","s16le","-ac","1","-ar","24000","pipe:1"])
    assert len(output) > duration * 24000 * 1.9, name
    frames=len(output)//2
    signal=struct.unpack("<"+str(frames)+"h",output)
    peak=max(abs(x) for x in signal)/32768.0
    rms=math.sqrt(sum((x/32768.0)**2 for x in signal)/frames)
    assert .02 < rms < .30, (name,rms)
    assert peak < .98, (name,peak)
    if name in ("engine", "cut"):
        # Looped audio begins and ends softly to avoid clicks from
        # tick/network playback skew. The engine cannot silence entirely.
        assert abs(signal[0]) < 650 and abs(signal[-1]) < 650, name
        assert rms > .07, name
    print(name,"duration=",round(float(info["format"]["duration"]),3),
          "RMS=",round(rms,3),"peak=",round(peak,3))
assert len(digest)==len(EXPECTED), "Some different events share the same sample"
for lang in ("zh_tw","en_us"):
    keys=json.loads((ROOT / "lang" / (lang+".json")).read_text("utf-8"))
    for name in EXPECTED:
        assert "subtitles.zonezero_chainsaw."+name in keys
print("PASS: six original mono, duration-matched spatial chainsaw sound events")
