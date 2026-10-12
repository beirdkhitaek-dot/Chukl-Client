package com.chukl.client.sixseven;

public class StringSetting extends Setting<String> {
    private final int maxLength;
    private final String placeholder;

    public StringSetting(String name, String description, String defaultValue) {
        this(name, description, defaultValue, 32, "");
    }

    public StringSetting(String name, String description, String defaultValue, int maxLength, String placeholder) {
        super(name, description, defaultValue);
        this.maxLength = maxLength;
        this.placeholder = placeholder == null ? "" : placeholder;
    }

    public int getMaxLength() { return maxLength; }
    public String getPlaceholder() { return placeholder; }

    @Override
    public void set(String v) {
        if (v != null && v.length() > maxLength) v = v.substring(0, maxLength);
        super.set(v);
    }

    @Override protected void pull() { if (backing != null) value = (String) backing.get(); }
    @Override protected void pushToBacking(String v) { if (backing != null) ((com.chukl.client.settings.StringSetting) backing).set(v); }

    @Override
    public com.chukl.client.settings.Setting<?> build() {
        com.chukl.client.settings.StringSetting s =
            new com.chukl.client.settings.StringSetting(getName(), getDescription(), value, maxLength, placeholder);
        s.visibleWhen(this::isVisible);
        backing = s;
        return s;
    }
}
