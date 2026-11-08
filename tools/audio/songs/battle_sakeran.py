"""music.battle_sakeran — サケラン戦. 和風祭囃子 x brass epic. D minor (min'yo flavour),
150 BPM, 60 bars (96 s), LOOP.
taiko / shime-daiko / kane (chan-chiki), shamisen riffs, shinobue, heroic brass,
kakegoe shouts (yo! / ha! / sore! / iyoo!).
Form: Intro taiko(4) Riff(8) Theme(16) Matsuri-shinobue(8) Build(8) Theme'(8+4) Riff-turn(4)."""
import instruments as I
import dsp
from sequencer import (Song, prog, mel, shift, harmonize, arp_notes, pad_notes, bass_notes, chord_hits,
                       chord_tone, root_note, voicing)

BPM = 150
BARS = 60

# shamisen riff templates: (interval from chord root, dur in beats)
RIFF_MIN = [(0, .5), (0, .25), (3, .25), (5, .5), (7, .5), (10, .5), (7, .25), (5, .25), (7, .5), (0, .5)]
RIFF_MIN2 = [(0, .5), (0, .25), (3, .25), (5, .5), (7, .25), (5, .25), (3, .5), (5, .5), (3, .25), (0, .25), (-2, .5)]
RIFF_MAJ = [(0, .5), (0, .25), (4, .25), (7, .5), (9, .5), (12, .5), (9, .25), (7, .25), (4, .5), (7, .5)]


def riff(progression, octave=3, vel=0.8, alt=True):
    out = []
    for i, (b, d, sym) in enumerate(progression):
        r = root_note(sym, octave)
        if r < 50:
            r += 12
        minor = 'm' in sym.replace('maj', '')
        tpl = RIFF_MAJ if not minor else (RIFF_MIN2 if (alt and i % 2) else RIFF_MIN)
        t = b
        k = 0
        while t < b + d - 1e-6:
            iv, du = tpl[k % len(tpl)]
            v = vel + (0.15 if abs((t - b) % 1) < 1e-6 else 0)
            out.append((t, du, r + iv, v))
            t += du
            k += 1
    return out


def render():
    s = Song(BPM, BARS, loop=True, seed=41, name='sakeran')
    s.ir = dsp.make_ir(1.8, pre=0.015, hf=0.5, seed=51)
    s.room_ir = dsp.make_ir(0.6, pre=0.004, hf=0.45, er_span=0.025, seed=52)
    B = s.bar
    pRiff = 'Dm | Dm | Bb | C | Dm | Dm | Bb | C'
    pTheme = 'Dm | Bb | C | Dm | Dm | Bb | Gm A | Dm'
    p1 = prog(pRiff, B(4))
    p2 = prog(pTheme, B(12)) + prog(pTheme, B(20))
    p3 = prog('Dm | C | Bb | Am | Dm | C | Bb C | Dm', B(28))
    p4 = prog('Bb | C | Dm | Dm | Bb | C | A | A', B(36))
    p5 = prog(pTheme, B(44)) + prog('Bb | C | Dm | Dm', B(52))
    p6 = prog('Dm | Dm | Bb | C', B(56))

    s.stem('brass_hi', gain=0.75, pan=0.18, rev=0.28)
    s.stem('brass_lo', gain=0.85, pan=-0.18, rev=0.25)
    s.stem('shamisen', gain=1.1, pan=-0.3, rev=0.15)
    s.stem('shamisen2', gain=1.3, pan=0.35, rev=0.15)
    s.stem('fue', gain=0.9, pan=0.1, rev=0.3)
    s.stem('strings', gain=0.5, pan=0.0, rev=0.3, width=1.4)
    s.stem('bass', gain=0.9, pan=0.0, rev=0.08)
    s.stem('taiko', gain=0.36, pan=0.0, rev=0.12, room=0.35)
    s.stem('shime', gain=0.85, pan=0.25, rev=0.08, room=0.25)
    s.stem('kane', gain=0.55, pan=-0.35, rev=0.15)
    s.stem('voice', gain=1.5, pan=0.0, rev=0.25, room=0.2, width=1.5)
    s.stem('cym', gain=0.45, pan=0.2, rev=0.25)

    # ---------- percussion grid
    def groove(b0, bars, heavy=False):
        for k in range(bars):
            bb = B(b0 + k)
            s.pattern('taiko', I.taiko, 'X..x..X.X..x..x.' if k % 2 == 0 else 'X..x..X.X.x.X.xx', bb, 0.25,
                      f=58.0, pan=-0.1, size=1.2)
            s.pattern('taiko', I.taiko, '....X.......X..x' if not heavy else '..x.X..x..x.X.xx', bb, 0.25,
                      f=92.0, pan=0.15, vels={'X': 0.85, 'x': 0.55})
            s.pattern('taiko', I.taiko_rim, '..x...x...x...x.', bb, 0.25, pan=0.3, vels={'x': 0.5})
            s.pattern('shime', I.shime, 'xoxoXoxoxoxoXoxo', bb, 0.25, vels={'X': 0.8, 'x': 0.55, 'o': 0.3})
            s.pattern('kane', I.kane, 'X.xX.xX.X.xX.xX.', bb, 0.25, vels={'X': 0.7, 'x': 0.4})
            s.pattern('kane', I.kane, '.x..x..x.x..x..x', bb, 0.25, vels={'x': 0.35}, damp=True)

    # intro: taiko solo building
    intro = ['X...X...X...X...', 'X..xX..xX..xX.xx', 'X.x.X.x.XxxxXxxx', 'XxxxXxxxXXXXXXXX']
    for k, pat in enumerate(intro):
        s.pattern('taiko', I.taiko, pat, B(k), 0.25, f=58.0 if k < 3 else 70.0, size=1.2,
                  vels={'X': 0.72 + 0.03 * k, 'x': 0.45 + 0.06 * k})
        s.pattern('shime', I.shime, 'x.x.x.x.x.x.x.x.' if k >= 1 else 'x...x...x...x...', B(k), 0.25,
                  vels={'x': 0.45 + 0.1 * k})
    # intro drone: low brass + tremolo strings on D, crescendo into the riff
    s.play('brass_lo', I.brass_section, [(B(0), 16, 38, 0.55), (B(0), 16, 45, 0.5), (B(0), 16, 50, 0.45)],
           kind='trombone', players=2, attack=3.5, vib=2)
    s.play('strings', I.strings, [(B(0), 16, 62, 0.6), (B(0), 16, 69, 0.55), (B(0), 16, 74, 0.5)], tremolo=0.6,
           attack=4.0, sect='vla')
    s.play('bass', I.upright, [(B(k), 1, 26, 0.9) for k in range(4)], t60=1.5)
    s.add('voice', B(0, 0), I.kakegoe('iyoo', 1, 0.9))
    s.add('voice', B(2, 3), I.kakegoe('yo', 2, 0.8))
    s.add('voice', B(3, 3.5), I.kakegoe('ha', 3, 1.0))
    s.add('voice', B(2, 0), I.kakegoe('sore', 9, 0.6))

    groove(4, 8)
    groove(12, 16)
    for k in range(8):  # matsuri section: lighter, kane-led
        bb = B(28 + k)
        s.pattern('taiko', I.taiko, 'X.......X.....x.', bb, 0.25, f=62.0, vels={'X': 0.7, 'x': 0.45})
        s.pattern('shime', I.shime, 'x.xxx.xxx.xxx.x.', bb, 0.25, vels={'x': 0.45})
        s.pattern('kane', I.kane, 'X.xX.xX.X.xX.xX.', bb, 0.25, vels={'X': 0.75, 'x': 0.45})
    for k in range(8):  # build
        bb = B(36 + k)
        s.pattern('taiko', I.taiko, 'X.x.X.x.X.x.X.x.' if k < 6 else 'XxxxXxxxXxxxXxxx', bb, 0.25, f=58.0, size=1.2,
                  vels={'X': 0.7 + 0.04 * k, 'x': 0.4 + 0.05 * k})
        s.pattern('shime', I.shime, 'xxxxxxxxxxxxxxxx', bb, 0.25, vels={'x': 0.3 + 0.06 * k})
    groove(44, 12, heavy=True)
    groove(56, 4)
    s.pattern('taiko', I.taiko, 'X.x.X.x.XxXxXXXX', B(59), 0.25, f=70.0, size=1.1)
    for b in (4, 12, 20, 28, 44, 52):
        s.hit('cym', I.cymbal, B(b), 0.85, decay=1.6)
    s.add('cym', B(42), I.cymbal(0.7, 5, decay=0.8, swell=s.secs(8)))

    # ---------- shamisen
    s.play('shamisen', I.shamisen, riff(p1 + p6, 3, 0.8), jitter_ms=3)
    s.play('shamisen', I.shamisen, riff(p2 + p5, 3, 0.62), jitter_ms=3)
    s.play('shamisen2', I.shamisen, riff(p1, 4, 0.5, alt=False), jitter_ms=4)
    # matsuri section: shamisen plays a light 8th accompaniment
    s.play('shamisen', I.shamisen, arp_notes(p3, ['R', '8', '5', '8', 'R', '8', '5', '10'], 0.5, 3, 0.55), jitter_ms=3)

    # ---------- brass
    theme = ('A4:1.5 D5:.5 D5:1 C5:.5 D5:.5 | F5:1.5 D5:.5 C5:1 Bb4:1 | G4:1.5 A4:.5 C5:1 D5:.5 C5:.5 | A4:3 r:1 | '
             'A4:1.5 D5:.5 D5:1 F5:.5 G5:.5 | Bb5:1.5 A5:.5 F5:1 D5:1 | G5:1 F5:.5 D5:.5 E5:1 C#5:1 | D5:3 r:1')
    t1 = mel(theme, B(12), 0.82, legato=0.95)
    t2 = mel(theme, B(20), 0.92, legato=0.95)
    s.play('brass_lo', I.brass_section, shift(t1, 0, -12), kind='horn', players=3)
    s.play('brass_lo', I.brass_section, shift(harmonize(t1, prog(pTheme, B(12))), 0, -12), kind='trombone', players=2)
    s.play('brass_hi', I.brass_section, t2, kind='trumpet', players=2)
    s.play('brass_lo', I.brass_section, shift(t2, 0, -12), kind='horn', players=3)
    s.play('brass_lo', I.brass_section, shift(harmonize(t2, prog(pTheme, B(20))), 0, -12), kind='trombone', players=2)
    t3 = mel(theme, B(44), 1.0, legato=0.95)
    s.play('brass_hi', I.brass_section, t3, kind='trumpet', players=3)
    s.play('brass_hi', I.brass_section, harmonize(t3, prog(pTheme, B(44))), kind='trumpet', players=2)
    s.play('brass_lo', I.brass_section, shift(t3, 0, -12), kind='horn', players=3)
    ext = mel('D5:1.5 C5:.5 Bb4:1 D5:1 | E5:1.5 D5:.5 C5:1 E5:1 | F5:2 E5:1 C5:1 | D5:4', B(52), 0.95)
    s.play('brass_hi', I.brass_section, ext, kind='trumpet', players=2)
    s.play('brass_lo', I.brass_section, shift(ext, 0, -12), kind='horn', players=3)
    # riff stabs ("jan! jan!")
    stab = chord_hits(p1 + p6, [0, 2.5], 57, 69, 3, 0.85, 0.4)
    s.play('brass_lo', I.brass_section, stab, kind='trombone', players=2, vib=0)
    s.play('brass_hi', I.brass_section, chord_hits(p1, [3.5], 62, 74, 3, 0.7, 0.3), kind='trumpet', players=1, vib=0)
    # build: brass swells
    for (b, d, sym) in p4:
        for m in voicing(sym, 55, 70, 4):
            s.play('brass_lo', I.brass_section, [(b, d, m, 0.75)], kind='horn', players=2, attack=0.5, vib=2)
    s.play('brass_hi', I.brass_section, mel('A4:1 C#5:1 E5:1 G5:1 | A5:4!', B(42), 0.9), kind='trumpet', players=3)

    # ---------- shinobue (matsuri section) + ornaments in theme rests
    fue = ('D5:1 F5:.5 G5:.5 A5:1 G5:.5 F5:.5 | G5:1.5 F5:.5 D5:1 C5:1 | D5:1 F5:1 G5:1 F5:.5 D5:.5 | C5:2 A4:2 | '
           'D5:.5 F5:.5 G5:.5 A5:.5 C6:1 A5:1 | G5:1 A5:.5 G5:.5 F5:1 D5:1 | F5:1 G5:1 A5:1 C6:.5 A5:.5 | D6:3 r:1')
    s.play('fue', I.flute, shift(mel(fue, B(28), 0.8), 0, 12), kind='shinobue')
    s.play('fue', I.flute, mel('r:3 A5:.25 C6:.25 D6:.5', B(15), 0.6) + mel('r:3 A5:.25 C6:.25 D6:.5', B(23), 0.6),
           kind='shinobue')
    s.play('fue', I.flute, shift(t3, 0, 12, 0.55), kind='shinobue')

    # ---------- strings & bass
    s.play('strings', I.strings, pad_notes(p2 + p5, 50, 69, 4, 0.45), sect='vla', attack=0.2)
    rise = []
    for k in range(8):
        rise += [(B(36 + k) + i * 0.25, 0.25, 62 + k + (i % 2) * 12, 0.45 + 0.05 * k) for i in range(16)]
    s.play('strings', I.strings, rise, marc=True, legato=0.9)
    s.play('strings', I.strings, [(B(42), 8, 81, 0.8), (B(42), 8, 85, 0.75)], tremolo=0.7, attack=0.3)
    oct8 = [(0, .5, 'R', 1.0), (.5, .5, '8', 0.6), (1, .5, 'R', 0.8), (1.5, .5, '8', 0.6), (2, .5, 'R', 0.9),
            (2.5, .5, '8', 0.6), (3, .5, '5', 0.8), (3.5, .5, '8', 0.6)]
    s.play('bass', I.upright, bass_notes(p1 + p2 + p4 + p5 + p6, oct8, 1, 0.85), t60=0.5)
    s.play('bass', I.upright, bass_notes(p3, [(0, 1, 'R', 1), (2, 1, '5', 0.8)], 1, 0.8), t60=0.8)

    # ---------- more kakegoe
    for b, w, v in ((7, 'ha', 1.0), (11, 'yo', 0.9), (15.75, 'yo', 0.9), (23.75, 'sore', 0.9), (27.5, 'iyoo', 0.8),
                    (35.75, 'ha', 0.9), (39, 'sore', 0.8), (41, 'sore', 0.9), (43, 'iyoo', 1.0), (47.75, 'ha', 1.0),
                    (51.75, 'yo', 0.9), (55, 'ha', 1.0), (59, 'yo', 0.9), (59.5, 'ha', 1.0)):
        bar_ = int(b)
        s.add('voice', B(bar_, (b - bar_) * 4 if b != bar_ else 3), I.kakegoe(w, int(b * 10), v),
              pan=float(s.rng.uniform(-0.3, 0.3)))

    return s.mixdown(peak_db=-3.0, drive_db=3.5, master_fx=[lambda z: dsp.hshelf(z, 6000, 1.0)])
