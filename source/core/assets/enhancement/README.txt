OLYMPUS RUN — ART & AMBIENCE PACK (libGDX)
============================================

Drop the contents into core/assets (or android/assets). Everything is PNG — no GIFs.
libGDX does not play GIFs; animations ship as sprite-sheet strips you slice with
TextureRegion.split (see code/OlympusGfx.java). Load every texture with Nearest filtering.

Viewport is 1280x720. Backgrounds are already scaled to that (pixel art at 4x).
Tiles and cloud-platform pieces stay at native 16px — scale your tile layer the same way you do today.

FOLDERS
  backgrounds/lv1-dungeon      4 layers, seamless horizontally (fog loops both ways)
  backgrounds/lv1-underworld   5 layers: void, far arches, columns, embers loop, foreground
  backgrounds/lv2-olympus      sky, sea, ranges, cloud banks (vertical parallax, pre-sized)
                               + wisps loop and storm-rain loop
  animation/lv1                torch flame (8f), sconce, lava flow (4f, tiles horizontally)
  animation/lv2                lightning bolt (6f), bird flap (4f), 9 drifting cloud sprites
  tiles/cloud-platforms        left / mid / right pieces for 5 cloud types; fade, storm and
                               thermal are frame sheets. Art is 24px tall: collision top at y=6.
  fx                           additive light sprites, sun rays, ember particle
  ui/titles                    title cards (kicker + title + ornament + subtitle), each part
                               also exported alone; zone banners; checkpoint / complete / died / paused
  ui/hud                       nine-patches (.png + .9.png for TexturePacker), portrait frame,
                               hearts 32/48, hotbar slots, pips, coin, rails + markers, vignettes
  fonts                        Olympus Pixel regular & bold, 14px and 28px, BMFont .fnt (white — tint in code)
  manifest.json                every layer's scroll factor, size and every animation's frame data
  code/OlympusGfx.java         VLayer, LoopLayer, strip(), nine(), drawLight(), TitleCard

DRAW ORDER (Level 2)
  sky → far sea → ranges → sun rays (additive, pulse) → cloud banks → drifting cloud sprites → birds
  → tile map & actors → wisps → rain (storm belt) → lightning + flash → vignette → HUD → title card

DRAW ORDER (Level 1)
  void → far arches/arcade → columns/pillars → lights (additive) → tile map & actors → lava flow
  → embers / fog → foreground → vignette → HUD → title card

PARALLAX MATH (vertical climb)
  layerHeight = 720 + (worldHeight - 720) * factor     (shipped at these sizes)
  draw at      y = camBottom * (1 - factor)

TITLE TIMING
  Level card on level start: fade in 0.6s, hold 2.2s, fade out 0.8s.
  Zone banner on entering a zone: same curve at half the duration, drawn at 30% screen height.

To change title wording, re-render with the font in fonts/ or ask for a new export.
