"""music.result — 報酬画面. Calm, accomplished. G major, 80 BPM, 16 bars (48 s), LOOP.
Flute melody, harp arpeggios, warm strings, celesta sparkles, soft horn counter-line,
pizzicato bass. Form: A(8) B(8) -> back to A."""
import instruments as I
import dsp
from sequencer import Song, prog, mel, shift, harmonize, arp_notes, pad_notes, bass_notes

BPM = 80
BARS = 16


def render():
    s = Song(BPM, BARS, loop=True, seed=181, name='result')
    s.ir = dsp.make_ir(2.4, pre=0.025, hf=0.5, seed=191)
    B = s.bar
    pA = prog('G | D/F# | Em | C | G/B | Am7 | D7sus4 D7 | G', B(0))
    pB = prog('C | D | Bm | Em | C | D/F# | Am7 D7 | G', B(8))
    allp = pA + pB
    s.stem('flute', gain=1.0, pan=0.12, rev=0.4)
    s.stem('horn', gain=1.0, pan=-0.2, rev=0.4)
    s.stem('harp', gain=1.2, pan=-0.3, rev=0.4, width=1.3)
    s.stem('strings', gain=0.7, pan=0.0, rev=0.45, width=1.5)
    s.stem('bass', gain=0.9, pan=0.0, rev=0.15)
    s.stem('celesta', gain=1.4, pan=0.35, rev=0.45)

    mA = mel('B4:1 D5:1 G5:1.5 F#5:.5 | F#5:1 E5:.5 D5:.5 A4:2 | G4:1 B4:1 E5:1.5 D5:.5 | C5:1 B4:.5 A4:.5 E4:2 | '
             'D5:1 B4:1 D5:1 G5:1 | E5:1.5 D5:.5 C5:1 A4:1 | G4:1 A4:1 C5:1 F#4:1 | G4:3 r:1', B(0), 0.7)
    mB = mel('E5:1.5 D5:.5 C5:1 G5:1 | F#5:1.5 E5:.5 D5:1 A5:1 | B5:1 A5:.5 F#5:.5 D5:2 | E5:1 G5:1 B5:1.5 A5:.5 | '
             'G5:1.5 E5:.5 C5:1 E5:1 | D5:1.5 A4:.5 F#4:1 A4:1 | C5:1 E5:1 D5:1 C5:1 | B4:3 r:1', B(8), 0.72)
    s.play('flute', I.flute, mA + mB)
    s.play('flute', I.flute, shift(harmonize(mB, pB), 0, 0, 0.7), kind='ocarina')
    s.play('horn', I.brass_section, mel('G3:4 | F#3:4 | G3:4 | E3:4 | D3:4 | E3:4 | C4:2 C4:2 | B3:4', B(8), 0.45),
           kind='horn', players=2, attack=0.25, vib=2)
    s.play('harp', I.harp, arp_notes(allp, ['R', '5', '8', '10', '12', '10', '8', '5'], 0.5, 3, 0.45), jitter_ms=4)
    s.play('strings', I.strings, pad_notes(allp, 55, 74, 4, 0.38), attack=0.6, release=0.9, sect='vla')
    s.play('bass', I.upright, bass_notes(allp, [(0, 2, 'B', 1.0), (2, 2, '5', 0.7)], 2, 0.7), t60=1.6)
    cel = mel('r:3 D6:.5 G6:.5 | r:4 | r:3 B5:.5 E6:.5 | r:4 | r:3 D6:.5 G6:.5 | r:4 | r:2 A6:.5 F#6:.5 D6:.5 C6:.5 | '
              'B5:4', B(0), 0.5)
    s.play('celesta', I.bell, cel + shift(cel, B(8), 0), kind='celesta')
    return s.mixdown(peak_db=-3.0, drive_db=1.0, master_fx=[lambda z: dsp.hshelf(z, 4500, 3.5)])
