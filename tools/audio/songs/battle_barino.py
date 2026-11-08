"""music.battle_barino — ブスコバリーノ王戦. Sacred & epic, C minor, 120 BPM, 48 bars (96 s), LOOP.
Pipe organ, SATB 'ahh' choir (formant synthesis), harp, tubular/church bells,
heavy timpani, brass chorale, string ostinato.
Form: Intro bells(4) A choir theme(8) B organ+brass(8) C heavenly Ab(8) D build(8) A'(8+4)."""
import instruments as I
import dsp
from sequencer import (Song, prog, mel, shift, harmonize, arp_notes, pad_notes, bass_notes, chord_hits,
                       voicing, root_note, bass_note)

BPM = 120
BARS = 48


def render():
    s = Song(BPM, BARS, loop=True, seed=61, name='barino')
    s.ir = dsp.make_ir(4.2, pre=0.04, hf=0.5, lf=1.2, seed=71, er_span=0.12)
    B = s.bar
    PA = 'Cm | Ab | Fm | G | Cm | Ab | Db | Gsus4 G'
    pI = prog('Cm | Cm | Ab | G', B(0))
    pA, pB = prog(PA, B(4)), prog(PA, B(12))
    pC = prog('Ab | Eb | Fm | Db | Ab | Eb/G | Fm7 | G', B(20))
    pD = prog('Cm | Cm/Bb | Ab | G | Cm | Cm/Bb | Ab | G', B(28))
    pA2 = prog(PA, B(36)) + prog('Cm | Ab | Db | G', B(44))

    s.stem('choirS', gain=1.45, pan=0.1, rev=0.5, width=1.5, fx=[lambda z: dsp.formant(z, 'soprano', 'a', bw_scale=1.3)])
    s.stem('choirA', gain=1.9, pan=-0.15, rev=0.5, width=1.5, fx=[lambda z: dsp.formant(z, 'alto', 'a', bw_scale=1.3)])
    s.stem('choirL', gain=1.35, pan=0.0, rev=0.5, width=1.5, fx=[lambda z: dsp.formant(z, 'bass', 'o', bw_scale=1.2)])
    s.stem('organ', gain=0.8, pan=0.0, rev=0.45, width=1.5)
    s.stem('pedal', gain=1.0, pan=0.0, rev=0.35)
    s.stem('brass', gain=0.62, pan=-0.15, rev=0.35)
    s.stem('bells', gain=1.0, pan=0.25, rev=0.5)
    s.stem('harp', gain=1.5, pan=-0.3, rev=0.4, width=1.3)
    s.stem('strings', gain=0.8, pan=0.1, rev=0.35, width=1.3)
    s.stem('timp', gain=0.9, pan=0.0, rev=0.3)
    s.stem('perc', gain=0.6, pan=0.0, rev=0.35)

    theme = ('C5:1 D5:1 Eb5:2 | C5:1 Eb5:1 Ab5:2 | G5:1 F5:1 Eb5:1 C5:1 | D5:3 B4:1 | '
             'C5:1 D5:1 Eb5:1 G5:1 | Ab5:2 G5:1 F5:1 | F5:2 Ab5:1 F5:1 | G5:1.5 F5:.5 D5:1 B4:1')

    # ---------- intro: bells toll + organ pedal + choir swell
    for k, m in enumerate([60, 60, 56, 55]):
        s.play('bells', I.bell, [(B(k), 4, m, 0.85)], kind='church')
    s.play('pedal', I.organ, [(B(0), 8, 36, 0.8), (B(2), 4, 32, 0.8), (B(3), 4, 31, 0.8)], reg='pedal', release=0.6)
    s.play('choirL', I.choir_src, pad_notes(pI, 43, 60, 3, 0.55), attack=1.2, release=0.8)
    s.play('choirA', I.choir_src, pad_notes(pI, 60, 72, 2, 0.45), attack=1.5, release=0.8)
    s.add('timp', B(3), I.timp_roll(float(dsp.mtof(43)), s.secs(4), 0.1, 0.85, seed=1, end_hit=False))

    # ---------- A: choir theme, string ostinato, timpani
    mA = mel(theme, B(4), 0.8)
    s.play('choirS', I.choir_src, mA, attack=0.08, release=0.35)
    s.play('choirL', I.choir_src, shift(mA, 0, -12, 0.85), attack=0.08, release=0.35)
    s.play('choirA', I.choir_src, pad_notes(pA, 55, 67, 2, 0.45), attack=0.4)
    ost = ['R', '8', '5', '8', '3', '8', '5', '8']
    s.play('strings', I.strings, arp_notes(pA + pB, ost, 0.5, 3, 0.55, 0.15), sect='vla', marc=True, legato=0.85)
    s.play('harp', I.harp, arp_notes(pA, ['R', '5', '8', '10', '12', '10', '8', '5'], 0.5, 3, 0.4))

    # ---------- B: full organ + brass theme + bells on the melody
    mB = mel(theme, B(12), 0.9)
    s.play('organ', I.organ, pad_notes(pB, 55, 72, 4, 0.8), reg='full')
    s.play('brass', I.brass_section, shift(mB, 0, -12), kind='horn', players=3)
    s.play('brass', I.brass_section, mB, kind='trumpet', players=2)
    s.play('brass', I.brass_section, shift(harmonize(mB, pB), 0, -12), kind='trombone', players=2)
    s.play('bells', I.bell, [n for n in mB if abs(n[0] % 2) < 1e-6], kind='tubular')
    s.play('choirL', I.choir_src, pad_notes(pB, 43, 60, 3, 0.5), attack=0.3)

    # ---------- C: heavenly — harp, soprano solo, soft organ flute
    themeC = ('Eb5:2 C5:1 Ab4:1 | G5:2 Eb5:1 Bb4:1 | Ab5:2 F5:1 C5:1 | Ab5:2 F5:2 | Eb5:2 Ab5:1 C6:1 | Bb5:2 G5:2 | '
              'Ab5:1 G5:1 F5:1 Eb5:1 | D5:4')
    mC = mel(themeC, B(20), 0.7)
    s.play('choirS', I.choir_src, mC, attack=0.15, release=0.5, voices=2)
    s.play('organ', I.organ, pad_notes(pC, 53, 70, 4, 0.45), reg='flute', chiff=0.05)
    s.play('harp', I.harp, arp_notes(pC, ['R', '5', '8', '10', '12', '15' if False else '8', '10', '5'], 0.25, 3, 0.4))
    s.play('bells', I.bell, [(B(20 + k), 2, n, 0.5) for k, n in enumerate([84, 82, 84, 80, 84, 82, 80, 79])],
           kind='celesta')
    s.play('pedal', I.organ, [(b, d, bass_note(sym, 2), 0.6) for (b, d, sym) in pC], reg='pedal')

    # ---------- D: build — brass melody, choir stabs, timpani 8ths, strings 16ths
    themeD = ('G4:1 C5:1 Eb5:1 D5:1 | C5:1 Bb4:1 G4:2 | Ab4:1 C5:1 Eb5:1 F5:1 | D5:2 B4:2 | '
              'G4:1 C5:1 Eb5:1 G5:1 | G5:1 Eb5:1 C5:2 | C5:1 Eb5:1 Ab5:1 G5:1 | G5:2 F5:1 D5:1')
    mD = mel(themeD, B(28), 0.9)
    s.play('brass', I.brass_section, mD, kind='trumpet', players=2)
    s.play('brass', I.brass_section, shift(mD, 0, -12), kind='horn', players=3)
    stabs = chord_hits(pD, [0.5, 1.5, 2.5, 3.5], 55, 70, 3, 0.75, 0.35)
    s.play('choirA', I.choir_stab, stabs)
    s.play('choirL', I.choir_stab, chord_hits(pD, [0.5, 1.5, 2.5, 3.5], 43, 55, 2, 0.75, 0.35))
    s.play('strings', I.strings, arp_notes(pD, ['R', '3', '5', '8'], 0.25, 4, 0.5, 0.15), sect='vln', marc=True,
           legato=0.8)
    s.play('organ', I.organ, chord_hits(pD, [0], 48, 67, 4, 0.75, 2.0), reg='full')
    for (b, d, sym) in pD:
        r = root_note(sym, 2)
        r = r - 12 if r > 50 else r
        for i in range(int(d * 2)):
            s.hit('timp', I.timpani, b + i * 0.5, 0.75 if i % 2 == 0 else 0.5, f=float(dsp.mtof(r)))
    s.add('timp', B(35), I.timp_roll(float(dsp.mtof(43)), s.secs(4), 0.3, 1.0, seed=7, end_hit=False))

    # ---------- A': tutti
    mA2 = mel(theme, B(36), 1.0)
    s.play('choirS', I.choir_src, mA2, attack=0.06, release=0.35)
    s.play('choirL', I.choir_src, shift(mA2, 0, -12), attack=0.06, release=0.35)
    s.play('brass', I.brass_section, shift(mA2, 0, -12), kind='horn', players=3)
    s.play('brass', I.brass_section, shift(harmonize(mA2, pA2), 0, -12), kind='trombone', players=2)
    s.play('organ', I.organ, pad_notes(pA2, 55, 72, 4, 0.85), reg='full')
    s.play('bells', I.bell, [n for n in mA2 if abs(n[0] % 2) < 1e-6], kind='tubular')
    s.play('strings', I.strings, arp_notes(pA2, ['R', '3', '5', '8', '10', '8', '5', '3'], 0.25, 4, 0.45), sect='vln',
           marc=True, legato=0.8)
    coda = mel('Eb5:2 G5:2 | Ab5:2 C6:2 | Ab5:2 F5:2 | G5:2 B4:2', B(44), 0.95)
    s.play('choirS', I.choir_src, coda, attack=0.1)
    s.play('brass', I.brass_section, coda, kind='trumpet', players=2)
    s.play('choirA', I.choir_src, pad_notes(prog('Cm | Ab | Db | G', B(44)), 55, 67, 2, 0.6), attack=0.2)

    # pedal bass everywhere except C
    for p in (pA, pB, pD, pA2):
        s.play('pedal', I.organ, [(b, d, bass_note(sym, 2), 0.75) for (b, d, sym) in p], reg='pedal')
    # heavy timpani & percussion accents
    for p in (pA, pB, pA2):
        for (b, d, sym) in p:
            r = root_note(sym, 2)
            r = r - 12 if r > 50 else r
            s.hit('timp', I.timpani, b, 0.85, f=float(dsp.mtof(r)))
    for b in (4, 12, 28, 36, 44):
        s.hit('perc', I.cymbal, B(b), 0.9, decay=2.5)
        s.hit('perc', I.gran_cassa, B(b), 0.9)
    s.add('perc', B(18), I.cymbal(0.6, 3, decay=1.0, swell=s.secs(8)))
    s.add('perc', B(46), I.cymbal(0.6, 4, decay=1.0, swell=s.secs(8)))
    for k in range(4):
        s.play('bells', I.bell, [(B(44 + k), 4, [60, 56, 61, 55][k], 0.8)], kind='church')

    return s.mixdown(peak_db=-3.0, drive_db=3.0, master_fx=[lambda z: dsp.hshelf(z, 5000, 3.0)])
