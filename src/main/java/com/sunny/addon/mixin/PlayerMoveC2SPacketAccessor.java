package com.sunny.addon.mixin;

import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerMoveC2SPacket.class)
public interface PlayerMoveC2SPacketAccessor {


    @Mutable
    @Accessor("y")
    public void setY(double y);

    @Accessor("y")
    public double getY();


    @Mutable
    @Accessor("onGround")
    public void setOnGround(boolean onGround);

    @Accessor("onGround")
    public boolean getOnGround();
}
