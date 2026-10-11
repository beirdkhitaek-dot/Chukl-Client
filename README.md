# Chukl Client

Fabric client mod for Minecraft 1.21.11. Needs only Fabric Loader and Fabric API.

- **Right Shift** opens the Click GUI (rebind it in the Click GUI module).
- Categories: Combat, Misc, Donut, Render, Client.
- Drag a panel header to move it. Drag the bottom-right corner to resize it. Tap the header (or +/-) to collapse.
- Left-click a module to toggle. Right-click, or tap the +/- at its right edge, to open its settings.
- Click GUI settings control size (Scale, Row Height, Header Height), every color (accent, panels, text, hover, sliders, buttons), per-category header colors, borders, text shadow and screen dim.
- Design it visually in Chukl GUI Studio and paste the exported JSON into `config/chuklclient.json`.

## MojoLauncher setup
1. Create a 1.21.11 instance with Fabric Loader.
2. Put Fabric API and `chukl-client-<version>.jar` in the instance's `mods` folder.
3. Right Shift: add a control button mapped to Right Shift in MojoLauncher's control editor.
   Without a keyboard you can type `.gui` in chat, or use the X button to close.

## Build
`gradle build` (Gradle 9.2, Java 21). The jar lands in `build/libs/`. See MODULES.md for adding modules.

## Custom font
Client > Fonts module > Font: Default, Unifont, Lemon Milk, Arial Bold, Xuong (bundled), or Custom. Custom uses `chuklclient:custom`, supplied by a resource pack. See `chukl-font-pack-template.zip`.

## HUD
Watermark, Coordinates, Module List and Clock are separate modules in the Client panel. Each has anchor, offsets, scale, color mode (static, accent, rainbow), background, padding, border width and color. Place them by dragging in Chukl GUI Studio.

## Donut modules (1.6.0)
Six base / chunk finders, all in the Donut panel. They draw through walls.
- **Water Sus Chunk**: Water Client's amethyst + underground chest finder.
- **Sus Chunk Finder**: your addon's tuned version.
- **Krispy Chunk Finder**: server-light and dig based finder.
- **Neon Chunk Finder**: Water's cached spawner, chest and dungeon finder with tracers.
- **OG Chunk Finder**: Water's seagrass, deepslate, repeater and polar-bear detector with chunk markers.
- **67 Sus Chunk Finder**: 67Client's scored finder with zones and a centroid marker.
- **Anti Vanish** (1.6.1, from Anubis): watches the DonutSMP staff list for vanished staff using tab, spectator switches, a /msg probe, invisible bodies, sounds and particles, plus the goliath map. A red HUD box and the Elder Guardian sound say "WATCHING YOU".
- **Logo** (1.6.2): Client panel > Logo. Pick the Ch logo or the Troll logo, size, opacity, corner, and whether it shows on the HUD and in the Click GUI corner. The Ch logo is also the mod icon.

## Combat modules (1.7.0)
Fourteen Water Client combat modules, ported as-is (extend `water.WModule`, so they have a secondary **Activation Key** bind alongside their normal toggle bind — set both in their settings), plus two aura modules in the Combat panel.
- **Anchor Macro**, **Single Anchor**, **Double Anchor**: respawn-anchor combos (place, charge, explode).
- **Auto Crystal**, **Auto Crystal AC**: end-crystal PvP automation, break/place delay, auto-obsidian, anti-cheat-aware variant.
- **Auto Hit Crystal**: crystal-hit timing helper.
- **Auto Double Hand**, **Spear Swap**, **Shield Breaker**: offhand/mainhand swap and shield-disable helpers.
- **Auto Totem**, **Auto Inv Totem**, **Hover Totem**: keeps a totem in the offhand (inventory swap or hotbar variants).
- **Hitbox**, **Triggerbot**: hitbox/attack helpers, friend-aware.
- **Hit Aura**: target-and-rotate aura — picks the closest valid entity in range, turns the player's view toward it and attacks on a configurable attacks-per-second timer. Rebuilt on Chukl's own module system rather than Water's separate rotation framework; see the class javadoc.
- **Legit Aura**: aim-assisted aura that never touches player rotation — it only attacks a target already inside a configurable aim-FOV cone around the real crosshair, for legit-looking play.
