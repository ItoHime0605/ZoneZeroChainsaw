#!/usr/bin/env python3
"""Draw two original, transparent 112x40 chainsaw-guide HUD sprites.

Art direction: a slim forged guide bar, raised blade rail, articulated chain
teeth, bevelled nose, recessed steel channel and tiny rivets. Uses no Minecraft
or TaCZ textures. The engine-on edition changes ONLY the bar's material palette
to red; the numeric fuel label is rendered separately in a constant color.
"""
from pathlib import Path
import struct
import zlib

W,H=112,40

def color(h,alpha=255):
    v=bytes.fromhex(h.lstrip("#"))
    return (v[0],v[1],v[2],alpha)

class Layer:
    def __init__(self):
        self.p=bytearray(W*H*4)
    def dot(self,x,y,c):
        if 0<=x<W and 0<=y<H:
            i=(y*W+x)*4
            self.p[i:i+4]=bytes(c)
    def rect(self,x,y,w,h,c):
        for yy in range(max(0,y),min(H,y+h)):
            for xx in range(max(0,x),min(W,x+w)):
                self.dot(xx,yy,c)
    @staticmethod
    def contains(px,py,poly):
        inside=False
        x1,y1=poly[-1]
        for x2,y2 in poly:
            if (y1>py)!=(y2>py):
                crossing=(x2-x1)*(py-y1)/(y2-y1)+x1
                if px<crossing:inside=not inside
            x1,y1=x2,y2
        return inside
    def shape(self,pts,c,dx=0,dy=0):
        pts=[(x+dx,y+dy) for x,y in pts]
        for y in range(max(0,min(pt[1] for pt in pts)),min(H,max(pt[1] for pt in pts)+1)):
            for x in range(max(0,min(pt[0] for pt in pts)),min(W,max(pt[0] for pt in pts)+1)):
                if self.contains(x+.5,y+.5,pts):self.dot(x,y,c)
    def png(self,path):
        raw=b"".join(b"\x00"+bytes(self.p[y*W*4:(y+1)*W*4]) for y in range(H))
        def chunk(kind,data):
            return (struct.pack(">I",len(data))+kind+data+
                    struct.pack(">I",zlib.crc32(kind+data)&0xffffffff))
        path.parent.mkdir(parents=True,exist_ok=True)
        path.write_bytes(
            b"\x89PNG\r\n\x1a\n"+
            chunk(b"IHDR",struct.pack(">IIBBBBB",W,H,8,6,0,0,0))+
            chunk(b"IDAT",zlib.compress(raw,9))+
            chunk(b"IEND",b""))

# Both states share exactly the same silhouette and mechanical details.
OUTER=[(15,5),(102,5),(107,8),(108,10),(108,30),(14,31),
       (9,29),(5,26),(3,22),(3,17),(5,13),(9,9)]
INNER=[(16,7),(101,7),(105,9),(106,11),(106,28),(15,29),
       (10,27),(7,24),(5,21),(5,18),(7,14),(10,11)]
SLOT=[(25,13),(100,13),(102,15),(102,24),(27,24),
      (21,22),(19,19),(21,16)]
SLOT_INNER=[(27,16),(99,16),(99,21),(28,21),(23,20),(23,18)]

PALETTES={
    "idle":{
        "edge":"#53636e","rail":"#e6eef0","top":"#f2f6f3",
        "mid":"#d9e1e3","bottom":"#b0c1c7","low":"#8a9ea7",
        "tooth":"#b5c4c9","tooth_light":"#f1f5f2",
        "channel":"#6c828e","channel_fill":"#d8e2e5",
        "bolt":"#eaf0ef","shadow":"#34434d"
    },
    "running":{
        "edge":"#762b31","rail":"#ffe0dc","top":"#ffaaa0",
        "mid":"#f36861","bottom":"#cf4546","low":"#a13239",
        "tooth":"#d45759","tooth_light":"#ffc5bf",
        "channel":"#932e37","channel_fill":"#f88781",
        "bolt":"#ffd1c6","shadow":"#58292f"
    }
}

def render(state):
    a=Layer()
    P={k:color(v) for k,v in PALETTES[state].items()}
    # Small offset contact shadow, NEVER a black rectangular panel.
    a.shape(OUTER,color("#35414a",82),dx=2,dy=2)
    a.shape(OUTER,P["edge"])
    for y in range(6,31):
        band="top" if y<10 else "mid" if y<19 else "bottom" if y<27 else "low"
        for x in range(4,108):
            if Layer.contains(x+.5,y+.5,INNER):
                shade=P[band]
                # Controlled pixel wear on the guide, never random noise.
                if (x*7+y*19)%43==0 and y>10 and y<27:
                    shade=P["rail"] if y<18 else P["low"]
                a.dot(x,y,shade)
    # Forge-rolled top and lower rails add depth without over-styling.
    a.rect(16,7,87,2,P["rail"])
    a.rect(16,28,87,2,P["edge"])
    a.rect(103,9,3,19,P["low"])
    # Recessed longitudinal slot, inspired by a guide bar, not the user's sketch.
    a.shape(SLOT,P["channel"])
    a.shape(SLOT_INNER,P["channel_fill"])
    a.rect(30,16,66,1,P["rail"])
    a.rect(29,21,69,1,P["bottom"])
    a.rect(29,24,72,1,P["low"])
    # Chain shoes: offset top and bottom for moving-link illusion.
    for i in range(12):
        tx=15+i*7
        a.rect(tx,2,4,4,P["edge"])
        a.rect(tx+1,2,2,2,P["tooth_light"])
        a.rect(tx+1,4,3,1,P["tooth"])
        bx=18+i*7
        a.rect(bx,31,4,4,P["edge"])
        a.rect(bx+1,32,2,2,P["tooth"])
        a.rect(bx+1,34,2,1,P["tooth_light"])
    # The nose wraps around the chain sprocket.
    for x,y,w,h in ((2,13,3,3),(0,18,3,4),(2,24,3,3),(7,29,3,3)):
        a.rect(x,y,w,h,P["edge"])
        a.rect(x+1,y,w-1,1,P["tooth_light"])
    # Light catches on a pair of forged mounting pins.
    for x,y in ((96,10),(98,26)):
        a.rect(x,y,4,4,P["edge"])
        a.rect(x+1,y+1,2,2,P["bolt"])
    # Short rear metal lug gives the icon a mechanically believable ending.
    a.rect(107,12,3,16,P["edge"])
    a.rect(108,14,2,11,P["rail"])
    return a

def generate(path):
    path=Path(path)
    for state in PALETTES:
        render(state).png(path/("guide_"+state+".png"))
    return 2

if __name__=="__main__":
    root=Path(__file__).resolve().parents[1]/"src/main/resources/assets/zonezero_chainsaw/textures/gui"
    print("Original high-contrast guide-bar HUD textures:",generate(root))
