# Ritaglia le icone dal regolamento (pagine gia' renderizzate in rules/) e rende trasparente lo sfondo.
import os
from collections import deque
from PIL import Image, ImageFilter, ImageDraw
K="C:/Users/matte/Documents/Project/Koi/KoiCalculator"
os.makedirs(K+"/img",exist_ok=True)
pages={}
def page(n):
    if n not in pages: pages[n]=Image.open(f"{K}/rules/p{n:02d}.jpg").convert("RGB")
    return pages[n]
def cut(name,pg,box,erase=(),thr=205,keep_bg=False,z=2):
    im=page(pg).crop(box)
    im=im.resize((im.width*z,im.height*z),Image.LANCZOS)
    if keep_bg:
        im.save(f"{K}/img/{name}.jpg",quality=88); return
    w,h=im.size; px=im.load()
    for (x0,y0,x1,y1) in erase:
        ImageDraw.Draw(im).rectangle([(x0-box[0])*z,(y0-box[1])*z,(x1-box[0])*z,(y1-box[1])*z],fill=(253,247,220))
    # sfondo = pixel chiari color crema collegati al bordo
    isbg=lambda p: p[0]>226 and p[1]>214 and p[2]>150 and p[0]>=p[2]-4
    seen=bytearray(w*h); q=deque()
    for x in range(w):
        for y in (0,h-1): q.append((x,y))
    for y in range(h):
        for x in (0,w-1): q.append((x,y))
    while q:
        x,y=q.popleft()
        if x<0 or y<0 or x>=w or y>=h or seen[y*w+x] or not isbg(px[x,y]): continue
        seen[y*w+x]=1
        q.extend(((x+1,y),(x-1,y),(x,y+1),(x,y-1)))
    a=Image.frombytes("L",(w,h),bytes(0 if s else 255 for s in seen)).filter(ImageFilter.GaussianBlur(1.1))
    a=a.point(lambda v: 0 if v<110 else min(255,int((v-110)*255/100)))
    im=im.convert("RGBA"); im.putalpha(a)
    im=im.crop(a.getbbox())
    im.save(f"{K}/img/{name}.png",optimize=True)
# Le carpe (img/carp-*.png) vengono da una foto delle tessere, non dal PDF.
cut("ninfea",2,(322,748,412,840))
cut("lanterna",2,(752,730,852,848))
cut("moneta",3,(92,788,170,874))
cut("fortuna",3,(333,808,396,902))
cut("tile-rami",3,(78,150,216,412))
cut("tile-salice",3,(520,205,688,340))
cut("tile-ponticello",3,(935,212,1110,345))
cut("tile-shishi",3,(1252,202,1335,315))
cut("tile-tartaruga",3,(172,512,252,608))
cut("tile-cascata",3,(497,505,675,645))
cut("tile-statua",3,(932,512,1110,645))
cut("tile-viola",3,(1213,460,1265,512))
cut("tile-percorso",3,(1200,547,1283,602))
cut("tile-centenaria",3,(1168,628,1310,728),erase=[(1278,628,1310,668)])
# Le carte Pergamena (img/perg-*.jpg) non vengono dal PDF ma da una foto piu' definita delle carte;
# quella del Mercato e' ricomposta dalla pergamena da 3 PV con la miniatura della carta Mercato.

# Anche le carte Obiettivo (img/card-*.jpg) vengono da una foto delle carte, non dal PDF.

# foglio di controllo
fs=sorted(os.listdir(K+"/img")); sheet=Image.new("RGB",(1500,1500),(252,247,234)); x=y=10; rowh=0
for f in fs:
    im=Image.open(f"{K}/img/{f}").convert("RGBA")
    if x+im.width>1490: x=10; y+=rowh+10; rowh=0
    sheet.paste(im,(x,y),im); x+=im.width+12; rowh=max(rowh,im.height)
sheet.save(os.path.join(os.environ.get("TEMP","."),"koi_sheet.png")); print(len(fs), sum(os.path.getsize(f"{K}/img/{f}") for f in fs)//1024,"KB")
