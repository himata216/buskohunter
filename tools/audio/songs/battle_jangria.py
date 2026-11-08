"""music.battle_jangria — ジャングリア戦. Heavy & evil (hell / lava). E Phrygian/minor,
170 BPM, 68 bars (96 s), LOOP.
Double-tracked distorted guitars (KS strings through drive + cab sim), double-kick,
low brass theme, dissonant reed organ, screaming tremolo violins, low 'oh' choir, hell bell.
Form: Intro(4) Riff(8) Theme(16) Breakdown(8) Tremolo/blast(8) Theme'(16) Riff-outro(8)."""
import instruments as I
import dsp
from sequencer import Song, prog, mel, shift, pad_notes, bass_notes, voicing

BPM = 170
BARS = 68
PITCH = {'e': 40, 'f': 41, 'g': 43, 'a': 45, 'b': 46, 'h': 47, 'c': 48, 'd': 50}


def parse_riff(pat, start, step=0.25):
    """chars: lowercase palm-muted chug, uppercase open accent, '-' sustain, '.' rest"""
    out = []
    pat = pat.replace(' ', '')
    i = 0
    while i < len(pat):
        c = pat[i]
        if c.lower() in PITCH:
            j = i + 1
            while j < len(pat) and pat[j] == '-':
                j += 1
            out.append((start + i * step, (j - i) * step, PITCH[c.lower()], c.isupper()))
            i = j
        else:
            i += 1
    return out


def render():
    s = Song(BPM, BARS, loop=True, seed=81, name='jangria')
    s.ir = dsp.make_ir(2.2, pre=0.02, hf=0.4, seed=91)
    s.room_ir = dsp.make_ir(0.5, pre=0.003, hf=0.4, er_span=0.02, seed=92)
    B = s.bar

    s.stem('gtrL', gain=0.62, pan=-0.85, rev=0.05)
    s.stem('gtrR', gain=0.62, pan=0.85, rev=0.05)
    s.stem('lead', gain=0.6, pan=0.1, rev=0.25, dly=0.12)
    s.stem('bass', gain=0.5, pan=0.0, rev=0.0)
    s.stem('kick', gain=0.36, pan=0.0, rev=0.0, room=0.08)
    s.stem('snare', gain=0.7, pan=0.0, rev=0.15, room=0.3)
    s.stem('cym', gain=0.75, pan=0.25, rev=0.12, width=1.3)
    s.stem('toms', gain=0.7, pan=0.0, rev=0.15, room=0.2)
    s.stem('brass', gain=0.6, pan=-0.1, rev=0.3)
    s.stem('organ', gain=0.75, pan=0.0, rev=0.4, width=1.6)
    s.stem('scream', gain=0.75, pan=0.2, rev=0.4, width=1.4)
    s.stem('choir', gain=1.8, pan=0.0, rev=0.45, width=1.5,
           fx=[lambda z: dsp.formant(z, 'bass', 'o', shift=0.9, bw_scale=1.2)])
    s.stem('bell', gain=1.1, pan=-0.3, rev=0.5)

    chugs = []      # (beat, dur, midi, open)

    def riff_bars(bars, b0):
        for k, pat in enumerate(bars):
            chugs.extend(parse_riff(pat, B(b0 + k)))

    rA = ['e.ee.e.eE---f.e.', 'e.ee.e.eG---F---', 'e.ee.e.eE---f.e.', 'B---A---G---F---']
    riff_bars(rA * 2, 4)
    gal = 'x.xxx.xxx.xxx.xx'
    theme_roots = ['e', 'b', 'f', 'e', 'e', 'c', 'af', 'e']
    for half in (12, 20, 44, 52):
        bars = []
        for r in theme_roots:
            if len(r) == 1:
                bars.append(gal.replace('x', r))
            else:
                bars.append(gal[:8].replace('x', r[0]) + gal[8:].replace('x', r[1]))
        if half in (20, 52):
            bars[-1] = 'E-------e.e.eeee'
        riff_bars(bars, half)
    rC = ['E-----e..e..e...', 'E-----e..e..G---', 'E-----e..e..e.e.', 'B-------A-------']
    riff_bars(rC * 2, 28)
    rD = ['eeeeeeeeeeeeeeee', 'ffffffffffffffff', 'eeeeeeeeeeeeeeee', 'ffffffffffffffff',
          'eeeeeeeeeeeeeeee', 'cccccccccccccccc', 'eeeeeeeeffffffff', 'E---------------']
    riff_bars(rD, 36)
    riff_bars(rA * 2, 60)

    for side, st, det in (('L', 'gtrL', -4.0), ('R', 'gtrR', 4.0)):
        for (b, d, m, op) in chugs:
            s.play(st, I.guitar_chug, [(b, d, m, 0.95 if op else 0.85)], palm=not op, detune=det,
                   jitter_ms=3 if side == 'L' else 4)
    # bass follows the chugs one octave down (distorted synth bass)
    s.play('bass', I.syn_bass, [(b, d, m - 12, 0.9) for (b, d, m, op) in chugs], cut=380, env_amt=900, drive=3.0)

    # ---------------- drums
    def kick_from(b0, bars):
        for (b, d, m, op) in chugs:
            if B(b0) <= b < B(b0 + bars):
                s.hit('kick', I.kick, b, 0.9 if op else 0.8, kind='metal', jitter_ms=1.5)

    kick_from(4, 8)
    kick_from(60, 8)
    kick_from(28, 8)
    for k in range(4, 12):
        s.pattern('snare', I.snare, '........X.......', B(k), 0.25)
        s.pattern('cym', I.hat, 'x.x.x.x.x.x.x.x.', B(k), 0.25, vels={'x': 0.5})
    for k in list(range(60, 68)):
        s.pattern('snare', I.snare, '........X.......' if k < 67 else '....X...X.X.XXXX', B(k), 0.25)
        s.pattern('cym', I.hat, 'x.x.x.x.x.x.x.x.', B(k), 0.25, vels={'x': 0.5})
    for b0 in (12, 44):  # theme: double-time, 16th double kick
        for k in range(16):
            s.pattern('kick', I.kick, 'xxxxxxxxxxxxxxxx', B(b0 + k), 0.25, kind='metal', vels={'x': 0.78})
            s.pattern('snare', I.snare, '....X.......X...' if k % 8 != 7 else '....X.......X.XX', B(b0 + k), 0.25)
            s.pattern('cym', I.cymbal, 'x...x...x...x...', B(b0 + k), 0.25, decay=0.35, kind='ride',
                      vels={'x': 0.45})
    for k in range(8):  # breakdown
        s.pattern('snare', I.snare, '........X.......', B(28 + k), 0.25)
        s.pattern('cym', I.cymbal, 'X.......X.......', B(28 + k), 0.25, decay=0.9, kind='china',
                  vels={'X': 0.7})
    for k in range(8):  # blast
        s.pattern('kick', I.kick, 'xxxxxxxxxxxxxxxx', B(36 + k), 0.25, kind='metal', vels={'x': 0.75})
        s.pattern('snare', I.snare, '.x.x.x.x.x.x.x.x', B(36 + k), 0.25, vels={'x': 0.6})
        s.pattern('cym', I.cymbal, 'x.x.x.x.x.x.x.x.', B(36 + k), 0.25, decay=0.4, kind='ride', vels={'x': 0.4})
    for b in (4, 12, 20, 28, 36, 44, 52, 60):
        s.hit('cym', I.cymbal, B(b), 0.95, decay=1.6)
        s.hit('cym', I.cymbal, B(b), 0.6, decay=1.2, kind='china', pan=-0.4)
    # intro fill (toms) + kick
    s.pattern('toms', I.tom, 'X.x.X.x.', B(3), 0.25, f=140.0)
    s.pattern('toms', I.tom, 'X.x.X.xx', B(3, 2), 0.25, f=95.0)
    s.pattern('toms', I.tom, 'xxxxXXXX', B(43, 2), 0.25, f=110.0)
    s.pattern('kick', I.kick, 'X.......X...X.X.', B(2), 0.25, kind='metal')
    s.pattern('kick', I.kick, 'X.X.X.X.XXXXXXXX', B(3), 0.25, kind='metal')

    # ---------------- low brass theme
    T1 = ('E3:1.5 F3:.5 G3:1 B3:1 | Bb3:2 A3:1 G3:1 | F3:1.5 G3:.5 F3:1 E3:1 | E3:4 | '
          'E3:1.5 F3:.5 G3:1 B3:1 | C4:2 B3:1 Bb3:1 | A3:1 G3:1 F3:1 D3:1 | E3:4')
    T2 = ('B3:1.5 C4:.5 D4:1 E4:1 | F4:2 E4:1 D4:1 | C4:1.5 D4:.5 C4:1 B3:1 | B3:4 | '
          'E4:1.5 F4:.5 G4:1 B4:1 | C5:2 B4:1 Bb4:1 | A4:1 G4:1 F4:1 D#4:1 | E4:4')
    for b0, v in ((12, 0.85), (44, 1.0)):
        m1, m2 = mel(T1, B(b0), v), mel(T2, B(b0 + 8), v)
        s.play('brass', I.brass_section, m1, kind='trombone', players=3)
        s.play('brass', I.brass, shift(m1, 0, -12), kind='tuba')
        s.play('brass', I.brass_section, m2, kind='horn', players=3)
        s.play('brass', I.brass_section, shift(m2, 0, -12), kind='trombone', players=2)
        if b0 == 44:
            s.play('brass', I.brass_section, shift(m1, 0, 12), kind='horn', players=3)
            s.play('brass', I.brass_section, shift(m2, 0, 12, 0.85), kind='trumpet', players=2)
    # riff stabs: low brass "blat" on open hits
    s.play('brass', I.brass_section, [(b, min(d, 1.0), m + 12, 0.8) for (b, d, m, op) in chugs
                                      if op and (B(4) <= b < B(12) or B(60) <= b)], kind='trombone', players=2)

    # ---------------- tremolo lead (D)
    TD = ('E4:1 F4:1 G#4:1 A4:1 | B4:2 C5:1 B4:1 | A4:1 G#4:1 F4:1 E4:1 | F4:4 | E4:1 F4:1 G#4:1 B4:1 | '
          'C5:2 D5:1 C5:1 | B4:1 A4:1 G#4:1 F4:1 | E4:4')
    trem = []
    for (b, d, m, v) in mel(TD, B(36), 0.85):
        k = 0
        while k * 0.25 < d - 1e-6:
            trem.append((b + k * 0.25, 0.22, m, v if k % 4 == 0 else v * 0.8))
            k += 1
    s.play('lead', I.guitar_lead, trem, jitter_ms=2)
    s.play('lead', I.guitar_lead, shift(trem, 0, 12, 0.6), jitter_ms=2)

    # ---------------- organ (dissonant), choir, screams, bell
    s.play('organ', I.organ, [(B(0), 16, m, 0.7) for m in (52, 53, 58, 59)], reg='reed', trem=0.25, detune_c=9,
           attack=1.5, release=1.0)
    s.play('organ', I.organ, [(B(28), 32, m, 0.6) for m in (40, 46, 52, 53)], reg='reed', trem=0.3, detune_c=10,
           attack=0.8, release=1.0)
    ptheme = prog('Em | Bb | F | Em | Em | C | Am F | Em', B(12)) + prog('Em | Bb | F | E | Em | C | Am F | Em', B(20))
    ptheme2 = prog('Em | Bb | F | Em | Em | C | Am F | Em', B(44)) + prog('Em | Bb | F | E | Em | C | Am F | Em', B(52))
    s.play('organ', I.organ, pad_notes(ptheme + ptheme2, 52, 71, 4, 0.6), reg='reed', detune_c=7, trem=0.15)
    s.play('choir', I.choir_src, pad_notes(ptheme2, 40, 59, 3, 0.75), attack=0.3, voices=4)
    s.play('choir', I.choir_src, pad_notes(prog('Em | Em | Em | Em | F | F | F | F', B(28)), 40, 55, 3, 0.6),
           attack=0.8, voices=4)
    # screaming violins: minor 2nds, tremolo, rising gliss into sections
    for b0, ln in ((2, 2), (26, 2), (34, 2), (42, 2), (58, 2), (66, 2)):
        s.play('scream', I.strings, [(B(b0), ln * 4, 88, 0.9), (B(b0), ln * 4, 89, 0.85)], tremolo=0.8,
               gliss=4, attack=0.4, release=0.2)
    s.play('scream', I.strings, [(B(28), 32, 76, 0.6), (B(28), 32, 77, 0.6)], tremolo=0.6, attack=2.0)
    for b in (0, 2, 28, 32):
        s.play('bell', I.bell, [(B(b), 8, 40, 0.9), (B(b), 8, 46, 0.7)], kind='church')

    return s.mixdown(peak_db=-3.0, drive_db=3.5, master_fx=[lambda z: dsp.peq(z, 300, -1.5, 0.8)])
