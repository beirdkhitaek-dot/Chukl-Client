package com.chukl.client.antivanish;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.World;

/** Notices when you moved to another goliath (or world / server): everything learned so far is then stale. */
public final class ShardTracker {
    private ClientWorld level;
    private ClientPlayerEntity self;
    private String brand;
    private int goliath = Integer.MIN_VALUE;

    public boolean moved(MinecraftClient mc) {
        ClientWorld nowLevel = mc.world;
        ClientPlayerEntity nowSelf = mc.player;
        if (nowLevel == null || nowSelf == null) {
            clear();
            return false;
        }
        ClientPlayNetworkHandler connection = mc.getNetworkHandler();
        String nowBrand = connection == null ? null : connection.getBrand();
        int nowGoliath = nowLevel.getRegistryKey() == World.OVERWORLD
            ? DonutGoliathMapData.goliathAt(nowSelf.getX(), nowSelf.getZ()) : Integer.MIN_VALUE;
        boolean moved = level != null && (nowLevel != level || (nowSelf != self && !self.isDead()) || nowGoliath != goliath
            || (nowBrand != null && brand != null && !nowBrand.equals(brand)));
        level = nowLevel;
        self = nowSelf;
        brand = nowBrand;
        goliath = nowGoliath;
        return moved;
    }

    public void clear() {
        level = null;
        self = null;
        brand = null;
        goliath = Integer.MIN_VALUE;
    }
}
