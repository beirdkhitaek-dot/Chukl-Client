package com.chukl.client.api;

/**
 * Entry point for a Chukl Client addon. An addon is its own Fabric mod that depends on Chukl Client (as a
 * compile-time + runtime dependency, the same way a Meteor Client addon depends on meteor-client) and
 * declares, in its own fabric.mod.json:
 *
 * <pre>{@code
 * "entrypoints": {
 *   "chukl.addon": [ "com.example.myaddon.MyAddon" ]
 * }
 * }</pre>
 *
 * where {@code com.example.myaddon.MyAddon} implements this interface. See ADDONS.md at the repo root for
 * a full walkthrough (build.gradle setup, an example module, packaging).
 */
public interface ChuklAddon {
    /**
     * Called once at client init, after Chukl's own built-in modules are already registered. Register
     * your modules here, e.g. {@code com.chukl.client.core.ModuleManager.register(new MyModule())}. If you
     * want your modules in their own Click GUI panel instead of filing them under an existing category,
     * register one first with {@code com.chukl.client.core.Category.register("MYADDON", "My Addon")} and
     * pass the result as each module's category.
     * <p>
     * Thrown exceptions are caught and logged per-addon -- a broken addon is skipped, it won't take down
     * the base client or other addons -- so there's no need to guard this yourself, though it's still your
     * responsibility to leave things in a sane state if you do throw partway through.
     */
    void onInitialize();

    /** Shown in logs (addon loaded / addon crashed) so a problem can be traced to the right addon. Defaults
     *  to the implementing class's simple name; override for something more readable. */
    default String name() { return getClass().getSimpleName(); }
}
