# Neon Rise — Game Mechanics Specification

A rack-shooter merge game (third game in the app). A rack of five numbered
blocks sits on the floor — one per lane. Fire a block straight up its lane;
it sticks to the stack hanging from the ceiling and merges on match. Stacks
creep down toward the danger line; keep them trimmed or die.

Same rules of engagement as Neon Drop: **no coins, no boosters, no shop, no
ads** — pure mechanics + animation, fully skinned by `GameTheme`.

---

## 1. Why it's different from Neon Drop

| | Neon Drop | Neon Rise |
|---|---|---|
| Ammo | ONE tile, you aim it anywhere | FIVE tiles racked, each locked to its lane |
| Skill | choosing the column | choosing the **firing order** + managing refills |
| Pressure | your own mistakes fill columns | the ceiling **descends on a cadence** |
| Fail state | overflow / board lock | any stack tip crossing the **danger line** |

Same board model (top-anchored columns, gravity toward the ceiling), so the
merge engine is shared.

## 2. Board

- **5 lanes × 12 rows** of visual space; stacks hang from the ceiling (row 0).
- **Danger line** drawn at `DANGER_ROW = 9`: a dashed, theme-accent line.
  After a shot fully settles, if any stack's length exceeds `DANGER_ROW`,
  the game is over. Stacks within 2 rows of the line pulse it.
- **Floor rack**: 5 slots aligned to the lanes, each holding one shootable tile.

## 3. The rack (core skill layer)

- Each slot holds a tile bound to its lane — tap the tile (or swipe up on
  the lane) to fire it **straight up that lane only**.
- After firing, the slot is empty for ~400 ms, then **refills** with a new
  spawned tile (pop-in animation). You may fire other slots while one refills.
- Only one shot resolves at a time (input locked during cascade playback,
  same as Neon Drop).
- **Re-rack (skip analog):** once per pressure cycle, a long-press on a rack
  tile rerolls it. Free, capped, resets when a pressure row drops.

## 4. Shooting & sticking

1. The tile launches up its lane with a **360° tumble** (the signature
   rotating-block flight) and an ease-in burst.
2. It lands at the tip of that lane's stack (or at the ceiling if empty):
   - **Match** → merge ×2 at the tip, then the full cascade runs.
   - **No match** → it sticks, extending the stack downward by one row
     (impact squash + stack shudder).

## 5. Merging & cascades (shared engine)

Identical to Neon Drop's resolution — same code path:

- A merge absorbs **all equal orthogonal neighbors**: ×2 / ×4 / ×8 for
  1 / 2 / 3 simultaneous neighbors.
- Gravity collapses stacks toward the ceiling; the board re-scans
  deterministically; chains cascade until stable, raising **COMBO ×N**.
- Score: every merge adds its result value. Best score + best tile persist
  (`neon_rise` DataStore).

## 6. Pressure — the descent

- Every `PRESSURE_EVERY = 8` shots, a **new row of random tiles spawns at
  the ceiling**, pushing every stack down one row (slam animation + micro
  screen shake). A shot counter ring around the rack shows how close the
  next drop is.
- Pressure rows spawn values from the low half of the current spawn window
  (they're meant to be mergeable fodder, not walls).
- The cadence tightens with progress: `8 → 7 → 6` shots as the best tile
  milestone rises (floor of 5). This is the difficulty curve — no separate
  difficulty menu needed at v1.

## 7. Spawning & milestones

- Rack refills draw from the same **weighted 5-value window** as Neon Drop
  (2…32 at start, floor rises with best tile ever; weights 6/5/4/2/1).
- **Unlock badge** shows the next milestone (256 → 512 → …) with the same
  banner celebration.
- **No purge** in this game — the pressure rows are the churn mechanic;
  purging too would double-relieve the board. (Spawn floor still rises, so
  old small tiles remain merge targets for new fodder — intentional.)

## 8. Game over & grace

- After a settle, any stack longer than `DANGER_ROW` → **game over**
  (board shake, delayed dialog — same feel as Neon Drop).
- Exception: if the overlong stack was created by a **pressure row** (not a
  player shot), the player gets **one grace shot** — the line flashes red
  and the next shot must bring that stack back above the line, or it's over.
  This keeps deaths feeling earned, not random.

## 9. QoL (parity with Neon Drop)

- **Undo** — one free single-step undo per shot (restores board + rack +
  pressure counter).
- **Re-rack** — see §3.
- Pause via `GamePauseDialog`, back button pauses, themed game-over dialog,
  sound/haptics via existing settings flows.

## 10. Screens & layout (fraction-sized, portrait + landscape)

- **Portrait:** top bar (back / score / unlock badge) · board with danger
  line · pressure ring indicator · floor rack.
- **Landscape:** score + badge panel left, board + rack centered, controls
  (pause / undo / pressure ring) right — mirrors Neon Drop's landscape.
- Entry from the main menu as a **third GameHubCard**: "NEON RISE —
  ⤴ Rack Shooter — PLAY", accent = `theme.secondaryColor`.

## 11. Animation inventory

| Event | Animation |
|-------|-----------|
| Launch | Tile tumbles 360° up the lane, ease-in, motion-blur-ish stretch |
| Stick | Impact squash + one-row stack shudder |
| Merge / cascade | Shared: consume fly-ins, pop + glow, COMBO ×N |
| Pressure row | Row slams in from above, whole board dips 4 px, micro shake |
| Pressure ring | Circular progress around the rack fills per shot, flashes on drop |
| Rack refill | Slot pops in from scale 0.5 with a soft glow |
| Re-rack | Tile flips (scaleX -1→1) to its new value |
| Danger | Dashed line pulses when any stack is ≤2 rows away; solid red flash during grace |
| Game over | Board shake, dialog delayed ~600 ms |

## 12. Architecture (max reuse)

```
game/DropMergeEngine.kt        REUSED as-is for merge cascades (same board model)
game/RiseEngine.kt             NEW thin layer: shot placement via resolveShot,
                               pressureRow() insertion, danger/grace rules,
                               rack + reroll logic — pure Kotlin, unit-tested
model/RiseModels.kt            RiseState, RackSlot, RiseConfig (rows, danger row,
                               pressure cadence), undo snapshot
data/repository/RiseRepository.kt   DataStore: best score / best tile / games
viewmodel/RiseViewModel.kt     step playback + pressure scheduling + events
ui/screens/RiseScreen.kt       portrait + landscape; reuses the shared tile
                               composables extracted from DropMergeScreen
ui/components/MergeTileViews.kt     NEW shared home for BoardTileView,
                               ConsumedTileView, TileNumber, ComboText,
                               dropTileColor (extracted, used by both games)
```

Engine emits the same snapshot-step lists; the ViewModel plays them with
delays; the UI animates stable tile ids — the proven Neon Drop pipeline.

## 13. Tuning constants (single source)

`RiseConfig`: `LANES=5`, `ROWS=12`, `DANGER_ROW=9`, `PRESSURE_EVERY=8`
(min 5), `REFILL_DELAY=400ms`, `RERACKS_PER_CYCLE=1`, spawn window shared
with `DropMergeConfig`.

## 14. Test plan (pure engine)

- Shot sticks on non-match, extends stack
- Shot merges on match, cascades chain (reuses DropMergeEngine — already covered)
- Pressure row shifts every stack down one and preserves tile ids
- Cadence tightens at milestones (8→7→6, floor 5)
- Danger: settle beyond DANGER_ROW → over; pressure-caused → grace flag set
- Grace consumed correctly: rescue shot clears it, failed shot ends game
- Re-rack: once per cycle, resets on pressure drop
- Undo restores board + rack + counters exactly
