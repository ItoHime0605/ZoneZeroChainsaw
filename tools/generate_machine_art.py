#!/usr/bin/env python3
"""Hand-authored original block-machine pixel materials, 64x64 RGBA.

Never samples or copies vanilla/Minecraft or other mods. No random pixel noise:
broad painted surfaces, ordered seams, machined metal edges and purposeful
details only. Art style is clean chunky post-apocalyptic industrial voxel.
"""
from pathlib import Path
import struct, zlib

SIZE=64
def rgba(s, a=255):
    s=s.lstrip("#")
    return (*bytes.fromhex(s),a)

class Canvas:
    def __init__(self,base):
        self.a=bytearray(bytes(rgba(base))*(SIZE*SIZE))
    def rect(self,x,y,w,h,col):
        v=bytes(rgba(col) if isinstance(col,str) else col)
        for yy in range(max(0,int(y)),min(SIZE,int(y+h))):
            for xx in range(max(0,int(x)),min(SIZE,int(x+w))):
                k=4*(yy*SIZE+xx);self.a[k:k+4]=v
        return self
    def line(self,x0,y0,x1,y1,col,width=1):
        steps=int(max(abs(x1-x0),abs(y1-y0))*2)+1
        for i in range(steps+1):
            t=i/max(1,steps)
            self.rect(round(x0+(x1-x0)*t),round(y0+(y1-y0)*t),width,width,col)
        return self
    def bezel(self,light,dark):
        self.rect(0,0,64,4,light).rect(0,0,4,64,light)
        self.rect(0,60,64,4,dark).rect(60,0,4,64,dark)
        return self
    def save(self,path):
        lines=b"".join(b"\0"+bytes(self.a[y*SIZE*4:(y+1)*SIZE*4]) for y in range(SIZE))
        def chunk(kind,payload):
            return (struct.pack(">I",len(payload))+kind+payload+
                    struct.pack(">I",zlib.crc32(kind+payload)&0xffffffff))
        path.parent.mkdir(parents=True,exist_ok=True)
        path.write_bytes(b"\x89PNG\r\n\x1a\n"+
            chunk(b"IHDR",struct.pack(">IIBBBBB",SIZE,SIZE,8,6,0,0,0))+
            chunk(b"IDAT",zlib.compress(lines,9))+chunk(b"IEND",b""))

def wood():
    c=Canvas("#684a35")
    # Four deliberate broad barrel staves, no grain speckles.
    for x,base in ((0,"#775138"),(16,"#805a3e"),(32,"#79533a"),(48,"#704c35")):
        c.rect(x+2,0,13,64,base)
        c.rect(x+3,0,2,64,"#926747")
        c.rect(x+13,0,2,64,"#583e2e")
        c.line(x+6,10,x+6,46,"#8b6040")
        c.rect(x+7,52,5,2,"#9b704c")
    c.rect(0,0,64,3,"#4e3b2f").rect(0,60,64,4,"#49392d")
    return c

def endgrain():
    c=Canvas("#6f4b34").bezel("#aa7a54","#3a3029")
    c.rect(7,7,50,50,"#80563a").rect(12,12,40,40,"#906346")
    for d in (17,24,31):
        c.rect(d,d,64-d*2,2,"#b58356")
        c.rect(d,62-d,64-d*2,2,"#5c4230")
        c.rect(d,d,2,64-d*2,"#ab754e")
        c.rect(62-d,d,2,64-d*2,"#59402f")
    c.rect(29,27,6,10,"#4c362a")
    return c

def steel():
    c=Canvas("#505b60")
    c.rect(0,0,64,8,"#646e72").rect(0,8,64,31,"#596368")
    c.rect(0,39,64,17,"#4c575d").rect(0,56,64,8,"#343e44")
    c.rect(0,0,64,3,"#9aa6a6").rect(0,3,3,54,"#808b8b")
    c.rect(60,3,4,57,"#353e44").rect(0,60,64,4,"#28343a")
    c.rect(8,11,48,2,"#737d80")
    c.rect(8,43,48,2,"#3d494e")
    return c

def dark():
    c=Canvas("#252e34")
    c.rect(0,0,64,6,"#48535a").rect(0,6,6,54,"#3b444a")
    c.rect(6,7,52,44,"#313a40")
    c.rect(6,51,52,7,"#20272e")
    c.rect(60,0,4,64,"#151e24").rect(0,60,64,4,"#171f26")
    c.rect(13,15,38,2,"#4a555a")
    return c

def brass():
    c=Canvas("#a47c46").bezel("#e1bf79","#5a4935")
    c.rect(7,7,50,50,"#b48b51")
    c.rect(9,9,46,4,"#d8b273")
    c.rect(9,47,46,7,"#906d3e")
    c.rect(15,20,34,4,"#dfbd7e")
    c.rect(15,24,34,18,"#a9824b")
    c.rect(16,41,32,3,"#785d39")
    return c

def enamel_red():
    c=Canvas("#a12e2d").bezel("#d85545","#5c2026")
    c.rect(7,7,50,44,"#b83b31")
    c.rect(9,9,46,5,"#d3503d")
    c.rect(9,42,46,8,"#8d282a")
    c.rect(14,18,35,2,"#df7054")
    c.rect(16,23,2,15,"#db5640")
    return c

def iron():
    c=Canvas("#9da8a7").bezel("#e0e5dc","#4e6065")
    c.rect(8,8,48,48,"#aeb7b4")
    c.rect(8,8,48,4,"#cdd7cf")
    c.rect(8,46,48,10,"#8b9999")
    c.rect(14,18,36,2,"#d8dfd8")
    c.rect(14,28,36,3,"#96a3a2")
    return c

def pipe():
    c=Canvas("#66747a")
    for x in range(0,64,16):
        c.rect(x,0,5,64,"#3a474d")
        c.rect(x+5,0,3,64,"#b4baba")
        c.rect(x+8,0,5,64,"#73838a")
        c.rect(x+13,0,3,64,"#303c43")
    c.rect(0,12,64,3,"#8c9a9d").rect(0,44,64,3,"#46565c")
    return c

def bolts():
    c=Canvas("#3d4a4f")
    for x,y in ((12,12),(36,12),(12,36),(36,36)):
        c.rect(x,y,15,15,"#566267")
        c.rect(x+2,y+2,11,11,"#cbd2ce")
        c.rect(x+4,y+4,7,7,"#6c777c")
        c.rect(x+5,y+5,5,2,"#dce0d7")
        c.rect(x+6,y+9,4,2,"#354047")
    return c

def glass(active):
    c=Canvas("#304447" if not active else "#395f42")
    c.bezel("#8daab0","#18262b")
    c.rect(7,7,50,50,"#293a41" if not active else "#526839")
    c.rect(10,10,44,44,"#41575c" if not active else "#6c7f3b")
    c.rect(10,10,44,7,"#60777c" if not active else "#91a45a")
    c.rect(10,44,44,10,"#27383d" if not active else "#3b5735")
    c.rect(15,10,6,35,"#9ac4c1" if not active else "#afcf77")
    c.rect(21,11,3,19,"#658c90" if not active else "#849e53")
    if active:
        for x,y,r in ((28,37,5),(39,23,4),(44,41,3),(30,18,3)):
            c.rect(x,y,r,r,"#b4d883")
            c.rect(x+1,y+1,max(1,r-2),max(1,r-2),"#7ca64d")
        c.rect(12,51,40,3,"#cadc86")
    else:
        c.rect(13,48,38,2,"#78949a")
    return c

def gauge(active):
    c=Canvas("#313c43").bezel("#aab7b7","#1b262d")
    c.rect(7,7,50,50,"#c7d3ce")
    c.rect(11,11,42,42,"#e6e6d8")
    c.rect(11,44,42,9,"#9aa8a7")
    c.rect(30,16,4,8,"#526670")
    c.rect(17,33,8,4,"#62747a")
    c.rect(41,33,8,4,"#62747a")
    c.line(32,37,45 if active else 24,22 if active else 30,
           "#cb4b3f" if active else "#3b5057",3)
    c.rect(29,34,7,7,"#4c5d62")
    c.rect(30,35,5,4,"#c6d0c8")
    return c

def lamp(active):
    c=Canvas("#263038").bezel("#6c777c","#131c22")
    c.rect(10,10,44,44,"#d14c3b" if active else "#586b66")
    c.rect(13,13,38,9,"#f29b68" if active else "#a1b7ad")
    c.rect(13,24,38,25,"#bd382c" if active else "#3c5857")
    c.rect(17,17,9,16,"#ffe0ae" if active else "#92a8a0")
    return c

def valve():
    c=Canvas("#303a40")
    c.rect(7,7,50,50,"#7b292b")
    c.rect(12,12,40,40,"#bc3e37")
    c.rect(27,9,10,46,"#ed6d51")
    c.rect(9,27,46,10,"#d95546")
    c.rect(24,24,16,16,"#39464b")
    c.rect(27,27,10,10,"#9fa9a8")
    c.rect(30,30,4,4,"#495b62")
    return c

def liquid(active):
    c=Canvas("#35554c" if not active else "#547739")
    c.rect(0,0,64,9,"#6e8681" if not active else "#a2bd6d")
    c.rect(0,9,64,35,"#526e61" if not active else "#819a4b")
    c.rect(0,44,64,20,"#283e3b" if not active else "#3c5632")
    c.rect(9,12,5,28,"#89a99a" if not active else "#b9d27b")
    if active:
        for x,y in ((17,24),(28,34),(39,19),(48,44)):
            c.rect(x,y,6,6,"#cbe08f")
            c.rect(x+2,y+2,2,2,"#759648")
    return c

def station_top():
    c=Canvas("#4e5c62").bezel("#9eadb2","#233139")
    c.rect(7,7,50,50,"#69777b")
    c.rect(11,11,42,42,"#74858a")
    c.rect(12,13,40,3,"#b5c1c1")
    c.rect(12,48,40,3,"#3f4c52")
    for x in (17,32,47):
        c.rect(x,12,2,40,"#536268")
    return c

def station_screen():
    c=Canvas("#17292f").bezel("#8298a1","#0b171d")
    c.rect(7,7,50,50,"#213b45")
    c.rect(12,13,40,6,"#62929a")
    c.rect(12,23,29,3,"#80abb0")
    c.rect(12,30,36,3,"#3e6972")
    c.rect(12,38,24,3,"#4d8990")
    c.rect(12,48,40,3,"#294e5a")
    return c

def station_tools():
    c=Canvas("#526065").bezel("#94a0a5","#303b42")
    c.line(15,51,46,15,"#252e34",9)
    c.line(15,49,46,14,"#c6d2d0",4)
    c.rect(38,7,13,8,"#3a474d")
    c.rect(43,12,11,8,"#a2b3b1")
    c.rect(8,45,15,11,"#a8b8b7")
    c.rect(13,50,6,5,"#37434a")
    return c

def station_hazard():
    c=Canvas("#4b5253")
    for x in range(-64,128,24):
        c.line(x,64,x+52,0,"#dda847",13)
        c.line(x+14,64,x+66,0,"#313c44",12)
    return c

def station_vent():
    c=Canvas("#58676c").bezel("#b3bfbe","#26353d")
    c.rect(8,8,48,48,"#8c9c9f")
    for y in (13,24,35,46):
        c.rect(12,y,40,7,"#233139")
        c.rect(12,y,40,2,"#596b72")
        c.rect(14,y+6,36,2,"#c0cccc")
    return c

def station_rubber():
    c=Canvas("#34393b").bezel("#61696a","#20272b")
    for x in range(7,60,11):
        c.rect(x,9,4,46,"#4d5356")
        c.rect(x+4,9,2,46,"#232c30")
    return c

def make(root):
    root=Path(root)
    images={
        "ferment_v2_wood":wood(),
        "ferment_v2_endgrain":endgrain(),
        "ferment_v2_steel":steel(),
        "ferment_v2_dark":dark(),
        "ferment_v2_brass":brass(),
        "ferment_v2_red":enamel_red(),
        "ferment_v2_iron":iron(),
        "ferment_v2_pipe":pipe(),
        "ferment_v2_bolt":bolts(),
        "ferment_v2_glass_idle":glass(False),
        "ferment_v2_glass_active":glass(True),
        "ferment_v2_gauge_idle":gauge(False),
        "ferment_v2_gauge_active":gauge(True),
        "ferment_v2_lamp_idle":lamp(False),
        "ferment_v2_lamp_active":lamp(True),
        "ferment_v2_valve":valve(),
        "ferment_v2_liquid_idle":liquid(False),
        "ferment_v2_liquid_active":liquid(True),
        "station_v2_steel":steel(),
        "station_v2_red":enamel_red(),
        "station_v2_top":station_top(),
        "station_v2_screen":station_screen(),
        "station_v2_tool":station_tools(),
        "station_v2_hazard":station_hazard(),
        "station_v2_vent":station_vent(),
        "station_v2_rubber":station_rubber(),
        "station_v2_bolt":bolts(),
        "station_v2_dark":dark(),
        "station_v2_lamp":lamp(True)
    }
    for name,img in images.items():
        img.save(root/(name+".png"))
    return len(images)

if __name__ == "__main__":
    folder=Path(__file__).resolve().parents[1]/"src/main/resources/assets/zonezero_chainsaw/textures/block"
    print("Original clean machine textures:",make(folder))
