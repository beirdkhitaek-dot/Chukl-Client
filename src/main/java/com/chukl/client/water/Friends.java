package com.chukl.client.water;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Minimal stand-in for Water's Friends list (used by combat modules to skip friendly targets).
 * Names are case-insensitive. Anti-triggerbot-on-friends is on by default.
 */
public final class Friends {
    private static final Set<String> FRIENDS = Collections.synchronizedSet(new LinkedHashSet<>());
    private static volatile boolean antiTriggerbot = true;

    private Friends() { }

    public static boolean isFriend(String name) {
        if (name == null) return false;
        return FRIENDS.contains(name.toLowerCase());
    }

    public static void addFriend(String name) {
        if (name != null) FRIENDS.add(name.toLowerCase());
    }

    public static void removeFriend(String name) {
        if (name != null) FRIENDS.remove(name.toLowerCase());
    }

    public static Set<String> list() { return Collections.unmodifiableSet(FRIENDS); }

    public static boolean isAntiTriggerbot() { return antiTriggerbot; }

    public static void setAntiTriggerbot(boolean v) { antiTriggerbot = v; }
}
