package com.chukl.client.settings;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** ARGB color. */
public class ColorSetting extends Setting<Integer> {
    public ColorSetting(String name, String description, int argb) { super(name, description, argb); }

    /** The color as an object with r, g, b, a fields (used by ported modules). */
    public com.chukl.client.compat.SettingColor color() {
        return new com.chukl.client.compat.SettingColor((value >> 16) & 255, (value >> 8) & 255, value & 255, (value >>> 24) & 255);
    }

    /** ch: 0=R 1=G 2=B 3=A */
    public int channel(int ch) { return (value >> (ch == 3 ? 24 : ch == 0 ? 16 : ch == 1 ? 8 : 0)) & 0xFF; }

    public void setChannel(int ch, int v) {
        int sh = ch == 3 ? 24 : ch == 0 ? 16 : ch == 1 ? 8 : 0;
        v = Math.max(0, Math.min(255, v));
        set((value & ~(0xFF << sh)) | (v << sh));
    }

    @Override public JsonElement toJson() { return new JsonPrimitive(value); }
    @Override public void fromJson(JsonElement e) { set(e.getAsInt()); }
}
