"""music.battle_buskoba — ブスコバ戦. Latin / mariachi x epic, A minor (C major B-section),
140 BPM, 56 bars (96 s), LOOP.
Two mariachi trumpets in 3rds/6ths (wide vibrato), vihuela/guitar strums (KS nylon),
guitarron bass, maracas / bongos (martillo) / claves (son 3-2), watery bubble synth,
epic toms & strings, gritos ("ay-ay-ay!").
Form: Intro fanfare(4) A(8) A2(8) B major(8) C comic break(8) D epic build(8) A'(8) Tag(4)."""
import numpy as np

import instruments as I
import dsp
from dsp import N
from sequencer import Song, prog, mel, shift, harmonize, pad_notes, bass_notes, voicing, chord_at, n2m

BPM = 140
BARS = 56
STRUM = [(0.5, False, 0.5), (1.0, True, 0.85), (1.5, False, 0.5), (2.5, False, 0.5), (3.0, True, 0.85),
         (3.5, False, 0.55)]


def grito(seed=0, f0=330.0):
    path = [(0.0, 'a')]
    t = 0.0
    for k in range(4):
        path += [(t + 0.09, 'i'), (t + 0.14, 'a')]
        t += 0.16
    path += [(t + 0.25, 'a'), (t + 0.45, 'i')]
    dur = t + 0.5
    y = I.shout(f0, dur, path, 'tenor', seed, 0.12, (1.0, 1.35, 0.9), jitter=0.02)
    y2 = I.shout(f0 * 1.26, dur, path, 'alto', seed + 1, 0.12, (1.0, 1.3, 0.85), jitter=0.02)
    tr = 1 + 0.25 * np.maximum(0, np.sin(2 * np.pi * 6.0 * np.arange(len(y)) / dsp.SR))
    return (y + 0.6 * y2) * tr * 0.8


def render():
    s = Song(BPM, BARS, loop=True, seed=121, name='buskoba')
    s.ir = dsp.make_ir(1.6, pre=0.012, hf=0.55, seed=131)
    s.room_ir = dsp.make_ir(0.5, pre=0.003, hf=0.5, er_span=0.02, seed=132)
    B = s.bar
    pI = prog('Am | Am | E7 | E7', B(0))
    PA = 'Am | G | F | E7 | Am | G | F E7 | Am'
    pA, pA2 = prog(PA, B(4)), prog(PA, B(12))
    pB = prog('C | G/B | Am | Em | F | C/E | Dm7 G7 | C', B(20))
    pC = prog('Am | E7 | E7 | Am | Am | E7 | E7 | Am', B(28))
    pD = prog('F | G | E7 | Am | F | G | E7 | E7', B(36))
    pA3 = prog(PA, B(44))
    pT = prog('F | F | E7 | E7', B(52))
    allp = pI + pA + pA2 + pB + pC + pD + pA3 + pT

    s.stem('tp1', gain=0.6, pan=0.22, rev=0.25)
    s.stem('tp2', gain=0.52, pan=-0.22, rev=0.25)
    s.stem('vihuela', gain=1.4, pan=0.35, rev=0.12)
    s.stem('guitar', gain=1.4, pan=-0.35, rev=0.15)
    s.stem('requinto', gain=4.2, pan=0.1, rev=0.25)
    s.stem('bass', gain=0.75, pan=0.0, rev=0.05)
    s.stem('strings', gain=0.55, pan=0.0, rev=0.35, width=1.4)
    s.stem('perc', gain=0.9, pan=0.0, rev=0.08, room=0.25)
    s.stem('shk', gain=1.7, pan=0.3, rev=0.08)
    s.stem('drums', gain=0.36, pan=0.0, rev=0.2, room=0.3)
    s.stem('bubbles', gain=1.3, pan=0.0, rev=0.3, dly=0.25, width=1.6)
    s.stem('voice', gain=1.3, pan=0.0, rev=0.3, room=0.2)

    # ---------------- trumpets
    themeA = ('A4:.75 C5:.75 E5:.5 A5:1.5 G5:.5 | F5:.75 E5:.75 D5:.5 B4:2 | C5:.75 D5:.75 E5:.5 F5:1 A5:1 | '
              'G#5:1.5 F5:.5 E5:1 D5:1 | A4:.75 C5:.75 E5:.5 A5:1.5 B5:.5 | C6:.75 B5:.75 A5:.5 G5:2 | '
              'F5:1 A5:1 G#5:1 B5:1 | A5:3 r:1')
    themeB = ('E5:2 G5:1 C6:1 | B5:2 A5:1 G5:1 | A5:1.5 G5:.5 E5:1 C5:1 | B4:3 r:1 | A5:2 C6:1 A5:1 | '
              'G5:2 E5:1 C5:1 | F5:1 A5:1 G5:1 B5:1 | C6:3 r:1')
    themeD = 'A5:2 C6:2 | B5:2 D6:2 | B5:1 G#5:1 E5:1 G#5:1 | A5:2 E5:2 | A5:2 C6:2 | B5:2 D6:2 | E6:2 D6:1 B5:1 | G#5:4'
    intro = 'A4:.5 C5:.5 E5:.5 A5:2.5 | r:4 | B4:.5 D5:.5 E5:.5 G#5:2.5 | r:2 E5:.5 F5:.5 G#5:.5 B5:.5'
    tag = 'A5:1 A5:.5 A5:.5 C6:1 A5:1 | F5:4 | G#5:1 G#5:.5 G#5:.5 B5:1 G#5:1 | E5:3 r:1'
    for text, b0, p, v in ((intro, 0, pI, 0.9), (themeA, 4, pA, 0.85), (themeA, 12, pA2, 0.9), (themeB, 20, pB, 0.85),
                           (themeD, 36, pD, 0.95), (themeA, 44, pA3, 1.0), (tag, 52, pT, 1.0)):
        m = mel(text, B(b0), v, legato=0.92)
        s.play('tp1', I.brass, m, kind='mariachi')
        s.play('tp2', I.brass, harmonize(m, p), kind='mariachi')
    # A2 & A': violins double an octave below the trumpets (mariachi violins)
    for b0, p in ((12, pA2), (44, pA3)):
        s.play('strings', I.strings, mel(themeA, B(b0), 0.7, transpose=0, legato=0.95), attack=0.04, marc=True)
    s.play('strings', I.strings, pad_notes(pB + pD, 57, 76, 4, 0.5), attack=0.25)
    run = []
    for k in range(8):
        sym = chord_at(pD, B(36 + k))
        v = voicing(sym, 64, 84, 4)
        for i in range(16):
            run.append((B(36 + k) + i * 0.25, 0.25, v[i % 4] + 12 * ((i // 4) % 2), 0.45 + 0.04 * k))
    s.play('strings', I.strings, run, marc=True, legato=0.9)

    # ---------------- strums (vihuela high, guitar mid)
    cache = {}

    def strum_at(stem, b, sym, down, vel, lo, hi, n, mute=False):
        key = (stem, sym, down, round(vel, 1), mute)
        if key not in cache:
            fr = [float(dsp.mtof(m)) for m in voicing(sym, lo, hi, n)]
            cache[key] = I.strum(fr, 0.25 if mute else 0.35, vel, seed=len(cache), spread=0.009 if down else 0.007,
                                 down=down, mute=mute)
        s.add(stem, b, cache[key], jitter_ms=3)

    for (b, d, sym) in allp:
        sec = int(b // 4)
        t0 = b
        while t0 < b + d - 1e-6:
            for (o, down, v) in STRUM:
                if t0 + o < b + d - 1e-6:
                    if 28 <= sec < 36:
                        if o in (1.0, 3.0):
                            strum_at('vihuela', t0 + o, sym, True, v * 0.7, 60, 76, 4)
                        continue
                    strum_at('vihuela', t0 + o, sym, down, v, 60, 76, 4)
                    strum_at('guitar', t0 + o, sym, down, v * 0.8, 50, 67, 5, mute=not down)
            t0 += 4.0
    # rasgueado flourish at intro / tag downbeats
    for b in (B(0), B(2), B(52), B(54)):
        sym = chord_at(allp, b)
        for i in range(5):
            strum_at('guitar', b + i * 0.0625, sym, True, 0.6 + 0.08 * i, 50, 67, 5)

    # ---------------- guitarron bass
    oom = [(0, 1, 'B', 1.0), (2, 1, '5', 0.85)]
    s.play('bass', I.upright, bass_notes(allp, oom, 2, 0.9), t60=0.9)
    s.play('bass', I.upright, bass_notes(allp, oom, 1, 0.55), t60=0.9)

    # ---------------- requinto melody (comic break)
    themeC = ('A4:.5 B4:.5 C5:.5 E5:.5 A5:1 G#5:1 | B5:1 G#5:.5 E5:.5 D5:1 B4:1 | E5:.5 F5:.5 E5:.5 D5:.5 B4:1 G#4:1 | '
              'A4:2 r:2 | C5:.5 B4:.5 A4:.5 B4:.5 C5:.5 E5:.5 A5:1 | G#5:.5 F5:.5 E5:.5 D5:.5 B4:1 E5:1 | '
              'D5:.5 B4:.5 G#4:.5 B4:.5 E4:1 G#4:1 | A4:3 r:1')
    s.play('requinto', I.nylon, mel(themeC, B(28), 0.85), t60=1.2)
    s.play('requinto', I.nylon, shift(mel(themeC, B(28), 0.6), 0, 12), t60=1.0)

    # ---------------- percussion
    for k in range(BARS):
        bb = B(k)
        busy = k >= 4
        s.pattern('shk', I.maracas, 'x.X.x.X.x.X.x.X.' if busy else 'x...x...x...x...', bb, 0.25,
                  vels={'X': 0.65, 'x': 0.4})
        if busy:
            s.pattern('perc', I.bongo, 'X.x...x.X.x...x.', bb, 0.25, pan=0.25, high=True, vels={'X': 0.7, 'x': 0.45})
            s.pattern('perc', I.bongo, '....o.......o..o', bb, 0.25, pan=0.15, high=False, vels={'o': 0.6})
            s.pattern('perc', I.claves, 'X.....X.....X...' if k % 2 == 0 else '....X...X.......', bb, 0.25,
                      pan=-0.3, vels={'X': 0.6})
    # bongo solo bits in the comic break
    for k in (31, 35):
        s.pattern('perc', I.bongo, 'XxXxXxxXXXxxXXXX', B(k, 2), 0.125, high=True, slap=True, vels={'X': 0.8, 'x': 0.5})
    # epic drums in D & A'
    for k in range(36, 52):
        s.pattern('drums', I.taiko, 'X.....X.X.......' if k % 2 == 0 else 'X.....X.X...X.XX', B(k), 0.25, f=75.0)
        s.pattern('drums', I.tom, '....x.......x...', B(k), 0.25, f=160.0, vels={'x': 0.6})
    s.pattern('drums', I.tom, 'XXxxXXxxXXXXXXXX', B(43), 0.25, f=120.0)
    for b in (4, 20, 36, 44):
        s.hit('drums', I.cymbal, B(b), 0.85, decay=1.5)
        s.hit('drums', I.gran_cassa, B(b), 0.8)

    # ---------------- bubbles (watery synth)
    r = s.rng
    pent = [n2m(x) for x in ('A4', 'C5', 'D5', 'E5', 'G5', 'A5', 'C6', 'E6')]
    for k in list(range(12, 20)) + list(range(28, 36)):
        dens = 0.35 if k >= 28 else 0.18
        for i in range(16):
            if r.uniform() < dens:
                m = int(r.choice(pent))
                s.add('bubbles', B(k) + i * 0.25,
                      I.bubble(float(dsp.mtof(m)), r.uniform(0.05, 0.12), r.uniform(0.4, 0.8), int(r.integers(1e6)),
                               up=r.uniform(1.4, 2.2)), pan=float(r.uniform(-0.8, 0.8)))
    for b in (3.5, 19.5, 27.5, 43.5, 55.5):  # bubbly up-runs into sections
        for i, m in enumerate(pent):
            s.add('bubbles', B(int(b), 2) + i * 0.25, I.bubble(float(dsp.mtof(m)), 0.08, 0.7, i, up=2.0),
                  pan=-0.6 + 0.17 * i)

    # ---------------- gritos
    for b, sd in ((11, 1), (19, 2), (43, 3), (51, 4), (55, 5)):
        s.add('voice', B(b, 2.5), grito(sd, 330.0 + 20 * sd), pan=float(r.uniform(-0.3, 0.3)))

    return s.mixdown(peak_db=-3.0, drive_db=3.0, master_fx=[lambda z: dsp.hshelf(z, 5000, 3.0)])
