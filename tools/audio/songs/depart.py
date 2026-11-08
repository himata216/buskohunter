"""music.depart — 出発ファンファーレ (one-shot ~7 s). F major, 96 BPM.
Horn call -> trumpet answer -> C7 with timpani roll crescendo -> tutti F major hit."""
import instruments as I
import dsp
from sequencer import Song, mel, voicing

BPM = 96


def render():
    s = Song(BPM, 3, loop=False, tail=2.4, seed=5, name='depart')
    s.ir = dsp.make_ir(2.0, pre=0.02, hf=0.5, seed=8)
    s.stem('horns', gain=1.2, pan=-0.2, rev=0.32)
    s.stem('trumpets', gain=0.85, pan=0.22, rev=0.3)
    s.stem('low', gain=0.9, pan=0.05, rev=0.25)
    s.stem('strings', gain=0.6, pan=0.0, rev=0.35, width=1.4)
    s.stem('perc', gain=1.0, pan=0.0, rev=0.3)
    s.stem('cym', gain=0.6, pan=0.25, rev=0.3)

    s.play('horns', I.brass_section, mel('C4:.5 F4:.5 A4:.5 C5:1.5!', 0, 0.85), kind='horn', players=3)
    s.play('horns', I.brass_section, mel('F3:3', 0, 0.55), kind='horn', players=2)
    s.play('trumpets', I.brass_section, mel('D5:.5 F5:.5 Bb5:.5 A5:.5 G5:.5 E5:.5 G5:1', 3, 0.85), kind='trumpet',
           players=2)
    # chords under the answer: Bb (beats 3-4.5) -> C7 (4.5-6.5)
    for m in voicing('Bb', 50, 65, 3):
        s.play('horns', I.brass_section, [(3, 1.5, m, 0.6)], kind='horn', players=2, vib=2)
    for m in voicing('C7', 52, 67, 4):
        s.play('horns', I.brass_section, [(4.5, 2.0, m, 0.7)], kind='horn', players=2, vib=2)
        s.play('strings', I.strings, [(4.5, 2.0, m + 12, 0.6)], tremolo=0.6, attack=0.1)
    s.play('low', I.brass, [(3, 1.5, 34, 0.7), (4.5, 2.0, 36, 0.8)], kind='tuba')
    s.add('perc', 4.5, I.timp_roll(float(dsp.mtof(48)), s.secs(2.0), 0.12, 0.6, seed=2, end_hit=False), gain=0.8)
    s.add('cym', 4.5, I.cymbal(0.35, 1, decay=0.6, swell=s.secs(2.0)), gain=0.7)

    # final tutti F major (beat 6.5)
    T = 6.5
    s.play('trumpets', I.brass_section, [(T, 3.4, m, 0.95) for m in (77, 81, 84)], kind='trumpet', players=2)
    s.play('horns', I.brass_section, [(T, 3.4, m, 0.9) for m in (65, 69, 72)], kind='horn', players=3)
    s.play('low', I.brass_section, [(T, 3.4, m, 0.9) for m in (53, 60, 57)], kind='trombone', players=2)
    s.play('low', I.brass, [(T, 3.4, 41, 0.95)], kind='tuba')
    s.play('strings', I.strings, [(T, 3.4, m, 0.8) for m in (65, 69, 72, 77, 81)], attack=0.03, marc=True)
    s.hit('perc', I.timpani, T, 1.0, f=float(dsp.mtof(41)))
    s.hit('perc', I.gran_cassa, T, 0.9)
    s.hit('cym', I.cymbal, T, 1.0, decay=2.2)
    s.hit('perc', I.snare, T - 0.25, 0.6, kind='march')
    s.hit('perc', I.snare, T - 0.125, 0.7, kind='march')
    return s.mixdown(peak_db=-3.0, drive_db=2.0, max_sec=7.6, fade_sec=1.4)
