"""music.battle_busaiber — ブサイバー戦. Cyberpunk EDM / synthwave, F minor, 150 BPM,
64 bars (102.4 s), LOOP.
16th arps through dotted-8th ping-pong delay, supersaw lead with glide, sidechained
saw bass & pads, 909 four-on-the-floor + clap, glitches, risers and a robot
'POSE YOUR GAME' vocal.
Form: Intro(8) Build(8) Drop(16) Break(8) Drop2(16) Outro(8)."""
import numpy as np

import instruments as I
import dsp
from sequencer import Song, prog, mel, shift, arp_notes, pad_notes, bass_notes

BPM = 150
BARS = 64


def render():
    s = Song(BPM, BARS, loop=True, seed=101, name='busaiber')
    s.ir = dsp.make_ir(2.4, pre=0.02, hf=0.55, seed=111)
    s.delay_beats = 0.75
    s.delay_fb = 0.38
    B = s.bar
    P = 'Fm | Db | Ab | Eb'
    allp = prog(' | '.join([P] * 16), B(0))
    seg = lambda b0, b1: [c for c in allp if B(b0) <= c[0] < B(b1)]
    duck = s.duck_curve(1.0, 0.65, 0.004, 0.24, active=[(B(8), B(32)), (B(40), B(64))])
    duck_l = s.duck_curve(1.0, 0.35, 0.004, 0.2, active=[(B(8), B(32)), (B(40), B(64))])

    s.stem('kick', gain=0.42, pan=0.0, rev=0.0)
    s.stem('clap', gain=0.8, pan=0.0, rev=0.25)
    s.stem('hats', gain=1.1, pan=0.2, rev=0.05, width=1.3)
    s.stem('bass', gain=0.6, pan=0.0, rev=0.0)
    s.stem('arp', gain=0.7, pan=-0.2, rev=0.15, dly=0.35, width=1.3)
    s.stem('pad', gain=1.25, pan=0.0, rev=0.35, width=1.7)
    s.stem('lead', gain=1.35, pan=0.05, rev=0.25, dly=0.22, width=1.2)
    s.stem('fx', gain=0.6, pan=0.0, rev=0.3, width=1.5)
    s.stem('glitch', gain=0.55, pan=0.0, rev=0.1, dly=0.15)
    s.stem('voice', gain=1.3, pan=0.0, rev=0.3, dly=0.25,
           fx=[lambda z: dsp.crush(z, 7), lambda z: dsp.hp(z, 120, 2)])
    for st in ('bass', 'pad'):
        s.stems[st].duck = duck
    s.stems['arp'].duck = duck_l
    s.stems['lead'].duck = duck_l

    # ---------------- drums
    for k in range(8, 64):
        if 32 <= k < 40:
            continue
        s.pattern('kick', I.kick, 'X...X...X...X...', B(k), 0.25, kind='909', jitter_ms=0)
        if k >= 16 or k >= 10:
            s.pattern('clap', I.clap, '....X.......X...', B(k), 0.25, jitter_ms=1)
            s.pattern('clap', I.snare, '....x.......x...', B(k), 0.25, kind='edm', vels={'x': 0.55}, jitter_ms=1)
        s.pattern('hats', I.hat, '..X...X...X...X.', B(k), 0.25, open_=True, decay=0.12, vels={'X': 0.5})
        if 16 <= k < 32 or 40 <= k < 56:
            s.pattern('hats', I.hat, 'xoxoxoxoxoxoxoxo', B(k), 0.25, vels={'x': 0.45, 'o': 0.25})
    for k in range(4, 8):
        s.pattern('hats', I.hat, 'x.o.x.o.x.o.x.oo', B(k), 0.25, vels={'x': 0.4, 'o': 0.22})
    # snare rolls into drops
    for b0 in (14, 38):
        s.pattern('clap', I.snare, 'x...x...x...x...', B(b0), 0.25, kind='edm', vels={'x': 0.4})
        s.pattern('clap', I.snare, 'x.x.x.x.x.x.x.x.', B(b0 + 1), 0.125, kind='edm', vels={'x': 0.55})
        s.pattern('clap', I.snare, 'xxxxxxxxxxxxxxxx', B(b0 + 1, 2), 0.125, kind='edm', vels={'x': 0.75})
    s.pattern('clap', I.snare, 'x.x.x.x.xxxxxxxx', B(7), 0.25, kind='edm', vels={'x': 0.4})
    for b in (8, 16, 24, 40, 48, 56):
        s.hit('fx', I.cymbal, B(b), 0.9, decay=1.8)
    for b in (16, 40):
        s.add('fx', B(b), I.impact(1.0, b, 2.5), gain=1.2)
    for b0, ln in ((6, 2), (14, 2), (38, 2)):
        s.add('fx', B(b0), I.riser(s.secs(ln * 4), seed=b0), gain=0.8)
        s.add('fx', B(b0), I.cymbal(0.5, b0, decay=0.4, swell=s.secs(ln * 4)), gain=0.8)

    # ---------------- bass (16th rolling, sidechained)
    roll = [(0, .25, 'R', 1.0), (.25, .25, 'R', 0.55), (.5, .25, '8', 0.8), (.75, .25, 'R', 0.55)]
    roll4 = [(o + q, d, deg, v) for q in range(4) for (o, d, deg, v) in roll]
    s.play('bass', I.syn_bass, bass_notes(seg(16, 32) + seg(40, 56), roll4, 1, 0.9), cut=500, env_amt=1800,
           decay=0.08, jitter_ms=0)
    eights = [(o, .5, 'R', 0.9) for o in (0, .5, 1, 1.5, 2, 2.5, 3, 3.5)]
    s.play('bass', I.syn_bass, bass_notes(seg(8, 16) + seg(56, 64), eights, 1, 0.85), cut=400, env_amt=1200,
           jitter_ms=0)
    s.play('bass', I.syn_bass, bass_notes(seg(32, 40), [(0, 4, 'R', 0.7)], 1, 0.7), cut=300, env_amt=300,
           jitter_ms=0)

    # ---------------- arp (16ths)
    deg = ['R', '8', '5', '8', '10', '8', '5', '8']
    s.play('arp', I.syn_pluck, arp_notes(seg(0, 8), deg, 0.25, 3, 0.45, 0.1), cut=(250, 1800), jitter_ms=0)
    s.play('arp', I.syn_pluck, arp_notes(seg(8, 16) + seg(56, 64), deg, 0.25, 3, 0.6, 0.1), cut=(300, 3500),
           jitter_ms=0)
    s.play('arp', I.syn_pluck, arp_notes(seg(16, 32) + seg(40, 56), deg, 0.25, 3, 0.7, 0.1), cut=(400, 6000),
           jitter_ms=0)
    s.play('arp', I.syn_pluck, arp_notes(seg(40, 56), ['10', '12', '15' if False else '8', '12'], 0.25, 4, 0.45),
           cut=(500, 7000), wave='square', jitter_ms=0, pan=0.4)
    s.play('arp', I.syn_pluck, arp_notes(seg(32, 40), ['R', None, '5', None, '8', None, '10', None], 0.25, 3, 0.4),
           cut=(200, 1200), jitter_ms=0)

    # ---------------- pads
    s.play('pad', I.pad_saw, pad_notes(allp, 56, 75, 4, 0.6), attack=0.35, release=0.9, cut=3200)

    # ---------------- lead
    L = ('C5:.75 Ab4:.75 F4:.5 C5:1 Eb5:1 | Db5:.75 C5:.75 Ab4:.5 F4:2 | Eb5:.75 C5:.75 Ab4:.5 Eb5:1 G5:1 | '
         'F5:.75 Eb5:.75 Bb4:.5 G4:2 | C5:.75 Ab4:.75 F4:.5 C5:1 F5:1 | Ab5:.75 F5:.75 Db5:.5 F5:1 Ab5:1 | '
         'G5:.75 Eb5:.75 C5:.5 Eb5:1 G5:1 | Bb5:1.5 G5:.5 F5:1 Eb5:1')
    L2 = ('C5:.75 Ab4:.75 F4:.5 C5:1 Eb5:1 | Db5:.75 C5:.75 Ab4:.5 F4:2 | Eb5:.75 C5:.75 Ab4:.5 Eb5:1 G5:1 | '
          'F5:.75 Eb5:.75 Bb4:.5 G4:2 | C6:.75 Ab5:.75 F5:.5 C6:1 Eb6:1 | Db6:.75 C6:.75 Ab5:.5 F5:2 | '
          'Eb6:.75 C6:.75 Ab5:.5 G5:1 Ab5:1 | G5:2 F5:1 G5:1')
    drop1 = mel(L, B(16), 0.9) + mel(L2, B(24), 0.95)
    drop2 = mel(L, B(40), 0.95) + mel(L2, B(48), 1.0)
    lead_buf = I.lead_line(drop1 + drop2, s.spb, s.L, seed=3, cut=6500)
    lead_oct = I.lead_line(shift(drop2, 0, 12, 0.8), s.spb, s.L, seed=4, cut=7000, sub=0.0)
    lead_brk = I.lead_line(mel(L, B(32), 0.6)[:14], s.spb, s.L, seed=5, cut=1400, vib_c=10)
    s.add('lead', 0, lead_buf + 0.45 * lead_oct + 0.8 * lead_brk)

    # ---------------- glitches & robot voice
    r = s.rng
    for b in (7.75, 15.75, 31.5, 31.75, 39.75, 47.75, 55.75, 63.5, 63.75):
        bar_ = int(b)
        s.add('glitch', B(bar_, (b - bar_) * 4), I.glitch_burst(s.secs(0.25), int(b * 100), 0.9),
              pan=float(r.uniform(-0.5, 0.5)))
    for k in range(18):
        bb = float(r.uniform(0, BARS * 4))
        s.add('glitch', bb, I.glitch_burst(r.uniform(0.04, 0.1), int(bb * 77), 0.5), pan=float(r.uniform(-0.8, 0.8)))
    # stutter-snare at the end of the drops
    for b0 in (31, 55):
        s.pattern('glitch', I.snare, 'xxxxxxxx', B(b0, 3), 0.0625, kind='edm', vels={'x': 0.5})
    words = [('p', ['o', 'u'], 'z'), ('', ['i', 'o', 'u'], 'r'), ('g', ['e', 'i'], 'm')]
    voice = I.robot_words(words, f0=float(dsp.mtof(41)), seed=7, syl=s.secs(0.5) * 0.8, gap=s.secs(0.5) * 0.2)
    voice_hi = I.robot_words(words, f0=float(dsp.mtof(53)), seed=8, syl=s.secs(0.5) * 0.8, gap=s.secs(0.5) * 0.2)
    vv = voice + 0.5 * dsp.pad_to(voice_hi, len(voice))
    for b in (33, 37):
        s.add('voice', B(b), vv)
    s.add('voice', B(15, 2), vv * 0.9)
    s.add('voice', B(39, 2), vv)

    return s.mixdown(peak_db=-3.0, drive_db=4.0, master_fx=[lambda z: dsp.hshelf(z, 8000, 1.0)])
