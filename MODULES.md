# Adding modules

1. Create a class in `src/main/java/com/chukl/client/modules/<category>/` that extends `Module`.
2. Register it in `ModuleManager.init()`: `register(new MyModule());`

```java
public class Example extends Module {
    private final BoolSetting loud = add(new BoolSetting("Loud", "Say hi", true));
    private final NumberSetting delay = add(new NumberSetting("Delay", "Ticks", 20, 1, 100, 1));

    public Example() { super(Category.MISC, "Example", "Does a thing"); }

    @Override protected void onEnable() { }
    @Override protected void onDisable() { }
    @Override protected void onTick() { }                              // every client tick while on
    @Override protected void onRender2D(DrawContext ctx, float d) { }  // HUD drawing
}
```

Settings: `BoolSetting`, `NumberSetting`, `ModeSetting`, `ColorSetting`, `ButtonSetting`.
They show up in the GUI and save to `config/chuklclient.json` automatically.
A module that throws is disabled and logged instead of crashing the game.
3D and packet hooks (added in 1.6.0):
```java
@Override protected void onRender3D(Render3D r) { r.box(pos, fillArgb, lineArgb, Render3D.Mode.BOTH); r.line(x1,y1,z1, x2,y2,z2, argb, width); }
@Override protected void onPacket(Packet<?> p) { }   // after the game handled chunk, light, block, join and respawn packets
```
Water Client modules can be dropped in as they are: extend `com.chukl.client.water.WModule`, use `water.Setting`, `water.RenderUtils`.
Meteor-style addon modules go through `compat/` (see the donut modules for examples).

Categories: COMBAT, MISC, DONUT, RENDER, CLIENT.

## Chat commands (for touch)
`.gui`, `.toggle <module>`, `.bind <module> <key|none>`, `.modules`
