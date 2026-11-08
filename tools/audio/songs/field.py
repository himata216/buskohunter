"""music.field — フィールド探索. Eb major, 80 BPM, 36 bars (108 s), LOOP.
Calm & majestic: string pads, long horn melody, violin theme, harp, soft
timpani swells, bird-call ornaments.
Form: Intro(4) A(8 horn) B(8 violins) A'(8 tutti) Outro(8 flute + birds)."""
import instruments as I
import dsp
from sequencer import Song, prog, mel, shift, harmonize, arp_notes, pad_notes, bass_notes, root_note

BPM = 80
BARS = 36


def render():
    s = Song(BPM, BARS, loop=True, seed=31, name='field')
    s.ir = dsp.make_ir(2.8, pre=0.03, hf=0.45, seed=41)
    B = s.bar
    pI = prog('Eb | Ab/Eb | Eb | Bbsus4 Bb', B(0))
    PA = 'Eb | Ab/Eb | Eb | Bb/D | Cm | Ab | Fm7 Bb7 | Eb'
    pA, pB, pA2 = prog(PA, B(4)), prog('Ab | Bb | Gm | Cm | Fm | Bb/D | Eb/G Ab | Bbsus4 Bb', B(12)), prog(PA, B(20))
    pO = prog('Ab | Eb/G | Fm | Bb | Ab | Eb/G | Fm7 | Bbsus4 Bb', B(28))
    allp = pI + pA + pB + pA2 + pO

    s.stem('horn', gain=1.6, pan=-0.15, rev=0.38)
    s.stem('vln', gain=0.85, pan=0.15, rev=0.4, width=1.3)
    s.stem('pad', gain=0.7, pan=0.0, rev=0.45, width=1.5)
    s.stem('cb', gain=0.9, pan=0.0, rev=0.25)
    s.stem('harp', gain=1.0, pan=-0.3, rev=0.4, width=1.3)
    s.stem('flute', gain=0.9, pan=0.25, rev=0.45)
    s.stem('timp', gain=0.7, pan=-0.1, rev=0.35)
    s.stem('birds', gain=0.8, pan=0.0, rev=0.5, width=1.6)
    s.stem('cym', gain=0.35, pan=0.2, rev=0.4)

    themeA = ('Bb3:1 Eb4:1.5 F4:.5 G4:1 | Ab4:2 C5:1 Ab4:1 | G4:1.5 F4:.5 Eb4:1 Bb3:1 | F4:4 | '
              'G4:1 C5:1.5 Bb4:.5 G4:1 | Ab4:2 Eb4:1 F4:1 | Ab4:1 G4:1 F4:1 D4:1 | Eb4:4')
    mA = mel(themeA, B(4), 0.72)
    s.play('horn', I.brass_section, mA, kind='horn', players=2, attack=0.09)
    themeB = ('C5:1.5 Eb5:.5 Ab5:2 | F5:1.5 Eb5:.5 D5:2 | Bb5:1.5 G5:.5 D5:2 | Eb5:1 G5:1 C5:2 | '
              'Ab5:1.5 G5:.5 F5:1 C5:1 | D5:1 F5:1 Bb5:2 | G5:1.5 Ab5:.5 C6:1 Ab5:1 | Eb5:2 D5:2')
    mB = mel(themeB, B(12), 0.68)
    s.play('vln', I.strings, mB, attack=0.12, release=0.4)
    s.play('horn', I.brass_section, mel('Eb4:4 | D4:4 | D4:4 | Eb4:4 | C4:4 | D4:4 | Eb4:4 | F4:2 D4:2', B(12), 0.5),
           kind='horn', players=2, attack=0.2, vib=2)
    mA2 = mel(themeA, B(20), 0.82)
    s.play('horn', I.brass_section, mA2, kind='horn', players=3, attack=0.08)
    s.play('vln', I.strings, shift(mA2, 0, 12, 0.85), attack=0.1, release=0.4)
    s.play('pad', I.strings, shift(mA2, 0, -12, 0.75), sect='vc', attack=0.1, release=0.4)
    themeO = ('Eb5:2 C5:1 Ab4:1 | Bb4:3 r:1 | C5:1.5 Ab4:.5 F4:2 | D5:2 Bb4:2 | C5:2 Eb5:1 Ab5:1 | G5:3 r:1 | '
              'Ab5:1.5 F5:.5 C5:2 | Eb5:2 D5:2')
    s.play('flute', I.flute, mel(themeO, B(28), 0.6))

    # pads (strings), contrabass
    s.play('pad', I.strings, pad_notes(allp, 51, 70, 4, 0.42), sect='vla', attack=0.6, release=1.0)
    for (b, d, sym) in prog('Eb | Eb | Eb | Bb', B(0)) + pA + pB + pA2 + pO:
        from sequencer import bass_note
        s.play('cb', I.strings, [(b, d + 0.05, bass_note(sym, 2), 0.5)], sect='cb', attack=0.25, release=0.5)

    # harp arpeggios (A, A', outro)
    up = ['R', '5', '8', '10', '12', '10', '8', '5']
    for p, v in ((pA, 0.45), (pA2, 0.5), (pI, 0.4)):
        s.play('harp', I.harp, arp_notes(p, up, 0.5, 3, v), jitter_ms=4)
    s.play('harp', I.harp, arp_notes(pB, ['R', '5', '10', '8', '12', '8', '10', '5'], 0.5, 3, 0.35), jitter_ms=4)
    s.play('harp', I.harp, arp_notes(pO, ['R', '8', '12', '15' if False else '10'], 1.0, 3, 0.4), jitter_ms=5)

    # soft timpani swells at phrase ends, suspended cymbal swells
    for bar_, root in ((11, 46), (19, 46), (27, 46), (35, 46)):
        s.add('timp', B(bar_, 2), I.timp_roll(float(dsp.mtof(root)), s.secs(2), 0.08, 0.45, seed=bar_, end_hit=False))
        s.add('cym', B(bar_, 1), I.cymbal(0.4, bar_, decay=1.5, swell=s.secs(3)))
    for b in (4, 20):
        s.hit('timp', I.timpani, B(b), 0.55, f=float(dsp.mtof(39)))

    # birds: lots in intro/outro, sparse elsewhere
    r = s.rng
    for k in range(70):
        bar_ = r.uniform(0, BARS)
        if 6 <= bar_ < 28 and r.uniform() < 0.65:
            continue
        s.add('birds', B(0) + bar_ * 4, I.bird(int(r.integers(0, 10 ** 6)), r.uniform(0.3, 0.7)),
              pan=float(r.uniform(-0.9, 0.9)))
    # flute trill ornaments (bird-like) on top of B and A'
    for b in (14, 18, 22, 26):
        s.play('flute', I.flute, mel('Bb5:.125 C6:.125 Bb5:.125 C6:.125 Bb5:.125 C6:.125 Bb5:.25 Eb6:.5', B(b, 3) - 0.5,
                                     0.35), kind='piccolo')

    return s.mixdown(peak_db=-3.0, drive_db=1.0, master_fx=[lambda z: dsp.hshelf(z, 5000, 1.5)])
