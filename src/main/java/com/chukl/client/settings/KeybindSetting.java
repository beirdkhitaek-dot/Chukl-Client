package com.chukl.client.settings;

import com.chukl.client.core.KeyNames;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** A key stored by a module (not the module's own toggle key). -1 means unbound. */
public class KeybindSetting extends Setting<Integer> {
    public KeybindSetting(String name, String description, int def) { super(name, description, def); }

    public boolean isBound() { return value >= 0; }
    public boolean matches(int keyCode) { return value >= 0 && value == keyCode; }
    public String keyName() { return KeyNames.name(value); }

    @Override public JsonElement toJson() { return new JsonPrimitive(value); }
    @Override public void fromJson(JsonElement e) { set(e.getAsInt()); }
}
