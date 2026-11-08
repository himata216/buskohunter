"""music.hub — 集会所/広場テーマ. D major, 96 BPM, 48 bars (120 s), LOOP.
Warm festive village tune: flute/ocarina lead, pizzicato oom-pah, harp
arpeggios, frame drum / tambourine / shaker, strings in the B section.

Form: Intro(4) A(8) A2(8) B(8) C-dance(8) A'(8) Outro(4) -> back to Intro.
"""
import instruments as I
import dsp
from sequencer import (Song, prog, mel, shift, harmonize, arp_notes, pad_notes, bass_notes,
                       chord_hits)

BPM = 96
BARS = 48


def render():
    s = Song(BPM, BARS, loop=True, seed=11, name='hub')
    s.ir = dsp.make_ir(1.9, pre=0.018, hf=0.5, seed=21)
    B = s.bar

    P_INTRO = prog('D | G | D | Asus4 A', B(0))
    P_A = 'D | G | D | A | D | G | Em7 A7 | D'
    P_B = 'Bm | G | D | A | Bm | G | Em | Asus4 A'
    P_C = 'G | A | F#m | Bm | G | A | D/F# G | A7'
    P_OUT = prog('G | D/F# | Em7 | A7', B(44))
    pA1, pA2 = prog(P_A, B(4)), prog(P_A, B(12))
    pB, pC, pA3 = prog(P_B, B(20)), prog(P_C, B(28)), prog(P_A, B(36))
    allp = P_INTRO + pA1 + pA2 + pB + pC + pA3 + P_OUT

    # ---------------- stems
    s.stem('flute', gain=1.0, pan=0.12, rev=0.28)
    s.stem('ocarina', gain=1.0, pan=-0.15, rev=0.3)
    s.stem('glock', gain=0.8, pan=0.3, rev=0.35)
    s.stem('harp', gain=1.0, pan=-0.25, rev=0.3, width=1.3)
    s.stem('pizz', gain=2.6, pan=0.25, rev=0.22)
    s.stem('bass', gain=1.0, pan=0.0, rev=0.1)
    s.stem('strings', gain=0.55, pan=0.0, rev=0.4, width=1.4)
    s.stem('perc', gain=0.6, pan=0.0, rev=0.15)
    s.stem('jingle', gain=1.2, pan=0.35, rev=0.2)

    # ---------------- melody
    theme = ('D5:1.5 E5:.5 F#5:1 A5:1 | B5:1.5 A5:.5 G5:1 F#5:.5 E5:.5 | F#5:1.5 E5:.5 D5:1 A4:1 | '
             'B4:.5 C#5:.5 D5:.5 E5:.5 E5:2 | D5:1.5 E5:.5 F#5:1 A5:1 | B5:1 D6:1.5 B5:.5 G5:1 | '
             'A5:1 G5:.5 F#5:.5 E5:1 C#5:1 | D5:3 r:1')
    mA1 = mel(theme, B(4), 0.78, legato=0.95)
    s.play('flute', I.flute, mA1)
    # A2: ocarina melody, flute takes harmony a third/sixth above
    mA2 = mel(theme, B(12), 0.75, legato=0.95)
    s.play('ocarina', I.flute, mA2, kind='ocarina')
    s.play('flute', I.flute, harmonize(mA2, pA2, below=False), vel_jit=0.04)

    themeB = ('F#5:1.5 E5:.5 D5:1 F#5:1 | G5:1.5 F#5:.5 E5:1 D5:1 | A5:1.5 G5:.5 F#5:1 E5:.5 D5:.5 | '
              'E5:3 r:1 | F#5:1.5 E5:.5 D5:1 F#5:1 | B5:1.5 A5:.5 G5:1 B5:1 | A5:1 G5:1 F#5:1 E5:1 | '
              'E5:2 C#5:1 A4:1')
    mB = mel(themeB, B(20), 0.72)
    s.play('flute', I.flute, mB)
    # ocarina answers in the gaps
    s.play('ocarina', I.flute, mel('r:3 A4:.5 B4:.5', B(23), 0.6) + mel('r:2 A4:.5 B4:.5 C#5:.5 E5:.5', B(27), 0.6),
           kind='ocarina')

    themeC = ['D5:.5 E5:.5 G5:.5 A5:.5 B5:1 A5:1', 'C#6:.5 B5:.5 A5:.5 G5:.5 A5:2',
              'C#6:1 A5:.5 F#5:.5 A5:1 C#6:1', 'D6:1.5 C#6:.5 B5:1 F#5:1',
              'G5:.5 A5:.5 B5:.5 D6:.5 E6:1 D6:1', 'C#6:.5 D6:.5 E6:.5 C#6:.5 A5:2',
              'F#5:1 A5:1 B5:1 D6:1', 'E6:1 C#6:1 A5:1 G5:.5 E5:.5']
    for i, ph in enumerate(themeC):
        notes = mel(ph, B(28 + i), 0.8)
        if i % 2 == 0:
            s.play('flute', I.flute, notes)
        else:
            s.play('ocarina', I.flute, shift(notes, 0, -12), kind='ocarina')
            s.play('glock', I.bell, shift(notes, 0, 0, 0.7), kind='glock')

    # A': flute + ocarina in octaves, glockenspiel sparkle
    mA3 = mel(theme, B(36), 0.85, legato=0.95)
    s.play('flute', I.flute, mA3)
    s.play('ocarina', I.flute, shift(mA3, 0, -12, 0.9), kind='ocarina')
    s.play('glock', I.bell, shift(mA3, 0, 12, 0.55), kind='glock')
    s.play('flute', I.flute, mel('B5:2 A5:1 G5:1 | F#5:2 E5:1 D5:1 | E5:1.5 F#5:.5 G5:1 B5:1 | A5:2 G5:1 E5:1',
                                 B(44), 0.62))
    # intro glock motif
    s.play('glock', I.bell, mel('A5:1 D6:1 F#6:2 | r:4 | A5:1 D6:1 F#6:1 E6:1', B(1), 0.55), kind='glock')

    # ---------------- harp
    up = ['R', '5', '8', '10', '12', '10', '8', '5']
    for p, v in ((P_INTRO, 0.55), (pA1, 0.5), (pA2, 0.45), (pA3, 0.5), (P_OUT, 0.55)):
        s.play('harp', I.harp, arp_notes(p, up, 0.5, 3, v), jitter_ms=3)
    # B: rolled half-note chords
    for (b, d, sym) in pB:
        for k, deg in enumerate(['R', '5', '8', '10', '12']):
            from sequencer import chord_tone
            s.play('harp', I.harp, [(b + k * 0.06, 2.0, chord_tone(sym, deg, 3), 0.5)])
            s.play('harp', I.harp, [(b + 2 + k * 0.06, 1.5, chord_tone(sym, deg, 3), 0.42)])
    # C: lively broken chords in 8ths, higher
    s.play('harp', I.harp, arp_notes(pC, ['R', '10', '5', '12', '8', '10', '5', '12'], 0.5, 4, 0.42))

    # ---------------- pizzicato oom-pah + bass
    oom = [(0, 1, 'B', 1.0), (2, 1, '5', 0.85)]
    for p in (pA1, pA2, pC, pA3):
        s.play('bass', I.upright, bass_notes(p, oom, 2, 0.85), t60=1.0)
        s.play('pizz', I.pizz, chord_hits(p, [1, 3], 62, 76, 3, 0.55, 0.5))
    s.play('bass', I.upright, bass_notes(P_INTRO + P_OUT, [(0, 2, 'B', 1.0), (2, 2, '5', 0.8)], 2, 0.75), t60=1.4)
    s.play('bass', I.upright, bass_notes(pB, [(0, 3, 'B', 1.0), (3, 1, '5', 0.7)], 2, 0.7), t60=1.4)
    s.play('pizz', I.pizz, chord_hits(pB, [1.5, 3], 62, 76, 3, 0.4, 0.5))

    # ---------------- strings pad (B, A', outro)
    s.play('strings', I.strings, pad_notes(pB, 57, 76, 4, 0.5), attack=0.35, release=0.6)
    s.play('strings', I.strings, pad_notes(pA3 + P_OUT, 55, 74, 3, 0.4), attack=0.4, release=0.6)
    s.play('strings', I.strings, pad_notes(P_INTRO, 50, 69, 3, 0.32), attack=0.6, release=0.8)

    # ---------------- percussion
    def groove(b0, bars, tamb=True, busy=False):
        for k in range(bars):
            bb = B(b0 + k)
            s.pattern('perc', I.frame_drum, 'X.....x.X...o...' if not busy else 'X..x..x.X.x.x.xo', bb, 0.25,
                      pan=-0.1)
            s.pattern('perc', I.frame_drum, '....x.......x.o.', bb, 0.25, pan=0.15, edge=True)
            s.pattern('jingle', I.shaker, 'x.o.x.o.x.o.x.o.', bb, 0.25, vels={'x': 0.55, 'o': 0.32})
            if tamb:
                s.pattern('jingle', I.tambourine, '....X.......X...', bb, 0.25, pan=0.4)
    groove(4, 8, tamb=False)
    groove(12, 8, tamb=True)
    for k in range(8):  # B: light
        s.pattern('perc', I.frame_drum, 'X.......x.......', B(20 + k), 0.25, vels={'X': 0.55, 'x': 0.35})
        s.pattern('jingle', I.shaker, 'x.o.x.o.x.o.x.o.', B(20 + k), 0.25, vels={'x': 0.4, 'o': 0.25})
    groove(28, 7, tamb=True, busy=True)
    s.pattern('perc', I.frame_drum, 'X.x.X.x.xxxxXXXX', B(35), 0.25, pan=-0.05)  # fill into A'
    s.hit('jingle', I.tambourine, B(35, 3.5), 0.8, shake=True)
    groove(36, 8, tamb=True)
    for k in range(4):
        s.pattern('perc', I.frame_drum, 'X.......X.......', B(44 + k), 0.25, vels={'X': 0.5})
        s.pattern('jingle', I.shaker, 'x.o.x.o.x.o.x.o.', B(44 + k), 0.25, vels={'x': 0.4, 'o': 0.25})
    s.pattern('jingle', I.shaker, '........x.o.x.o.', B(3), 0.25, vels={'x': 0.35, 'o': 0.22})

    return s.mixdown(peak_db=-3.0, drive_db=1.5,
                     master_fx=[lambda z: dsp.lshelf(z, 180, 1.0), lambda z: dsp.hshelf(z, 5000, 2.5)])
