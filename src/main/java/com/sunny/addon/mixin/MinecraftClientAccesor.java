package com.sunny.addon.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MinecraftClient.class)
public interface MinecraftClientAccesor {

    @Accessor("currentFps")
    static int meteor$getFps() { return 0; }

    @Mutable @Accessor("session")
    void meteor$setSession(Session session);

    @Accessor("attackCooldown")
    int meteor$getAttackCooldown();

    @Accessor("attackCooldown")
    void meteor$setAttackCooldown(int value);


    @Invoker("doAttack")
    boolean suleftClick();

    @Invoker("handleInputEvents")
    void suhandleInputEvents();

}
