package com.sunny.addon.mixin;

import meteordevelopment.meteorclient.mixininterface.IPlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(PlayerMoveC2SPacket.class)
public class PlayerMoveC2SPacketMixin implements IPlayerMoveC2SPacket {

    @Unique
    private int meteor$tag;

    @Override
    public int meteor$getTag() {
        return this.meteor$tag;
    }

    @Override
    public void meteor$setTag(int tag) {
        this.meteor$tag = tag;
    }
}
