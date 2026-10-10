package com.chukl.client.settings;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** Free text. Edited by clicking its row in the GUI and typing. */
public class StringSetting extends Setting<String> {
    public final int maxLength;
    public final String placeholder;

    public StringSetting(String name, String description, String def, int maxLength, String placeholder) {
        super(name, description, def);
        this.maxLength = maxLength;
        this.placeholder = placeholder == null ? "" : placeholder;
    }

    public StringSetting(String name, String description, String def) {
        this(name, description, def, 64, "");
    }

    @Override
    protected String sanitize(String v) {
        if (v == null) return def;
        return v.length() > maxLength ? v.substring(0, maxLength) : v;
    }

    @Override public JsonElement toJson() { return new JsonPrimitive(value); }
    @Override public void fromJson(JsonElement e) { set(e.getAsString()); }
}
