#!/usr/bin/env python3
"""Original 64x64 authored pixel artwork for ZoneZeroChainsaw.
No vanilla/third-party textures are sampled. Deterministic source art creates
16 painted cast housings and eight independent mechanical materials.
"""
from pathlib import Path
import struct
import zlib

S = 64
DYES = {
    'white':'#dee1db','orange':'#e76b21','magenta':'#b85ab4',
    'light_blue':'#7cbedc','yellow':'#dfbf35','lime':'#91c346',
    'pink':'#e59fb5','gray':'#636c73','light_gray':'#a3aaad',
    'cyan':'#3f92a2','purple':'#7c5cad','blue':'#4668b8',
    'brown':'#855c41','green':'#55854e','red':'#bb4338',
    'black':'#353c43'
}

def rgb(text):
    return tuple(bytes.fromhex(text.removeprefix('#'))) + (255,)

def tint(color, power):
    return tuple(max(0,min(255,round(x*power))) for x in color[:3]) + (255,)

def noise(x,y,seed=0):
    v=(x*73856093)^(y*19349663)^(seed*83492791)
    v=(v^(v>>13))*1274126177
    return (v^(v>>16))&0xffffffff

class Canvas:
    def __init__(self,color):
        self.pixels=bytearray(bytes(color)*(S*S))
    def pixel(self,x,y,color):
        x,y=int(x),int(y)
        if 0<=x<S and 0<=y<S:
            self.pixels[(y*S+x)*4:(y*S+x+1)*4]=bytes(color)
    def rect(self,x,y,w,h,color):
        for yy in range(int(y),int(y+h)):
            for xx in range(int(x),int(x+w)):
                self.pixel(xx,yy,color)
    def line(self,x0,y0,x1,y1,color,width=1):
        n=max(abs(x1-x0),abs(y1-y0))*2
        for i in range(int(n)+1):
            x=round(x0+(x1-x0)*i/max(1,n))
            y=round(y0+(y1-y0)*i/max(1,n))
            self.rect(x,y,width,width,color)
    def write(self,path):
        raw=b''.join(b'\x00'+bytes(self.pixels[y*S*4:(y+1)*S*4]) for y in range(S))
        def chunk(kind,blob):
            return (struct.pack('>I',len(blob))+kind+blob+
                    struct.pack('>I',zlib.crc32(kind+blob)&0xffffffff))
        path.parent.mkdir(parents=True,exist_ok=True)
        path.write_bytes(
            b'\x89PNG\r\n\x1a\n'+
            chunk(b'IHDR',struct.pack('>IIBBBBB',S,S,8,6,0,0,0))+
            chunk(b'IDAT',zlib.compress(raw,9))+
            chunk(b'IEND',b''))

def scuffs(c,light,dark,count=35,seed=13):
    for i in range(count):
        x=5+noise(i,91,seed)%54
        y=5+noise(i,29,seed)%54
        c.rect(x,y,2 if i%11==0 else 1,1,light if i%4==0 else dark)

def housing(color):
    a=rgb(color);p=Canvas(tint(a,.77))
    for y in range(0,S,4):
        for x in range(0,S,4):
            p.rect(x,y,4,4,tint(a,(.92,.99,1.03,.88,1.06)[noise(x,y,14)%5]))
    p.rect(0,0,64,3,tint(a,1.19));p.rect(0,3,3,58,tint(a,1.13))
    p.rect(61,0,3,64,tint(a,.51));p.rect(0,61,64,3,tint(a,.57))
    p.rect(6,7,52,48,tint(a,.74));p.rect(8,8,48,43,tint(a,1.01))
    p.rect(8,8,48,3,tint(a,1.20));p.rect(8,47,48,4,tint(a,.63))
    p.rect(11,15,39,27,tint(a,.69));p.rect(13,17,35,23,tint(a,.97))
    p.rect(14,18,33,2,tint(a,1.25));p.rect(13,38,35,2,tint(a,.55))
    for i in range(3):
        x=17+i*10
        p.rect(x,22,6,3,tint(a,.61))
        p.rect(x+1,22,4,1,tint(a,1.22))
        p.rect(x+2,26,2,8,tint(a,.82))
    p.rect(11,54,42,2,tint(a,.48));p.rect(12,56,40,1,tint(a,1.18))
    for x,y in ((6,6),(54,6),(6,55),(54,55)):
        p.rect(x-1,y-1,5,5,rgb('#41464a'))
        p.rect(x,y,3,3,rgb('#bec0b9'))
        p.rect(x+1,y+1,1,1,rgb('#585c5b'))
    scuffs(p,rgb('#d7c7a8'),tint(a,.59),38)
    return p

def guide():
    p=Canvas(rgb('#b4b3ad'))
    palette=['#bec0ba','#adaeaa','#d0cec8','#b4b4b1']
    for y in range(0,64,4):
        for x in range(0,64,4):
            p.rect(x,y,4,4,rgb(palette[noise(x,y,22)%4]))
    p.rect(0,0,64,4,rgb('#454d54'));p.rect(0,60,64,4,rgb('#454a50'))
    p.rect(0,4,3,56,rgb('#6f787e'));p.rect(61,4,3,56,rgb('#7a7e7e'))
    p.rect(5,9,54,46,rgb('#bbbcb8'));p.rect(5,9,54,2,rgb('#e7e7e2'))
    p.rect(6,52,53,3,rgb('#7d868a'))
    for i in range(17):
        x=5+noise(i,1,30)%50;y=12+noise(i,3,39)%40
        p.rect(x,y,5+noise(i,2,1)%6,1,rgb('#a6a7a4') if i%3 else rgb('#d7d6ce'))
    # Dark dried blood on the metal guide plate, NOT on chain teeth.
    p.line(12,42,25,21,rgb('#802d29'),4)
    p.line(17,44,29,30,rgb('#a64032'),2)
    p.line(28,45,44,24,rgb('#752622'),3)
    p.line(35,43,48,30,rgb('#9c382a'),3)
    for x,y,w,h in ((12,35,4,2),(17,39,3,3),(28,25,6,2),(35,34,2,4),
                    (43,26,4,2),(52,37,3,4),(39,47,2,3),
                    (56,29,2,1),(9,27,2,1),(49,21,2,1),(26,48,4,1)):
        p.rect(x,y,w,h,rgb('#943529') if (x+y)%2 else rgb('#68332d'))
    for i in range(18):
        x=9+noise(i,7,44)%48;y=12+noise(i,5,81)%38
        if (x*2+y)%3==0:p.rect(x,y,1+(i%5==0),1,rgb('#7d302b'))
    return p

def chain():
    p=Canvas(rgb('#23282d'))
    for y in range(0,64,8):
        p.rect(0,y,64,2,rgb('#596169'))
        p.rect(0,y+2,64,1,rgb('#30383d'))
        p.rect(0,y+7,64,1,rgb('#14191e'))
    for x in range(0,64,8):
        p.rect(x,0,2,64,rgb('#333b42'))
        for y in range(3,64,16):
            p.rect(x+2,y,4,4,rgb('#858d8d'))
            p.rect(x+3,y+1,2,2,rgb('#d5d0c5'))
            p.rect(x+3,y+2,2,1,rgb('#51565b'))
    return p

def metal():
    p=Canvas(rgb('#999fa1'))
    palette=['#aeb3b5','#898f95','#c1c2bd','#92989a']
    for y in range(0,64,4):
        for x in range(0,64,4):
            p.rect(x,y,4,4,rgb(palette[noise(x,y,71)%4]))
    p.rect(0,0,64,3,rgb('#e1dbce'));p.rect(0,61,64,3,rgb('#4b535a'))
    p.rect(0,3,3,58,rgb('#cdd0cb'));p.rect(61,3,3,58,rgb('#737d83'))
    for i in range(11):
        x=7+noise(i,2,9)%45;y=8+noise(i,3,12)%50
        p.line(x,y,x+8,y-1,rgb('#b7babc') if i%2 else rgb('#777e83'))
    scuffs(p,rgb('#e1e0d9'),rgb('#555e60'),22,39)
    return p

def rubber():
    p=Canvas(rgb('#24292d'))
    for y in range(0,64,4):p.rect(0,y,64,3,rgb('#30343a'))
    for x in range(-16,80,9):
        p.line(x,0,x-15,63,rgb('#101519'),2)
        p.line(x+3,0,x-12,63,rgb('#474c4e'))
    p.rect(0,0,64,3,rgb('#505359'))
    p.rect(0,61,64,3,rgb('#151a1e'))
    return p

def vent():
    p=Canvas(rgb('#92989a'))
    p.rect(3,3,58,58,rgb('#525d64'))
    p.rect(6,6,52,52,rgb('#afb4b2'))
    p.rect(7,7,49,2,rgb('#e7e5dc'))
    p.rect(7,54,49,4,rgb('#646f75'))
    for y in (13,25,37):
        p.rect(12,y,40,8,rgb('#252d35'))
        p.rect(12,y,40,2,rgb('#454c51'))
        p.rect(14,y+7,36,2,rgb('#e1dbd0'))
        p.rect(17,y+4,31,1,rgb('#11191d'))
    for x,y in ((6,6),(52,6),(6,52),(52,52)):
        p.rect(x,y,5,5,rgb('#444b50'))
        p.rect(x+1,y+1,3,3,rgb('#d5d4ca'))
        p.rect(x+2,y+2,1,1,rgb('#50545b'))
    return p

def cap():
    p=Canvas(rgb('#90999c'))
    for i in range(5):
        d=i*4
        p.rect(d,d,64-2*d,3,rgb('#d4d5d0'))
        p.rect(d,61-d,64-2*d,3,rgb('#596167'))
        p.rect(d,d,3,64-2*d,rgb('#bbc1bc'))
        p.rect(61-d,d,3,64-2*d,rgb('#5d666b'))
    p.rect(24,22,16,20,rgb('#384047'))
    p.rect(27,24,10,16,rgb('#929a9d'))
    p.rect(29,27,6,9,rgb('#495258'))
    return p

def frame():
    p=Canvas(rgb('#292c31'))
    palette=['#252a2e','#30353b','#373c42','#282c31']
    for y in range(0,64,4):
        for x in range(0,64,4):
            p.rect(x,y,4,4,rgb(palette[noise(x,y,11)%4]))
    p.rect(0,0,64,4,rgb('#686f72'))
    p.rect(0,60,64,4,rgb('#161a20'))
    for y in range(9,57,13):
        p.rect(6,y,50,2,rgb('#41474d'))
        p.rect(6,y+2,50,1,rgb('#171d24'))
    return p

def bolt():
    p=Canvas(rgb('#4a5056'))
    p.rect(5,5,54,54,rgb('#8b9497'))
    p.rect(9,9,46,46,rgb('#bbbdb8'))
    p.rect(12,12,40,3,rgb('#e8e4db'))
    p.rect(12,49,40,3,rgb('#606a71'))
    p.rect(23,15,18,34,rgb('#5e666c'))
    p.rect(16,23,32,18,rgb('#586166'))
    p.rect(24,24,16,16,rgb('#828c90'))
    return p

def generate(root):
    root=Path(root)
    artwork={
        'guide_blood_v3':guide(),
        'chain_graphite_v3':chain(),
        'metal_cast_v3':metal(),
        'rubber_grip_v3':rubber(),
        'vent_cast_v3':vent(),
        'cap_steel_v3':cap(),
        'frame_black_v3':frame(),
        'bolt_steel_v3':bolt()
    }
    artwork.update({'housing_'+name:housing(color)
                    for name,color in DYES.items()})
    for name,image in artwork.items():
        image.write(root/(name+'.png'))
    return len(artwork)

if __name__=='__main__':
    out=Path(__file__).resolve().parents[1] / 'src/main/resources/assets/zonezero_chainsaw/textures/item'
    print('Original chainsaw textures:',generate(out))
