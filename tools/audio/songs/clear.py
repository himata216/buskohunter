"""music.clear — クエストクリア (one-shot ~9.5 s). C major, 120 BPM.
'ta-ta-ta-TAAA' trumpet fanfare over C -> F -> Ab Bb -> big C major tutti
with cymbals, timpani roll and glockenspiel sparkle."""
import instruments as I
import dsp
from sequencer import Song, mel, voicing, shift, harmonize, prog

BPM = 120


def render():
    s = Song(BPM, 4, loop=False, tail=3.0, seed=141, name='clear')
    s.ir = dsp.make_ir(2.0, pre=0.02, hf=0.55, seed=151)
    s.stem('tp', gain=0.45, pan=0.15, rev=0.3)
    s.stem('horns', gain=0.7, pan=-0.2, rev=0.32)
    s.stem('low', gain=0.8, pan=0.0, rev=0.25)
    s.stem('strings', gain=0.6, pan=0.0, rev=0.35, width=1.4)
    s.stem('perc', gain=0.85, pan=0.0, rev=0.25)
    s.stem('cym', gain=0.65, pan=0.2, rev=0.3)
    s.stem('glock', gain=1.1, pan=0.3, rev=0.4)

    p = prog('C | F | Ab Bb | C', 0)
    lead = mel('G4:1/3 G4:1/3 G4:1/3 C5:1.5 E5:.5 G5:1 | F5:1/3 F5:1/3 F5:1/3 A5:1.5 G5:.5 F5:1 | '
               'Ab4:.5 C5:.5 Eb5:.5 Ab5:.5 Bb4:.5 D5:.5 F5:.5 Bb5:.5 | C6:4!', 0, 0.9)
    s.play('tp', I.brass_section, lead, kind='trumpet', players=2)
    s.play('tp', I.brass_section, harmonize(lead[:-1], p), kind='trumpet', players=1)
    for (b, d, sym) in p[:-1]:
        s.play('horns', I.brass_section, [(b, d, m, 0.65) for m in voicing(sym, 55, 69, 3)], kind='horn', players=2)
        s.play('low', I.brass, [(b, d, voicing(sym, 36, 47, 1)[0], 0.7)], kind='tuba')
        s.play('strings', I.strings, [(b, d, m + 12, 0.5) for m in voicing(sym, 55, 69, 3)], tremolo=0.5, attack=0.05)
    # final C major tutti (beat 12)
    T = 12.0
    s.play('tp', I.brass_section, [(T, 4.0, m, 0.95) for m in (72, 76, 79)], kind='trumpet', players=2)
    s.play('horns', I.brass_section, [(T, 4.0, m, 0.9) for m in (60, 64, 67, 72)], kind='horn', players=3)
    s.play('low', I.brass_section, [(T, 4.0, m, 0.9) for m in (48, 55, 52)], kind='trombone', players=2)
    s.play('low', I.brass, [(T, 4.0, 36, 0.95)], kind='tuba')
    s.play('strings', I.strings, [(T, 4.0, m, 0.75) for m in (60, 67, 72, 76, 79, 84)], attack=0.03, marc=True)
    s.play('glock', I.bell, [(T + i * 0.125, 2.5, m, 0.6) for i, m in enumerate((72, 76, 79, 84, 88, 91, 96))],
           kind='glock')
    # percussion
    for k in range(3):
        s.pattern('perc', I.snare, 'x.x.x.x.x.x.xxxx' if k < 2 else 'xxxxxxxxxxxxXXXX', k * 4, 0.25, kind='march',
                  vels={'x': 0.35, 'X': 0.6})
    for b, f in ((0, 48), (4, 41), (8, 44), (10, 46)):
        s.hit('perc', I.timpani, b, 0.8, f=float(dsp.mtof(f)))
    s.add('perc', 10.5, I.timp_roll(float(dsp.mtof(43)), s.secs(1.5), 0.3, 0.8, seed=3, end_hit=False))
    s.hit('perc', I.timpani, T, 1.0, f=float(dsp.mtof(36)))
    s.hit('perc', I.gran_cassa, T, 0.9)
    s.hit('cym', I.cymbal, 0, 0.8, decay=1.4)
    s.hit('cym', I.cymbal, T, 1.0, decay=2.5)
    s.hit('cym', I.cymbal, T + 0.02, 0.7, decay=2.0, pan=-0.4)
    return s.mixdown(peak_db=-3.0, drive_db=2.0, max_sec=9.6, fade_sec=1.6)
