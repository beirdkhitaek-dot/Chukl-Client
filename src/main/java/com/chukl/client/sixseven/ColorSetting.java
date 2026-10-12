package com.chukl.client.sixseven;

public class ColorSetting extends Setting<Integer> {
    public ColorSetting(String name, String description, int defaultArgb) {
        super(name, description, defaultArgb);
    }

    public int red() { return get() >> 16 & 0xFF; }
    public int green() { return get() >> 8 & 0xFF; }
    public int blue() { return get() & 0xFF; }
    public int alpha() { return get() >>> 24 & 0xFF; }

    public void setRed(int r) { set(get() & 0xFF00FFFF | (r & 0xFF) << 16); }
    public void setGreen(int g) { set(get() & 0xFFFF00FF | (g & 0xFF) << 8); }
    public void setBlue(int b) { set(get() & 0xFFFFFF00 | b & 0xFF); }

    public String hex() { return String.format("#%06X", get() & 0xFFFFFF); }

    @Override protected void pull() { if (backing != null) value = (Integer) backing.get(); }
    @Override protected void pushToBacking(Integer v) { if (backing != null) ((com.chukl.client.settings.ColorSetting) backing).set(v); }

    @Override
    public com.chukl.client.settings.Setting<?> build() {
        com.chukl.client.settings.ColorSetting c = new com.chukl.client.settings.ColorSetting(getName(), getDescription(), value);
        c.visibleWhen(this::isVisible);
        backing = c;
        return c;
    }
}
