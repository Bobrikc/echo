"""Генерирует скины NPC (64x64) и иконку раковины эха (16x16). Запуск: python3 tools/gen_textures.py"""
from PIL import Image
import os, random

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src/main/resources/assets/echocity/textures')
C = lambda r, g, b, a=255: (r, g, b, a)

def blend(a, b, t):
    return tuple(int(a[i] * (1 - t) + b[i] * t) for i in range(3)) + (255,)

def shade(c, k):
    return (max(0, min(255, int(c[0] * k))), max(0, min(255, int(c[1] * k))), max(0, min(255, int(c[2] * k))), c[3] if len(c) > 3 else 255)

class Skin:
    def __init__(self, seed):
        self.im = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
        self.rnd = random.Random(seed)

    def px(self, x, y, c):
        if 0 <= x < 64 and 0 <= y < 64: self.im.putpixel((x, y), c)

    def rect(self, x, y, w, h, c, noise=0.0):
        for i in range(x, x + w):
            for j in range(y, y + h):
                k = 1.0 + (self.rnd.uniform(-noise, noise) if noise else 0)
                self.px(i, j, shade(c, k))

    def tone(self, x, y, w, h, k):
        for i in range(x, x + w):
            for j in range(y, y + h):
                if 0 <= i < 64 and 0 <= j < 64:
                    r, g, b, a = self.im.getpixel((i, j))
                    if a:
                        self.im.putpixel((i, j), (max(0, min(255, int(r * k))), max(0, min(255, int(g * k))), max(0, min(255, int(b * k))), a))

    def vgrad(self, x, y, w, h, c, top=1.08, bot=0.88, noise=0.04):
        for j in range(h):
            k = top + (bot - top) * j / max(1, h - 1)
            for i in range(w):
                self.px(x + i, y + j, shade(c, k + self.rnd.uniform(-noise, noise)))

def draw_eyes(s, eyes, hc):
    # аниме-глаза 2x2 на каждый глаз: тёмный верх с ресницей, цветная радужка, белок и блик
    lash = shade(hc if sum(hc[:3]) < 330 else (40, 36, 44, 255), 0.55)
    dark = shade(eyes, 0.55); light = shade(eyes, 1.35)
    white = C(246, 246, 252)
    # левый глаз (x 9..10)
    s.px(9, 12, lash); s.px(10, 12, dark)
    s.px(9, 13, white); s.px(10, 13, eyes)
    # правый глаз (x 13..14)
    s.px(13, 12, dark); s.px(14, 12, lash)
    s.px(13, 13, eyes); s.px(14, 13, white)
    s.px(10, 12, blend(dark, light, 0.25)); s.px(13, 12, blend(dark, light, 0.25))
    s.px(10, 13, blend(eyes, C(255, 255, 255), 0.35)); s.px(13, 13, blend(eyes, C(255, 255, 255), 0.35))   # блик
    s.px(8, 12, shade(lash, 1.3)); s.px(15, 12, shade(lash, 1.3))                                          # ресницы по краям

def make(name, skin, hair, eyes, shirt, pants, accent, boots=None, long_hair=False, style='none', seed=1,
         mask=False, hood=None, helmet=None, beard=None, scar=False, apron=None, shawl=None, vest=None,
         bandana=None, bald=False, coat_trim=None, bun=False, ribbon=None, necklace=None):
    s = Skin(seed)
    boots = boots or shade(pants, 0.6)
    # ---------- голова ----------
    s.vgrad(0, 8, 32, 8, skin, 1.05, 0.92)              # боковые и лицо
    s.rect(8, 0, 16, 8, skin, 0.03)                     # верх/низ
    hc = skin if bald else hair
    # макушка и волосы
    s.rect(8, 0, 8, 8, hc, 0.07)
    s.rect(0, 8, 8, 3, hc, 0.07); s.rect(16, 8, 8, 3, hc, 0.07)   # виски
    s.rect(8, 8, 8, 2, hc, 0.07)                                    # чёлка
    s.rect(24, 8, 8, 8, hc, 0.08)                                   # затылок
    if bald:                                                        # щетина
        s.rect(8, 8, 8, 2, shade(skin, 0.95)); s.rect(24, 8, 8, 8, shade(skin, 0.95))
    if long_hair:
        s.rect(0, 8, 8, 8, hc, 0.07); s.rect(16, 8, 8, 8, hc, 0.07)
        s.rect(8, 8, 1, 7, hc, 0.06); s.rect(15, 8, 1, 7, hc, 0.06)
        s.px(9, 10, hc); s.px(14, 10, hc)
    if style == 'fringe':
        for x in (9, 11, 12, 14): s.px(x, 10, hc)
    if style == 'messy':
        for x, y in ((8, 10), (10, 10), (13, 10), (15, 10), (9, 11)): s.px(x, y, hc)
    # лицо (передняя грань 8..15 x 8..15)
    white = C(245, 245, 250)
    s.px(9, 11, shade(hc, 0.85)); s.px(10, 11, shade(hc, 0.85)); s.px(13, 11, shade(hc, 0.85)); s.px(14, 11, shade(hc, 0.85))  # брови
    s.px(11, 13, shade(skin, 0.84)); s.px(12, 13, shade(skin, 0.84))                                                    # нос
    s.px(10, 14, shade(skin, 0.88)); s.px(13, 14, shade(skin, 0.88))
    s.px(11, 14, C(196, 108, 104)); s.px(12, 14, C(196, 108, 104))                                                      # рот
    s.px(9, 13, shade(skin, 1.07)); s.px(14, 13, shade(skin, 1.07))
    if beard:
        s.rect(9, 14, 6, 2, beard, 0.05); s.px(11, 14, shade(beard, 0.8)); s.px(12, 14, shade(beard, 0.8))
    if scar:
        for i in range(3): s.px(13 + (i // 2), 10 + i + 1, C(200, 120, 110))
    if mask:
        s.rect(8, 13, 8, 3, mask, 0.04)
    if bandana:
        s.rect(8, 8, 8, 2, bandana, 0.05); s.rect(0, 8, 8, 2, bandana, 0.05); s.rect(16, 8, 8, 2, bandana, 0.05); s.rect(24, 8, 8, 2, bandana, 0.05)
    # ---------- тело ----------
    s.vgrad(16, 16, 24, 16, shirt, 1.08, 0.86)
    s.rect(16, 16, 24, 4, shade(shirt, 1.12), 0.02)    # плечи
    # руки
    for (ax, ay) in ((40, 16), (32, 48)):
        s.vgrad(ax, ay, 16, 12, shirt, 1.06, 0.88)
        s.rect(ax, ay + 12, 16, 4, skin, 0.03)          # кисти
    # ноги
    for (lx, ly) in ((0, 16), (16, 48)):
        s.vgrad(lx, ly, 16, 11, pants, 1.06, 0.86)
        s.rect(lx, ly + 11, 16, 5, boots, 0.05)         # сапоги
    # пояс
    s.rect(16, 30, 24, 2, accent, 0.03)
    s.px(23, 30, shade(accent, 1.3)); s.px(24, 30, shade(accent, 1.3)); s.px(23, 31, shade(accent, 1.3)); s.px(24, 31, shade(accent, 1.3))  # пряжка
    # воротник
    s.rect(20, 20, 8, 1, shade(shirt, 1.25)); s.px(23, 21, skin); s.px(24, 21, skin); s.px(23, 20, skin); s.px(24, 20, skin)
    if vest:
        s.rect(20, 21, 2, 9, vest, 0.04); s.rect(26, 21, 2, 9, vest, 0.04)
        s.rect(32, 20, 8, 10, vest, 0.04)
    if apron:
        s.rect(21, 23, 6, 8, apron, 0.02); s.rect(21, 22, 6, 1, shade(apron, 0.9))
        for x in (21, 26): s.px(x, 24, shade(apron, 0.8))
    if shawl:
        s.rect(20, 20, 8, 3, shawl, 0.05); s.rect(20, 23, 2, 3, shawl, 0.05); s.rect(26, 23, 2, 3, shawl, 0.05)
        s.rect(44, 16, 4, 4, shawl, 0.05); s.rect(36, 52, 4, 4, shawl, 0.05)
        for x in range(20, 28, 2): s.px(x, 23, shade(shawl, 0.8))
    if coat_trim:
        s.rect(20, 20, 8, 1, coat_trim); s.rect(23, 21, 2, 10, coat_trim)          # центральная планка
        for y in (22, 24, 26, 28): s.px(23, y, C(240, 200, 70)); s.px(24, y, C(240, 200, 70))  # золотые пуговицы
        s.rect(44, 26, 4, 2, coat_trim); s.rect(36, 62, 4, 2, coat_trim)            # манжеты
    if not bald:
        # внешний слой волос: даёт объём, как у настоящих скинов
        s.rect(40, 0, 8, 8, hc, 0.06); s.rect(48, 0, 8, 8, shade(hc, 0.9), 0.06)         # макушка
        s.rect(40, 8, 8, 2, hc, 0.06); s.px(40, 10, hc); s.px(47, 10, hc)                  # чёлка и виски
        s.rect(32, 8, 8, 3, hc, 0.06); s.rect(48, 8, 8, 3, hc, 0.06)                       # бока
        s.rect(56, 8, 8, 8, shade(hc, 0.92), 0.06)                                         # затылок
        if long_hair:
            s.rect(32, 8, 8, 8, hc, 0.06); s.rect(48, 8, 8, 8, hc, 0.06)
            s.px(40, 11, hc); s.px(47, 11, hc); s.px(40, 12, hc); s.px(47, 12, hc)         # локоны у лица
        for (x, y) in ((42, 8), (44, 9), (46, 8)): s.tone(x, y, 1, 1, 1.2)                # блики на чёлке
    if ribbon:
        s.rect(46, 9, 2, 2, ribbon); s.px(47, 11, shade(ribbon, 0.8)); s.px(48, 10, ribbon)
    if necklace:
        for k, (x, y) in enumerate(((21, 21), (22, 22), (23, 23), (24, 23), (25, 22), (26, 21))): s.px(x, y, necklace)
        s.px(23, 24, shade(necklace, 1.3)); s.px(24, 24, shade(necklace, 1.3))
    if helmet:
        s.rect(32, 0, 32, 8, helmet, 0.04)                                              # шлем (слой шапки)
        s.rect(32, 8, 32, 3, helmet, 0.04)
        s.rect(40, 11, 8, 1, shade(helmet, 0.8))
        s.px(43, 12, helmet); s.px(44, 12, helmet)
    if hood:
        s.rect(32, 0, 32, 8, hood, 0.03); s.rect(32, 8, 32, 8, hood, 0.03)
        s.rect(40, 12, 8, 4, C(0, 0, 0, 0)); s.rect(40, 11, 8, 1, shade(hood, 0.7))    # прорезь лица
        s.px(41, 12, C(0, 0, 0, 0))
    if bun:
        s.rect(32, 0, 8, 8, shade(hair, 1.0), 0.06)
    # доспех Гарта поверх тела
    if helmet:
        s.rect(20, 20, 8, 8, shade(helmet, 1.0), 0.04)
        s.rect(20, 20, 8, 1, C(220, 190, 70)); s.rect(20, 27, 8, 1, C(220, 190, 70))
        s.rect(23, 20, 2, 8, shade(helmet, 1.2))
        s.rect(44, 16, 4, 3, shade(helmet, 1.1)); s.rect(36, 52, 4, 3, shade(helmet, 1.1))   # наплечники
    # --- объём: боковые грани темнее, как в обычных скинах ---
    s.tone(16, 20, 4, 12, 0.88); s.tone(28, 20, 4, 12, 0.88); s.tone(32, 20, 8, 12, 0.94)     # тело
    s.tone(40, 20, 4, 12, 0.90); s.tone(48, 20, 4, 12, 0.84); s.tone(52, 20, 4, 12, 0.94)      # правая рука
    s.tone(32, 52, 4, 12, 0.90); s.tone(40, 52, 4, 12, 0.84); s.tone(44, 52, 4, 12, 0.94)      # левая рука
    s.tone(0, 20, 4, 12, 0.92); s.tone(8, 20, 4, 12, 0.85); s.tone(12, 20, 4, 12, 0.94)        # правая нога
    s.tone(16, 52, 4, 12, 0.92); s.tone(24, 52, 4, 12, 0.85); s.tone(28, 52, 4, 12, 0.94)      # левая нога
    s.tone(0, 11, 8, 5, 0.93); s.tone(16, 11, 8, 5, 0.90)                                      # виски и уши
    # --- мелкие детали лица и волос ---
    for (x, y) in ((10, 1), (12, 2), (9, 3), (13, 4), (11, 5), (14, 6), (9, 7)):
        s.tone(x, y, 1, 1, 1.22)                                                              # блики на макушке
    for (x, y) in ((26, 10), (28, 12), (25, 14), (29, 9)):
        s.tone(x, y, 1, 1, 0.82)                                                              # пряди на затылке
    draw_eyes(s, eyes, hc)
    s.tone(20, 24, 1, 6, 0.93); s.tone(26, 24, 1, 6, 0.93)                                    # складки одежды
    s.tone(5, 22, 2, 2, 0.88); s.tone(21, 54, 2, 2, 0.88)                                     # заплатки на коленях
    # --- доработка форм ---
    for x in range(8, 16): s.tone(x, 10, 1, 1, 0.9)                                           # тень от чёлки на лбу
    for x in range(8, 16): s.tone(x, 15, 1, 1, 0.86)                                          # тень под подбородком
    s.tone(8, 8, 1, 8, 0.95); s.tone(15, 8, 1, 8, 0.95)                                       # скулы
    s.px(9, 14, blend(s.im.getpixel((9, 14)), (255, 150, 150, 255), 0.18)); s.px(14, 14, blend(s.im.getpixel((14, 14)), (255, 150, 150, 255), 0.18))
    s.tone(20, 21, 8, 1, 0.82)                                                                # тень от шеи
    for y in range(22, 30): s.tone(20, y, 1, 1, 0.94); s.tone(27, y, 1, 1, 0.94)              # боковые швы
    s.rect(20, 29, 8, 1, shade(shirt, 0.78))                                                  # подол рубахи
    for lx, ly in ((4, 20), (20, 52)):
        s.rect(lx, ly + 10, 4, 1, shade(pants, 0.72)); s.rect(lx, ly + 11, 4, 1, shade(boots, 1.25))   # манжет и носок сапога
        s.rect(lx, ly + 11, 4, 1, shade(boots, 1.25)); s.rect(lx, ly + 11 + 4, 4, 1, shade(boots, 0.6))  # подошва
    for ax, ay in ((44, 20), (36, 52)):
        s.rect(ax, ay + 8, 4, 1, shade(shirt, 0.72)); s.tone(ax, ay + 11, 4, 1, 0.9)                    # манжета рукава
    s.im.save(os.path.join(OUT, 'entity', name + '.png'))
    return s.im

SK1, SK2, SK3 = C(240, 200, 170), C(205, 155, 120), C(150, 105, 80)
make('tavi', SK1, C(205, 215, 240), C(60, 140, 225), C(70, 110, 160), C(55, 55, 80), C(120, 80, 50),
     long_hair=True, style='fringe', apron=C(245, 240, 230), ribbon=C(120, 190, 255), seed=1)
make('nuna', SK2, C(185, 185, 190), C(90, 70, 50), C(110, 90, 130), C(80, 60, 60), C(220, 180, 120),
     long_hair=True, shawl=C(160, 60, 90), bun=True, necklace=C(240, 200, 90), seed=2)
make('garth', SK1, C(90, 60, 40), C(60, 100, 60), C(120, 125, 140), C(70, 75, 90), C(200, 170, 60),
     helmet=C(150, 156, 170), beard=C(80, 55, 40), seed=3)
make('orsen', SK1, C(60, 60, 65), C(40, 40, 40), C(150, 25, 45), C(35, 35, 45), C(230, 190, 60),
     beard=C(55, 55, 60), coat_trim=C(235, 200, 90), style='fringe', necklace=C(240, 210, 90), seed=4)
make('mira', SK2, C(25, 25, 35), C(210, 60, 50), C(40, 40, 58), C(30, 30, 42), C(160, 40, 40),
     mask=C(35, 35, 48), hood=C(30, 30, 42), seed=5)
make('brok', SK2, C(140, 90, 40), C(60, 60, 60), C(135, 120, 90), C(85, 70, 50), C(180, 60, 40),
     bandana=C(190, 50, 40), scar=True, vest=C(95, 70, 45), style='messy', seed=6)
make('thug', SK3, C(30, 30, 30), C(50, 50, 50), C(80, 80, 92), C(55, 55, 66), C(120, 120, 130),
     bald=True, scar=True, beard=C(40, 35, 32), seed=7)
SKINS = [SK1, SK2, SK3, C(225, 180, 150), C(180, 130, 100)]
HAIRS = [C(120, 80, 40), C(40, 30, 20), C(200, 160, 80), C(170, 60, 40), C(90, 60, 40), C(60, 40, 30), C(30, 30, 30), C(210, 200, 190), C(150, 100, 60)]
SHIRTS = [C(150, 120, 70), C(95, 135, 95), C(165, 95, 60), C(100, 100, 155), C(120, 80, 120), C(170, 150, 100),
          C(180, 100, 100), C(70, 120, 150), C(200, 190, 150), C(120, 140, 80), C(150, 70, 70), C(90, 110, 130)]
PANTS = [C(70, 80, 60), C(60, 60, 90), C(70, 60, 50), C(60, 70, 70), C(85, 70, 70), C(55, 55, 60)]
for i in range(1, 17):
    rr = random.Random(500 + i)
    lh = rr.random() < 0.45
    st = rr.choice(['none', 'fringe', 'messy'])
    ap = C(240, 235, 225) if (lh and rr.random() < 0.3) else None
    vest = C(rr.randint(70, 110), rr.randint(55, 85), rr.randint(35, 60)) if rr.random() < 0.2 else None
    beard = None
    hair = rr.choice(HAIRS)
    if not lh and rr.random() < 0.25: beard = shade(hair, 0.85)
    make('citizen%d' % i, rr.choice(SKINS), hair, C(60, 60, 60), rr.choice(SHIRTS), rr.choice(PANTS), C(200, 190, 150),
         long_hair=lh, style=st, apron=ap, vest=vest, beard=beard, seed=10 + i)

# второстепенные жители
make('dorn', SK2, C(70, 45, 30), C(60, 60, 60), C(90, 60, 45), C(60, 55, 55), C(70, 70, 75),
     beard=C(70, 45, 30), apron=C(75, 55, 40), seed=30)
make('leya', SK1, C(150, 90, 50), C(80, 110, 70), C(220, 215, 200), C(110, 90, 80), C(200, 170, 120),
     long_hair=True, apron=C(250, 248, 240), seed=31)
make('osip', SK2, C(150, 150, 150), C(70, 100, 140), C(60, 100, 90), C(80, 75, 60), C(120, 90, 60),
     beard=C(150, 150, 150), helmet=C(190, 170, 110), seed=32)
make('foma', SK1, C(200, 200, 200), C(80, 80, 100), C(80, 85, 100), C(50, 50, 60), C(150, 150, 170),
     style='fringe', vest=C(60, 60, 75), seed=33)
make('tim', SK1, C(180, 120, 60), C(60, 130, 60), C(220, 80, 60), C(70, 80, 110), C(240, 210, 90),
     style='messy', seed=34)
make('guard', SK1, C(80, 60, 50), C(70, 70, 70), C(95, 105, 125), C(55, 60, 75), C(190, 160, 60),
     helmet=C(120, 130, 150), seed=35)
# иконка раковины эха 16x16
ic = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
rows = [
 "......tttt......", "....ttwwwwtt....", "...twwccccwwt...", "..twccwwwwccwt..",
 ".twcwwtttwwcwwt.", ".twcwtccctwcwt..", ".twcwtcwwcwcwt..", ".twcwtcwtcwcwt..",
 ".twcwwcwwcwcwt..", "..twcwwwwcwwt...", "..twwccccwwt....", "...ttwwwwtt.....",
 "....ttttttt.....", "......ppp.......", ".....ppppp......", "................"]
pal = {'t': (30, 90, 110, 255), 'w': (235, 250, 255, 255), 'c': (80, 200, 220, 255), 'p': (160, 120, 220, 255)}
for y, r in enumerate(rows):
    for x, ch in enumerate(r):
        if ch in pal: ic.putpixel((x, y), pal[ch])
ic.save(os.path.join(OUT, 'item', 'echo_shell.png'))

# превью: передние виды всех жителей одним листом (для проверки глазами)
names = ['tavi', 'nuna', 'garth', 'orsen', 'mira', 'brok', 'thug', 'dorn', 'leya', 'osip', 'foma', 'tim', 'guard',
         'citizen1', 'citizen2', 'citizen3', 'citizen4', 'citizen5', 'citizen6', 'citizen7', 'citizen8', 'citizen9', 'citizen10', 'citizen11', 'citizen12', 'citizen13', 'citizen14', 'citizen15', 'citizen16']
sheet = Image.new('RGBA', (len(names) * 40, 80), (60, 70, 80, 255))
for n, name in enumerate(names):
    sk = Image.open(os.path.join(OUT, 'entity', name + '.png'))
    ox = n * 40 + 12
    sheet.alpha_composite(sk.crop((8, 8, 16, 16)), (ox, 2))      # голова
    if name in ('garth', 'mira', 'guard', 'osip'):
        sheet.alpha_composite(sk.crop((40, 8, 48, 16)), (ox, 2))  # шлем/капюшон
    sheet.alpha_composite(sk.crop((20, 20, 28, 32)), (ox, 10))   # тело
    sheet.alpha_composite(sk.crop((44, 20, 48, 32)), (ox - 4, 10))
    sheet.alpha_composite(sk.crop((36, 52, 40, 64)), (ox + 8, 10))
    sheet.alpha_composite(sk.crop((4, 20, 8, 32)), (ox, 22))
    sheet.alpha_composite(sk.crop((20, 52, 24, 64)), (ox + 4, 22))
sheet = sheet.resize((sheet.width * 4, sheet.height * 4), Image.NEAREST)
prev = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'preview_skins.png')
sheet.save(prev)
print('ok')
