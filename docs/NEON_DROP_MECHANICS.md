# Neon Drop — Game Mechanics Specification

The top-drop falling-merge game (Drop-The-Number style). The current tile
hovers above the board; pick any column and it **free-falls** onto the stack
below with real gravity motion — accelerate, impact squash, cascade.

No coins, no boosters, no shop, no ads — pure mechanics + animation, fully
skinned by `GameTheme`.

---

## 1. Relationship to Neon Shoot

Neon Drop is the exact **mirror** of Neon Shoot and runs on the same rules
engine (`DropMergeEngine`):

| | Neon Shoot | Neon Drop |
|---|---|---|
| Stacks | hang from the **ceiling** | build up from the **floor** |
| Tile enters | fired **up** from below | **falls** from above |
| Gravity | toward the ceiling | toward the floor (real gravity) |
| Motion feel | springy shot | accelerating free-fall + impact squash |
| Fail state | column overflows downward | column reaches the **top** |

Merges, cascades, scoring, spawn windows, milestones and purge are identical
and share one implementation (25 unit tests).

## 2. Board

- 5 columns × 8 rows; row 0 is the **floor**, stacks grow upward.
- The **dropper** strip sits above the board holding the current tile.
- Danger: a column with ≤ 2 free cells pulses; a full column pulses harder.

## 3. Dropping

1. Aim by tapping a column, or dragging across the dropper/board — the
   slidable tile springs along the strip and a **ghost slot** marks exactly
   where it will come to rest; the aimed column glows with a gradient beam
   in the tile's color (strongest at the top, where it enters).
2. Release/tap → the tile **free-falls**: accelerating easing (not a spring),
   then an impact **squash-and-recover** when it lands on the stack.
3. Cascade collapses also fall with gravity — when tiles below vanish, the
   ones above drop down with the same accelerating motion.

## 4. Merging & cascades (shared engine)

- A landed tile merges with **all equal orthogonal neighbors**: ×2 / ×4 / ×8.
- After each merge, stacks collapse toward the floor, the board re-scans
  deterministically, and chains cascade until stable — **COMBO ×N** flair.
- Score adds each merge's result value.

## 5. Spawning, milestones, purge

- Weighted 5-value spawn window (2…32 at start), floor rises with the best
  tile ever; **next-unlock badge** and banner (256 → 512 → …).
- **Purge:** reaching 512 detonates all 2s (score credited), 1024 the 4s,
  and so on — same table as Neon Shoot.

## 6. Game over

- Dropping into a **full column** whose top tile doesn't match → overflow →
  game over (board shake, delayed dialog).
- Board lock: all columns full and the current tile matches no column top.

## 7. QoL

- **Undo** — one free single-step undo per drop.
- **Skip** — once per turn, swap the current tile for the next (resets each
  drop, restored by undo).
- Pause via `GamePauseDialog`, back button pauses, best score/tile persisted
  (`fall_merge` DataStore), sound/haptics via existing settings.

## 8. Layout & animation

- Fraction-sized **portrait + landscape** layouts (side panels in landscape),
  mirroring Neon Shoot's structure with the launcher flipped to the top.
- Animation inventory: free-fall entry + impact squash, gravity collapse,
  merge pop + glow, consume fly-ins, purge shrink, combo scale-in rise,
  next-preview swap pop, dropper bob + reload pop, danger pulse, game-over
  shake with delayed dialog.

## 9. Architecture

```
game/DropMergeEngine.kt          shared rules (bottom-anchor is just a render flip)
viewmodel/FallMergeViewModel.kt  step playback with fall timings, skip, undo
data/repository/FallMergeRepository.kt   fall_merge DataStore
ui/screens/FallMergeScreen.kt    mirrored board, top dropper, gravity tiles
ui/components/MergeTileViews.kt  shared tiles — BoardTileView(gravity = true)
```
