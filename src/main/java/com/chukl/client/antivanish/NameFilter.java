package com.chukl.client.antivanish;

/** Hook for hiding real names (kept for parity with Anubis). Identity for now. */
public final class NameFilter {
    private NameFilter() { }
    public static String apply(String name) { return name; }
}
