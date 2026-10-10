package com.chukl.client.antivanish;

import java.util.Locale;

public final class AntiVanishHeuristics {
    private AntiVanishHeuristics() {
    }

    public static boolean suspiciousSound(String id) {
        String path = AntiVanishHeuristics.path(id);
        return path.contains("chest.open") || path.contains("chest.close") || path.contains("chest.locked") || path.contains("barrel.open") || path.contains("barrel.close") || path.contains("shulker_box.open") || path.contains("shulker_box.close") || path.contains("door.open") || path.contains("door.close") || path.contains("fence_gate.open") || path.contains("fence_gate.close") || path.contains("button.click") || path.contains("lever.click");
    }

    public static boolean suspiciousParticle(String id) {
        String path = AntiVanishHeuristics.path(id);
        return path.contains("crit") || path.contains("enchanted_hit") || path.contains("damage_indicator") || path.contains("smoke") || path.equals("block");
    }

    public static String path(String id) {
        if (id == null) {
            return "unknown";
        }
        int split = id.indexOf(58);
        return (split >= 0 && split + 1 < id.length() ? id.substring(split + 1) : id).toLowerCase(Locale.ROOT);
    }
}
