# Writing a Chukl Client addon

Chukl is open source (MIT) and addonable the same way Meteor Client is: an addon is its **own Fabric mod**
that depends on Chukl Client and declares an entrypoint. It is not a jar dropped into some special folder —
it goes through Fabric Loader's normal mod loading, so it gets mixins, resources, its own fabric.mod.json,
everything a normal mod gets.

## 1. Depend on Chukl Client

In your addon's `build.gradle.kts`, add Chukl as a mod dependency. The easiest way (same approach Meteor's
own addon template uses) is [Jitpack](https://jitpack.io), which builds any public GitHub repo on request:

```kotlin
repositories {
    maven("https://jitpack.io")
}

dependencies {
    modImplementation("com.github.beirdkhitaek-dot:chukl-client:<version-or-commit>")
}
```

Use a tagged release (e.g. a Git tag like `v1.13.0`) or a commit hash for `<version-or-commit>`. Jitpack
builds it the first time it's requested, which can take a minute or two.

If you're developing Chukl and an addon side by side, `includeBuild("../chukl-client")` (a Gradle composite
build) or publishing Chukl to `mavenLocal()` both work too.

## 2. Implement `ChuklAddon`

```java
package com.example.myaddon;

import com.chukl.client.api.ChuklAddon;
import com.chukl.client.core.Category;
import com.chukl.client.core.ModuleManager;

public class MyAddon implements ChuklAddon {
    // Optional: give your modules their own Click GUI panel instead of filing them under an
    // existing category (Combat, Misc, Donut, Render, Client). Skip this to use an existing one.
    public static final Category CATEGORY = Category.register("MYADDON", "My Addon");

    @Override
    public void onInitialize() {
        ModuleManager.register(new MyModule());
    }

    @Override
    public String name() {
        return "My Addon";
    }
}
```

`onInitialize()` is called once, after Chukl's own built-in modules are registered. If it throws, the
addon is logged and skipped — it won't take down the base client or any other addon.

## 3. Write a module

Modules work exactly like Chukl's own — extend `com.chukl.client.core.Module`, add settings in the
constructor, override the hooks you need:

```java
package com.example.myaddon;

import com.chukl.client.core.Module;
import com.chukl.client.settings.BoolSetting;

public class MyModule extends Module {
    private final BoolSetting loud = add(new BoolSetting("Loud", "Shout about it.", false));

    public MyModule() {
        super(MyAddon.CATEGORY, "MyModule", "Does a thing.");
    }

    @Override
    protected void onTick() {
        // ...
    }
}
```

See `src/main/java/com/chukl/client/modules/` in this repo for real examples of every hook (`onTick`,
`onRender2D`, `onRender3D`, `onPacket`, key binds) and every setting type (`BoolSetting`, `NumberSetting`,
`ColorSetting`, `ModeSetting`, `StringSetting`, `KeybindSetting`, `ButtonSetting`).

## 4. Declare the entrypoint

In your addon's own `fabric.mod.json`:

```json
{
  "schemaVersion": 1,
  "id": "myaddon",
  "version": "1.0.0",
  "name": "My Addon",
  "environment": "client",
  "entrypoints": {
    "client": [ "com.example.myaddon.MyAddonModInit" ],
    "chukl.addon": [ "com.example.myaddon.MyAddon" ]
  },
  "depends": {
    "chuklclient": "*",
    "fabricloader": ">=0.18.0",
    "minecraft": "~1.21.11",
    "fabric-api": "*"
  }
}
```

The `"chukl.addon"` entrypoint is what Chukl looks for (`com.chukl.client.core.AddonManager`, called from
`ChuklClient.onInitializeClient()`, right after Chukl's own modules are registered). A regular `"client"`
entrypoint is optional — most addons don't need one, since `onInitialize()` already runs at the right time.

## 5. Build and test

`gradle build` produces your addon's jar in `build/libs/`. Drop it in `mods/` alongside `chukl-client-*.jar`
and Fabric API. Your modules show up in the Click GUI, in their own panel if you registered a `Category`,
or under whichever existing one you passed in.

## Notes

- Chukl is published with sources (`withSourcesJar()`), so your IDE can jump straight into Chukl's own code
  for reference.
- `ModuleManager.register(Module)` and `Category.register(id, displayName)` are both safe to call from
  anywhere during `onInitialize()` — order between multiple addons doesn't matter, and registering the same
  category id twice just returns the existing one instead of creating a duplicate panel.
- Settings, config persistence, and the Click GUI all treat addon modules and categories exactly like
  Chukl's own — there's no second-class citizenship here.
