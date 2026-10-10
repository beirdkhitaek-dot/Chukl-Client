package com.chukl.client.core;

import com.chukl.client.ChuklClient;
import com.chukl.client.gui.Layout;
import com.chukl.client.settings.Setting;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

/** Saves to .minecraft/config/chuklclient.json. Autosaves ~3s after a change and on exit. */
public final class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path path;
    private static boolean dirty;
    private static boolean loading;
    private static int ticks;

    private Config() { }

    public static void init() {
        path = FabricLoader.getInstance().getConfigDir().resolve("chuklclient.json");
    }

    public static void markDirty() {
        if (!loading) dirty = true;
    }

    public static void tick() {
        if (dirty && ++ticks >= 60) save();
    }

    public static void load() {
        if (path == null || !Files.exists(path)) return;
        loading = true;
        try {
            JsonObject root = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            JsonObject mods = root.has("modules") ? root.getAsJsonObject("modules") : new JsonObject();
            for (Module m : ModuleManager.all()) {
                try {
                    if (!mods.has(m.name)) continue;
                    JsonObject o = mods.getAsJsonObject(m.name);
                    if (o.has("bind")) m.setBind(o.get("bind").getAsInt());
                    if (o.has("settings")) {
                        JsonObject so = o.getAsJsonObject("settings");
                        for (Setting<?> s : m.getSettings()) {
                            try {
                                if (so.has(s.name)) s.fromJson(so.get(s.name));
                            } catch (Exception e) {
                                ChuklClient.LOG.warn("Bad setting {}.{}", m.name, s.name);
                            }
                        }
                    }
                    if (m.persistEnabled() && o.has("enabled") && o.get("enabled").getAsBoolean()) m.setEnabled(true);
                } catch (Exception e) {
                    ChuklClient.LOG.warn("Bad config for module {}", m.name, e);
                }
            }
            if (root.has("gui")) Layout.fromJson(root.getAsJsonObject("gui"));
        } catch (Exception e) {
            ChuklClient.LOG.error("Could not read config, using defaults", e);
        } finally {
            loading = false;
        }
    }

    public static void save() {
        dirty = false;
        ticks = 0;
        if (path == null) return;
        try {
            JsonObject root = new JsonObject();
            JsonObject mods = new JsonObject();
            for (Module m : ModuleManager.all()) {
                JsonObject o = new JsonObject();
                if (m.persistEnabled()) o.addProperty("enabled", m.isEnabled());
                o.addProperty("bind", m.getBind());
                JsonObject so = new JsonObject();
                for (Setting<?> s : m.getSettings()) {
                    JsonElement e = s.toJson();
                    if (e != null) so.add(s.name, e);
                }
                o.add("settings", so);
                mods.add(m.name, o);
            }
            root.add("modules", mods);
            root.add("gui", Layout.toJson());
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(root));
        } catch (Exception e) {
            ChuklClient.LOG.error("Could not save config", e);
        }
    }
}
