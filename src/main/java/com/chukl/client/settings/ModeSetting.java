package com.chukl.client.settings;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class ModeSetting extends Setting<String> {
    public final String[] modes;

    public ModeSetting(String name, String description, String def, String... modes) {
        super(name, description, def);
        this.modes = modes;
    }

    @Override
    protected String sanitize(String v) {
        for (String m : modes) if (m.equalsIgnoreCase(v)) return m;
        return def;
    }

    public boolean is(String m) { return value.equalsIgnoreCase(m); }

    public void cycle(int dir) {
        int i = 0;
        for (int k = 0; k < modes.length; k++) if (modes[k].equals(value)) i = k;
        set(modes[(i + dir + modes.length) % modes.length]);
    }

    @Override public JsonElement toJson() { return new JsonPrimitive(value); }
    @Override public void fromJson(JsonElement e) { set(e.getAsString()); }
}
