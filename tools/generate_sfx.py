#!/usr/bin/env python3
"""Generate royalty-free arcade SFX as 16-bit mono WAV files for the 2048 game.

All sounds are synthesized from scratch (additive sine synthesis with envelopes),
so there are no licensing concerns and nothing is downloaded.

Regenerate the in-app sounds from the repo root with:
    python3 tools/generate_sfx.py app/src/main/res/raw

To tweak or add a sound: edit the recipes in main(), then rerun the command above.
Each new file needs a matching R.raw entry in SoundManager.kt.
"""
import wave
import struct
import math
import os
import sys

SR = 44100  # sample rate


def env_edges(n, total, attack_s=0.004, release_s=0.02):
    """Linear attack + linear release to avoid clicks at the edges."""
    t = n / SR
    dur = total / SR
    a = min(1.0, t / attack_s) if attack_s > 0 else 1.0
    rel_start = dur - release_s
    r = 1.0
    if release_s > 0 and t > rel_start:
        r = max(0.0, (dur - t) / release_s)
    return a * r


def synth(segments, gain=0.72):
    """segments: list of dicts: freq, dur, glide_to?, harmonics?, decay?"""
    samples = []
    for seg in segments:
        dur = seg["dur"]
        total = int(SR * dur)
        f0 = seg["freq"]
        f1 = seg.get("glide_to", f0)
        harmonics = seg.get("harmonics", [(1, 1.0)])
        decay = seg.get("decay", 3.0)  # exponential body decay
        amp_sum = sum(a for _, a in harmonics)
        phase = 0.0
        for n in range(total):
            frac = n / total
            f = f0 * (1 - frac) + f1 * frac
            phase += 2 * math.pi * f / SR
            val = 0.0
            for mult, amp in harmonics:
                val += amp * math.sin(phase * mult)
            val /= amp_sum
            e = math.exp(-decay * frac) * env_edges(n, total)
            samples.append(val * e)
    peak = max(1e-6, max(abs(s) for s in samples))
    return [int(max(-1.0, min(1.0, s / peak * gain)) * 32767) for s in samples]


def write_wav(path, samples):
    with wave.open(path, "w") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(b"".join(struct.pack("<h", s) for s in samples))
    print(f"  wrote {path}  ({os.path.getsize(path)/1024:.1f} KB, {len(samples)/SR*1000:.0f} ms)")


def main():
    out_dir = sys.argv[1]
    os.makedirs(out_dir, exist_ok=True)

    # Soft slide / shoot: short low blip with a slight downward glide.
    move = synth([
        {"freq": 300, "glide_to": 210, "dur": 0.075, "decay": 4.5,
         "harmonics": [(1, 1.0), (2, 0.15)]},
    ], gain=0.5)

    # Merge pop: bright rising two-partial "pop" with a touch of sparkle.
    merge = synth([
        {"freq": 480, "glide_to": 760, "dur": 0.14, "decay": 3.2,
         "harmonics": [(1, 1.0), (2, 0.5), (3, 0.18)]},
    ], gain=0.78)

    # Button click: very short tick.
    button = synth([
        {"freq": 340, "dur": 0.035, "decay": 6.0, "harmonics": [(1, 1.0), (2, 0.3)]},
    ], gain=0.55)

    # Level up / unlock: bright ascending major arpeggio C5-E5-G5-C6 with a final octave sparkle.
    up_harm = [(1, 1.0), (2, 0.4), (3, 0.12)]
    level_up = synth([
        {"freq": 523, "dur": 0.10, "decay": 2.0, "harmonics": up_harm},
        {"freq": 659, "dur": 0.10, "decay": 2.0, "harmonics": up_harm},
        {"freq": 784, "dur": 0.10, "decay": 2.0, "harmonics": up_harm},
        {"freq": 1047, "dur": 0.28, "decay": 1.6, "harmonics": up_harm},
    ], gain=0.8)

    # Game over: descending minor arpeggio A4 -> F4 -> C4, slightly gritty.
    go_harm = [(1, 1.0), (2, 0.35), (3, 0.15)]
    game_over = synth([
        {"freq": 440, "dur": 0.16, "decay": 2.5, "harmonics": go_harm},
        {"freq": 349, "dur": 0.16, "decay": 2.5, "harmonics": go_harm},
        {"freq": 262, "dur": 0.30, "decay": 2.2, "harmonics": go_harm},
    ], gain=0.72)

    write_wav(os.path.join(out_dir, "sfx_move.wav"), move)
    write_wav(os.path.join(out_dir, "sfx_merge.wav"), merge)
    write_wav(os.path.join(out_dir, "sfx_button.wav"), button)
    write_wav(os.path.join(out_dir, "sfx_level_up.wav"), level_up)
    write_wav(os.path.join(out_dir, "sfx_game_over.wav"), game_over)


if __name__ == "__main__":
    main()
