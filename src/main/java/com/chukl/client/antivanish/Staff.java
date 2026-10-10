package com.chukl.client.antivanish;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** DonutSMP staff names and "is this staff member on my goliath" memory (replaces Anubis's Staff List module). */
public final class Staff {
    public static final List<String> DONUT_STAFF = List.of("FluffyMaster07", "archivePedro", "Munkerlich", "Frenk_Btw", "Napooo_", "auzzitech", "CryptoDaveYT", "W1zox_", "ZEEF69", "showered", "CaptainMoose35", "BobIsFound", "NoahvdAa", "0GSummer", "LzouzMP5", "Pastagamer08", "u_vv", "Owen1212055", "Splaterd", "Fallerfly", "Bloodspulse");
    private static final Set<String> KEYS = new HashSet<>();
    private static final long MARK_MS = 600000L;
    private static final Map<UUID, Long> proven = new ConcurrentHashMap<>();

    static {
        for (String n : DONUT_STAFF) KEYS.add(n.toLowerCase(Locale.ROOT));
    }

    private Staff() { }

    public static boolean isStaffName(String name) { return name != null && KEYS.contains(name.toLowerCase(Locale.ROOT)); }

    /** The server told us about this player, so they are on our goliath. The mark lasts 10 minutes. */
    public static void mark(UUID id) { if (id != null) proven.put(id, System.currentTimeMillis()); }

    public static boolean isInYourRegion(UUID id) {
        Long at = id == null ? null : proven.get(id);
        return at != null && System.currentTimeMillis() - at < MARK_MS;
    }

    public static void clear() { proven.clear(); }

    /** Anubis's own siren; not part of this client. */
    public static boolean alarmedRecently(UUID id) { return false; }

    public static ModuleChat.Body switchedTo(ModuleChat.Body body, String mode) {
        if (mode == null) return body.text("switched game mode");
        body.text("switched to ");
        if ("spectator".equals(mode)) return body.danger(mode);
        if ("creative".equals(mode)) return body.warn(mode);
        return body.name(mode);
    }
}
