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
