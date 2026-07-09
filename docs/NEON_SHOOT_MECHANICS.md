# Neon Shoot — Game Mechanics Specification

A shoot-up number-merge game (X2-Blocks style) for the Neon 2048 app.
No coins, no boosters, no shop, no ads — pure mechanics, physics-feel animation,
fully themed through `GameTheme`.

---

## 1. Board

| Property | Value |
|----------|-------|
| Columns  | 5 |
| Rows     | 8 (capacity per column) |
| Anchor   | Stacks hang from the **top** of the board |
| Gravity  | Toward the **ceiling** (row 0). Removing a tile makes tiles below it slide up |
| Launcher | Bottom of the screen, fires tiles **upward** |

Cell addressing: `(col, row)` with `row 0` at the top. A column is an ordered
list of tiles; index = row. New tiles land at `row = column.size`.

## 2. Tiles

- Values are powers of two: 2, 4, 8, … (no cap; colors cycle above 2048).
- Every tile has a **stable id** — the UI animates position/value changes by id,
  which is what makes movement, collapse and merges look continuous.
- Colors come from `GameTheme.tileColors[value]`, so every app theme skins the
  mode automatically. Values above 2048 reuse the 2048 accent.

## 3. Shooting

1. The launcher holds the **current** tile; a small preview shows the **next** tile.
2. Aim by tapping a column, or dragging horizontally across the board/launcher —
   the hovered column highlights and a **ghost slot** shows the exact landing cell.
3. On release/tap the tile flies up the column (~160 ms, ease-out) and lands with
   a squash-and-settle impact.
4. Input is locked while a resolution (merge cascade) is playing (~short); the
   launcher reloads from the spawn generator when the board settles.

## 4. Merging

When a tile lands (or the board changes), merges resolve as a cascade:

- A tile merges with **every orthogonally adjacent tile of equal value**
  (above, below, left, right — within stacks).
- Merging with `n` neighbors simultaneously: `newValue = value × 2^n`
  - 1 neighbor → ×2 (classic)
  - 2 neighbors → ×4
  - 3 neighbors → ×8
- Neighbor tiles are **consumed** — they fly into the focus tile, which pops to
  the new value with a glow burst.
- After each merge, **gravity** collapses affected columns toward the ceiling,
  then the board is re-scanned; any new adjacency triggers the next chain step.
- The cascade repeats until the board is stable.

### Determinism
The scan order for follow-up merges is fixed (left→right, top→bottom, placed
tile first), so identical inputs always resolve identically — this keeps undo,
tests and animation playback exact.

## 5. Scoring & combos

- Every merge adds `newValue` to the score (2048-style, predictable).
- Each extra cascade step shows **COMBO ×N** floating text; combos are a
  celebration, not a hidden multiplier — score stays honest and readable.
- **Best score** and **best tile** persist via DataStore (`drop_merge` prefs).

## 6. Spawn generator

- Spawn window = **5 consecutive powers of two**, weighted toward the small end
  (weights 6/5/4/2/1), starting at 2…32.
- The window floor rises with your **best tile ever** (see purge below), so
  late-game boards aren't littered with useless 2s.
- The generator never spawns the next-unlock value directly — you must build it.

## 7. Milestones & the purge

- A badge shows the **next unlock** target (first target: 256), greyed until reached.
- Reaching a new max tile fires an **UNLOCKED** banner + glow celebration.
- **Purge:** when the best tile reaches a threshold, the lowest tier detonates
  off the board (with score credit for each removed tile) and leaves the spawn pool:

| Best tile reached | Purged from board | New minimum spawn |
|-------------------|-------------------|-------------------|
| 512               | all 2s            | 4 |
| 1024              | all 4s            | 8 |
| 2048              | all 8s            | 16 |
| 4096              | all 16s           | 32 |
| …                 | next tier         | doubles |

After a purge, gravity collapses and the merge cascade re-runs (purges can chain!).

## 8. Danger & game over

- A column with **≤ 2 free cells** pulses its track border in the theme's danger
  accent; a full column pulses harder.
- **Game over** when:
  1. You fire into a **full column** whose end (bottom-most) tile ≠ current tile, or
  2. The **board locks**: all 5 columns are full and the current tile matches
     none of the column-end tiles (every possible shot would be fatal).
- Firing into a full column whose end tile **matches** is legal — it merges in place.

## 9. Undo & Skip

- **Undo:** one free undo per shot (single-step, like Time Attack): restores
  columns, score, current + next tile from before the last shot.
- **Skip:** once per turn the current tile can be skipped — it is replaced by
  the next tile and a fresh next is generated. Resets after every shot, so it
  helps planning without enabling endless rerolls. No currency attached to
  either.

## 10. Flow & screens

- **Pause** — reuses `GamePauseDialog` (resume / restart / home), back button pauses.
- **Game over** — themed dialog: final score, best score, best tile, play again / home.
- Entry from the Main menu as a third `GameModeCard` ("NEON DROP"), portrait and
  landscape variants, navigation route `dropMerge`.

## 11. Animation inventory

| Event | Animation |
|-------|-----------|
| Shot | Tile travels launcher → slot, ease-out, squash on impact |
| Merge | Consumed tiles fly/fade into target; target pops 1→1.25→1 with radial glow |
| Collapse | Tiles slide to new rows (spring, slight overshoot) |
| Combo | Floating "COMBO ×N" text rising over the board with scale-in |
| Purge | Purged tiles flash then shrink-fade with glow |
| Unlock | Banner sweep + tile glow celebration |
| Danger | Infinite pulse on near-full column borders |
| Launcher | ONE slidable block that springs between columns, follows drags, idle-bobs, glows, and pops on reload |
| Aim | Aimed column glows with a vertical gradient beam in the incoming tile's color; ghost landing slot pulses in the same color |
| Next swap | Next-tile preview scales in when it changes (shot or skip) |
| Game over | Board shake before the dialog appears |

The screen is fully fraction-sized from its constraints and has dedicated
portrait and landscape layouts (side control panels in landscape).

All timings live in one place (`DropAnim`) so feel can be tuned centrally.

## 12. Settings integration

- Sound uses the existing `SoundManager` (merge / move / game-over cues) gated by
  `soundEnabledFlow`; haptics gated by `vibrationEnabledFlow` — same behavior as
  the other modes.

## 13. Architecture

```
game/DropMergeEngine.kt        pure Kotlin rules — resolve(shot) → list of board
                               snapshots (steps) with merge metadata; unit-tested
model/DropMergeModels.kt       DropTile, DropStep, DropMergeState
data/repository/DropMergeRepository.kt   DataStore: best score / best tile / games
viewmodel/DropMergeViewModel.kt          plays steps with delays, exposes StateFlow,
                                         events for sound/haptics, undo, pause
ui/screens/DropMergeScreen.kt            themed board + launcher + dialogs
```

The engine emits **snapshot steps**; the ViewModel plays them on a coroutine with
tuned delays; the UI animates tile ids between snapshots. Logic stays 100%
testable and animation stays 100% declarative.
