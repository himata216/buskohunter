"""music.prepare — クエスト準備. F major march, 110 BPM, 28 bars (61.1 s), LOOP.
Snare cadence + horns, trumpets in B, strings ostinato, tuba oom-pah.
Form: Intro(4, snare cadence) A(8, horns) B(8, trumpets, minor colour) A'(8, tutti)."""
import instruments as I
import dsp
from sequencer import (Song, prog, mel, shift, harmonize, arp_notes, pad_notes, bass_notes, chord_hits)

BPM = 110
BARS = 28


def render():
    s = Song(BPM, BARS, loop=True, seed=23, name='prepare')
    s.ir = dsp.make_ir(1.7, pre=0.015, hf=0.5, seed=33)
    B = s.bar
    pI = prog('F | Bb/F | F | Csus4 C', B(0))
    PA = 'F | Bb | F | C | Dm | Bb | Gm C | F'
    pA, pB, pA2 = prog(PA, B(4)), prog('Dm | Am | Bb | F | Gm | Dm/F | Eb | Csus4 C', B(12)), prog(PA, B(20))

    s.stem('horns', gain=1.25, pan=-0.2, rev=0.3)
    s.stem('trumpets', gain=0.8, pan=0.2, rev=0.28)
    s.stem('trombones', gain=0.75, pan=0.1, rev=0.25)
    s.stem('tuba', gain=1.0, pan=0.0, rev=0.12)
    s.stem('strings', gain=0.6, pan=-0.1, rev=0.25, width=1.3)
    s.stem('vln', gain=0.45, pan=0.15, rev=0.3, width=1.3)
    s.stem('snare', gain=0.75, pan=0.05, rev=0.12, room=0.2)
    s.stem('bd', gain=0.42, pan=0.0, rev=0.1, fx=[lambda z: dsp.hp(z, 45, 2)])
    s.stem('cym', gain=0.55, pan=0.25, rev=0.2)
    s.stem('timp', gain=0.75, pan=-0.15, rev=0.25)
    s.stem('glock', gain=0.55, pan=0.3, rev=0.3)

    themeA = ('C4:1 F4:1.5 E4:.5 F4:.5 G4:.5 | Bb4:1.5 A4:.5 G4:1 D4:1 | C4:1.5 D4:.5 F4:1 A4:1 | G4:3 r:1 | '
              'A4:1 D5:1.5 C5:.5 A4:1 | Bb4:1.5 A4:.5 G4:1 F4:1 | G4:1 Bb4:1 A4:1 G4:1 | F4:3 r:1')
    mA = mel(themeA, B(4), 0.8, legato=0.92)
    s.play('horns', I.brass_section, mA, kind='horn', players=3)
    s.play('trombones', I.brass_section, shift(harmonize(mA, pA), 0, -12), kind='trombone', players=2)
    fill = mel('r:3 C5:1/3 E5:1/3 G5:1/3', B(7), 0.75) + mel('r:3 A4:1/3 C5:1/3 F5:1/3', B(11), 0.75)
    s.play('trumpets', I.brass_section, fill, kind='trumpet', players=2)

    themeB = ('D5:1.5 E5:.5 F5:1 D5:1 | E5:1.5 F5:.5 E5:1 C5:1 | D5:1.5 C5:.5 Bb4:1 D5:1 | C5:3 r:1 | '
              'Bb4:1 D5:1 G5:1.5 F5:.5 | F5:1 E5:.5 D5:.5 A4:2 | G5:1.5 F5:.5 Eb5:1 G5:1 | F5:1 E5:1 D5:1 C5:.5 E5:.5')
    mB = mel(themeB, B(12), 0.82, legato=0.92)
    s.play('trumpets', I.brass_section, mB, kind='trumpet', players=2)
    s.play('horns', I.brass_section, pad_notes(pB, 53, 67, 3, 0.55), kind='horn', players=2, vib=3)
    s.play('horns', I.brass_section, mel('r:3 A3:1/3 C4:1/3 F4:1/3', B(15), 0.7), kind='horn', players=2)

    mA2 = mel(themeA, B(20), 0.9, legato=0.92)
    s.play('trumpets', I.brass_section, shift(mA2, 0, 12), kind='trumpet', players=2)
    s.play('horns', I.brass_section, mA2, kind='horn', players=3)
    s.play('trombones', I.brass_section, shift(harmonize(mA2, pA2), 0, -12), kind='trombone', players=2)
    s.play('glock', I.bell, shift(mA2, 0, 24, 0.6), kind='glock')

    # tuba oom + trombone pah
    oom = [(0, 1, 'B', 1.0), (2, 1, '5', 0.8)]
    s.play('tuba', I.brass, bass_notes(pA + pB + pA2, oom, 1, 0.8, ), kind='tuba', legato=0.8)
    s.play('trombones', I.brass_section, chord_hits(pA + pA2, [1, 3], 53, 64, 3, 0.45, 0.45), kind='trombone',
           players=1, vib=0)
    s.play('tuba', I.brass, bass_notes(pI, [(0, 1, 'B', 0.9), (1, 1, 'B', 0.5), (2, 1, '5', 0.7), (3, 1, 'B', 0.5)],
                                       1, 0.7), kind='tuba', legato=0.7)

    # strings ostinato (8ths) + violins 16ths in A'
    ost = ['R', 'R', '5', '5', '8', '8', '5', '5']
    s.play('strings', I.strings, arp_notes(pI[2:] + pA + pB + pA2, ost, 0.5, 3, 0.55, 0.15), sect='vla',
           marc=True, legato=0.8)
    s.play('vln', I.strings, arp_notes(pA2, ['R', '3', '5', '8'], 0.25, 4, 0.5, 0.15), sect='vln', marc=True,
           legato=0.8)
    s.play('vln', I.strings, pad_notes(pB, 65, 81, 3, 0.45), sect='vln', attack=0.3)

    # snare cadences
    cad = ['X.xxX.x.X.xxX.xx', 'X.x.X.xxX.x.xxxx', 'X.xxX.x.X.xxX.x.', 'X.x.X.x.XxxxXxxx']
    for k in range(28):
        if k < 2:
            pat = ['X..xX..xX.x.X...', 'X.xxX.xxX.x.xxxx'][k]
        elif 12 <= k < 20:
            pat = ['X...x.x.X...x.xx', 'X...x.x.X.x.xxxx'][k % 2] if k != 19 else 'XxxxXxxxXXXXXXXX'
        else:
            pat = cad[k % 4]
        if k in (11, 27):
            pat = 'X.xxX.xxXxxxXXXX'
        s.pattern('snare', I.snare, pat, B(k), 0.25, vels={'X': 0.85, 'x': 0.45}, kind='march')
    for k in range(2, 28):
        s.pattern('bd', I.kick, 'X.......x.......', B(k), 0.25, kind='march')
    for b in (4, 12, 20):
        s.hit('cym', I.cymbal, B(b), 0.9, decay=1.8)
    for k in range(20, 28):
        s.pattern('cym', I.cymbal, '....x.......x...', B(k), 0.25, decay=0.25, vels={'x': 0.4})

    # timpani
    for (b, d, sym) in pI + pA + pA2:
        from sequencer import root_note
        r = root_note(sym, 2)
        while r > 55:
            r -= 12
        while r < 41:
            r += 12
        s.hit('timp', I.timpani, b, 0.6, f=float(dsp.mtof(r)))
    s.add('timp', B(19), I.timp_roll(float(dsp.mtof(48)), s.secs(4), 0.2, 0.9, seed=3, end_hit=False))
    s.add('timp', B(3, 2), I.timp_roll(float(dsp.mtof(48)), s.secs(2), 0.2, 0.7, seed=4, end_hit=False))

    return s.mixdown(peak_db=-3.0, drive_db=2.0, master_fx=[lambda z: dsp.hshelf(z, 5000, 1.5)])
