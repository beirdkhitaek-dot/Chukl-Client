package com.chukl.client.mixin;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MinecraftClient.class)
public interface MinecraftAccessor {
    @Accessor("itemUseCooldown")
    void chukl$setItemUseCooldown(int v);

    @Invoker("doAttack")
    boolean chukl$doAttack();

    @Invoker("doItemUse")
    void chukl$doItemUse();
}
