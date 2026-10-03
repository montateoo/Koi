# Ritaglia le tre carpe dalla foto delle tessere in acrilico: resta solo il pesce, su sfondo trasparente.
import sys, colorsys, math
from collections import deque
from PIL import Image, ImageFilter, ImageDraw
src=Image.open(r"C:\Users\matte\Downloads\pic9661875.webp").convert("RGB")
BOX={"s":(448,268,590,412),"m":(243,460,499,614),"l":(652,858,905,1088)}
OUT=sys.argv[1] if len(sys.argv)>1 else "img"
# Ogni carpa viene appoggiata sulla sua tessera: 1, 2 o 3 esagoni, per far capire la taglia a colpo d'occhio.
R3=3**.5
HEX={"s":[(0,0)],"m":[(0,0),(R3,0)],"l":[(R3/2,0),(0,1.5),(R3,1.5)]}
def tile(n,fish,R=64,ss=4):
    cs=HEX[n]; pad=6
    W=round((max(c[0] for c in cs)+R3)*R)+2*pad; H=round((max(c[1] for c in cs)+2)*R)+2*pad
    big=Image.new("RGBA",(W*ss,H*ss),(0,0,0,0)); d=ImageDraw.Draw(big)
    for cx,cy in cs:
        x=(cx*R+R3/2*R+pad)*ss; y=(cy*R+R+pad)*ss
        pts=[(x+R*ss*math.cos(math.radians(60*i-90)),y+R*ss*math.sin(math.radians(60*i-90))) for i in range(6)]
        d.polygon(pts,fill=(178,224,221,255),outline=(255,255,255,255),width=5*ss)
    base=big.resize((W,H),Image.LANCZOS)
    k=min(W*.86/fish.width,H*.86/fish.height); f=fish.resize((round(fish.width*k),round(fish.height*k)),Image.LANCZOS)
    base.alpha_composite(f,((W-f.width)//2,(H-f.height)//2))
    return base
VMIN={"s":0.25,"m":0.25,"l":0.58}   # sotto la carpa arancione la tessera riflette un marrone saturo: soglia piu' alta
sheet=Image.new("RGB",(1100,480),(252,247,234)); x0=10
for n,b in BOX.items():
    im=src.crop(b); im=im.resize((im.width*2,im.height*2),Image.LANCZOS); w,h=im.size; px=im.load()
    m=Image.new("L",(w,h)); mp=m.load()
    for y in range(h):
        for x in range(w):
            r,g,bl=px[x,y]; hh,s,v=colorsys.rgb_to_hsv(r/255,g/255,bl/255)
            mp[x,y]=255 if (s>0.30 and v>VMIN[n]) or v>0.76 else 0     # colori saturi oppure bianco: il pesce
    m=m.filter(ImageFilter.MinFilter(5)).filter(ImageFilter.MaxFilter(7)); mp=m.load()   # via i riflessi sottili dei bordi
    # tieni solo la macchia piu' grande (il pesce), scartando riflessi e graffi isolati
    lab=[0]*(w*h); best=(0,0); cur=0
    for y in range(h):
        for x in range(w):
            if mp[x,y] and not lab[y*w+x]:
                cur+=1; q=deque([(x,y)]); lab[y*w+x]=cur; size=0
                while q:
                    a,c=q.popleft(); size+=1
                    for a2,c2 in ((a+1,c),(a-1,c),(a,c+1),(a,c-1)):
                        if 0<=a2<w and 0<=c2<h and mp[a2,c2] and not lab[c2*w+a2]: lab[c2*w+a2]=cur; q.append((a2,c2))
                if size>best[0]: best=(size,cur)
    a=Image.frombytes("L",(w,h),bytes(255 if v==best[1] else 0 for v in lab)).filter(ImageFilter.MaxFilter(3)).filter(ImageFilter.GaussianBlur(1.3))
    out=im.convert("RGBA"); out.putalpha(a); out=out.crop(a.point(lambda v:255 if v>40 else 0).getbbox())
    out=tile(n,out)
    out.save(f"{OUT}/carp-{n}.png",optimize=True)
    sheet.paste(out,(x0,10),out); x0+=out.width+30
if len(sys.argv)>2: sheet.save(sys.argv[2])
