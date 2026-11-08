#!/usr/bin/env python3
"""Generate six original, license-independent chainsaw sound effects.

No external samples, Minecraft audio or recorded recordings are used.
This synthesizer produces deterministic, mono Ogg Vorbis in PLAYERS category:
start (0.90s), engine (0.60s), cut (0.40s),
contact (0.22s), stop (1.80s) and stall (0.70s).

The idle and cutting loops fit exact 20-TPS server clip intervals. Their edges
are short crossfades rather than hard stops, and their motor harmonics form
whole cycles across a period (avoids clicks and the old repeated "cough").
"""
import math
import random
import shutil
import struct
import subprocess
import tempfile
import wave
from pathlib import Path

RATE = 24000
TAU = 2.0 * math.pi
CLIPS = {
    "start": (.90, 457),
    "engine": (.60, 658),
    "cut": (.40, 859),
    "contact": (.22, 1060),
    "stop": (1.80, 1261),
    "stall": (.70, 1462),
}
TARGET_RMS = {
    "start": .155, "engine": .153, "cut": .167,
    "contact": .139, "stop": .160, "stall": .152
}

def clamp(x, lo=0., hi=1.):
    return max(lo, min(hi, x))

def smooth(x):
    x=clamp(x)
    return x*x*(3.-2.*x)

def sine(hz, t, phase=0.):
    return math.sin(TAU*hz*t + phase)

def running(t, period, cutting):
    # Whole-cycle frequencies: 90Hz and 25Hz for 0.6s;
    # 150Hz and 35Hz for 0.4s. Both end at their starting phase.
    fundamental = 150.0 if cutting else 90.0
    pulse = 35.0 if cutting else 25.0
    whine = (1050., 1475., 1725.) if cutting else (675., 975., 1275.)
    periodic_noise = (
        .20*sine(whine[0], t, .16)+
        .13*sine(whine[1], t, 1.02)+
        .07*sine(whine[2], t, 2.10)
    )
    engine = (.61*sine(fundamental, t) +
              .30*sine(2*fundamental, t, .24) +
              .15*sine(3*fundamental, t, .75) +
              .085*sine(4*fundamental, t, 1.10))
    chuff = 1.0 + .12*sine(pulse, t, -.27)
    return (.75 if cutting else .87)*engine*chuff + periodic_noise

def generate_wave(kind, duration, seed):
    rng=random.Random(seed)
    length=round(RATE*duration)
    samples=[]
    lowpass=0.
    phase=0.
    for i in range(length):
        t=i/RATE
        u=t/duration
        white=rng.random()*2.-1.
        lowpass=lowpass*.84 + white*.16
        highpass=white-lowpass
        if kind in ("engine", "cut"):
            s=running(t, duration, kind=="cut")
            # A barely perceptible 5ms audio boundary ramp keeps repeated
            # playSound clips quiet at packet/tick boundaries.
            ramp=min(1., t/.005, (duration-t)/.005)
            s*=max(0.,ramp)
        elif kind=="start":
            # Three starter pulls, a hesitant combustion catch, then RPM rises.
            pulse=sum(math.exp(-abs(t-at)*32.) *
                      (1.4*sine(115+70*j,t) + .8*highpass)
                      for j,at in enumerate((.11,.32,.54)))
            hz=38. + 58.*smooth((u-.28)/.60)
            phase+=TAU*hz/RATE
            motor=(.57*math.sin(phase)+.27*math.sin(phase*2+.38)
                   +.13*math.sin(phase*3+.11))
            engage=smooth((u-.45)/.35)
            fade=smooth((1.-u)/.13)
            s=(pulse*.30 + engage*(motor+.12*highpass))*fade
        elif kind=="stop":
            # 40-tick release: mechanical coast, rattle and falling tone.
            hz=94.-77.*smooth(u)
            phase+=TAU*hz/RATE
            amp=(1.-u)**1.65
            crank=.58*math.sin(phase)+.31*math.sin(2.*phase+.19)
            wobble=1.+.15*math.sin(TAU*(11.*t+6.*t*t))
            s=amp*wobble*(crank+.15*highpass)
        elif kind=="stall":
            hz=87.-43.*smooth(u)
            phase+=TAU*hz/RATE
            firing=.4+.6*max(0.,math.sin(TAU*(12.*t+8.*t*t)))
            clunk=math.exp(-((u-.71)/.04)**2) * (.9*highpass+.25*sine(680,t))
            s=(1.-u)*firing*(.62*math.sin(phase)+.28*math.sin(2*phase)) + .50*clunk
        else:
            # Impact ONLY on authorized hit, not just on empty-air holding.
            attack=math.exp(-17*t)
            metal=(.32*sine(780,t)+.20*sine(1150,t)+
                   .10*sine(1700,t)+.45*highpass)
            s=attack*metal
        # Light saturation softens the mechanically layered audio.
        samples.append(math.tanh(1.18*s))
    rms=(sum(s*s for s in samples)/len(samples))**.5
    gain=TARGET_RMS[kind]/max(rms, 1e-8)
    gain=min(1.85,gain)
    return [max(-.89,min(.89,gain*s)) for s in samples]

def create(directory):
    directory=Path(directory)
    directory.mkdir(parents=True,exist_ok=True)
    ffmpeg=shutil.which("ffmpeg")
    if not ffmpeg:
        raise RuntimeError("ffmpeg must be installed to generate original OGG Vorbis.")
    with tempfile.TemporaryDirectory() as tmp:
        for kind,(duration,seed) in CLIPS.items():
            samples=generate_wave(kind,duration,seed)
            wav=Path(tmp)/(kind+".wav")
            with wave.open(str(wav),"wb") as out:
                out.setnchannels(1)
                out.setsampwidth(2)
                out.setframerate(RATE)
                out.writeframes(b"".join(struct.pack("<h",int(s*32767)) for s in samples))
            output=directory/(kind+".ogg")
            subprocess.run([
                ffmpeg,"-hide_banner","-loglevel","error","-nostdin","-y",
                "-i",str(wav),"-c:a","libvorbis","-q:a","5",str(output)
            ],check=True)
            if output.stat().st_size < 1000:
                raise RuntimeError("Unexpected silent or missing OGG: "+kind)
    return len(CLIPS)

if __name__=="__main__":
    root=Path(__file__).resolve().parents[1]/"src/main/resources/assets/zonezero_chainsaw/sounds"
    print("Generated independently authored chainsaw sounds:",create(root))
