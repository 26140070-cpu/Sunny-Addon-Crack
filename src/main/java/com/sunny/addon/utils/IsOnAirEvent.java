package com.sunny.addon.utils;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.AbstractBlock.AbstractBlockState;
import meteordevelopment.meteorclient.MeteorClient;

public class IsOnAirEvent {
    private static final BlockPos.Mutable testPos = new BlockPos.Mutable();

    private IsOnAirEvent() {
    }

    public static boolean isOnAir(Entity entity) {
        if (MeteorClient.mc.world == null) return true;
        
        for (var shape : MeteorClient.mc.world.getBlockCollisions(entity, entity.getBoundingBox().contract(0.0625).offset(0.0, -0.55, 0.0))) {
            if (!shape.isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
