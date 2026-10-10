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
Not built yet: packet and 3D world-render hooks. They get added with the first module that needs them.

Categories: COMBAT, MISC, DONUT, RENDER, CLIENT.

## Chat commands (for touch)
`.gui`, `.toggle <module>`, `.bind <module> <key|none>`, `.modules`
