# Level 3 — Zeus's Palace: boss arena

One throne room, 52 x 26 tiles, fully enclosed. The camera (38 x 21) sees about three quarters of it at once, so the fight never loses the player and Zeus is rarely far off screen.
Grid: `maps/level3-arena.txt` · markers: `maps/level3-markers.txt` · legend: `maps/level3-legend.json`.
Reachability check (4-tile jump, 4-tile reach, ladders, drop-through): all 118 standing spots are reachable from P and every one leads back — no traps.

## Layout (x = column, y = row from top)

- **Floor (y 23)** — red porphyry wings (x 2–11, 40–49), checker marble (x 12–21, 30–39), purple dais with gold medallion under the throne (x 22–29). Charged strips at x 10–11 and 40–41.
- **Cover** — two stone blocks, 2x2, at x 16–17 and 34–35. The two balcony columns at x 23 and 28 are decoration (no collision).
- **Platforms** — symmetric one-way slabs in four tiers: y 19 (x 7–12, 39–44), y 15 (x 3–7, 16–21, 30–35, 44–48), y 11 (x 11–16, 35–40), and the throne balcony y 7 (x 19–32).
- **Ladders** — at both walls, x 3 and x 48, floor to the y 15 ledges.
- **Landmarks** — the throne on the balcony, statues at both ends of the floor, six braziers, four banners.
- **Backdrop** — great-hall arches open onto the storm, with a colossal seated Zeus on a far peak, the cloud sea, a distant colonnade and drifting mist. Distant lightning flickers behind the arches (small sprite, never a screen flash).

## Fight flow

**Phase 1 — the floor (100–70%).** Zeus starts at Z (x 26) and floats along the floor, using melee and thrown bolts. Hide behind the stone blocks, jump the bolts, punish the recovery after each cast.

**Phase 2 — the throne (70–35%).** Zeus rises to the balcony and calls ground strikes under the player. Climb the tiers (or the wall ladders) to reach him; the balcony is the only place to hit him. Strikes force constant movement between tiers. Loot at the balcony ends (x 20, 31) and the y 15 wall ledges.

**Phase 3 — enraged (35–0%).** Zeus slams back to the floor, adds the shockwave and casts faster. Stay on the y 19 slabs or the cover blocks to clear shockwaves, drop down to strike, climb back up.

## Attacks and warnings

| Attack | Phases | Warning the player sees | Where | Time |
|---|---|---|---|---|
| Melee swing | 1, 3 | Bolt pulled back over the shoulder | on Zeus | 0.25 s |
| Thrown bolt | 1, 2, 3 | Arm overhead, sparks at the bolt tip; edge arrow if Zeus is off screen | on Zeus / screen edge | 0.6 s (0.4 s enraged) |
| Ground strike | 2, 3 | Gold marker + electric glow on the tile under the player, brightens for the last 0.3 s | under the player | 0.9 s (0.7 s enraged) |
| Shockwave | 3 | Floor tiles light up in sequence ahead of the wave; edge arrow if off screen | along the floor | 0.5 s |
| Charged floor | all | Animated hazard stripe and arcs, constant glow | on the tiles | always |

No full-screen flashes anywhere.
