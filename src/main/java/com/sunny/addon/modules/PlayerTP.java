package com.sunny.addon.modules;

import com.sunny.addon.SunnyAddon;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;

public class PlayerTP extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> disableOnDisconnect = sgGeneral.add(new BoolSetting.Builder()
        .name("disable-on-disconnect")
        .description("Desactiva el módulo al salir del servidor.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> ignoreFriends = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-friends")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> doAttack = sgGeneral.add(new BoolSetting.Builder()
        .name("attack-player")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> hitsPerTick = sgGeneral.add(new IntSetting.Builder()
        .name("hits-per-tick")
        .defaultValue(1)
        .min(1)
        .sliderMax(10)
        .build()
    );

    public PlayerTP() {
        super(SunnyAddon.CATEGORY, "FlyTP", "Teleport to nearest player and attack");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null) return;

        PlayerEntity target = getNearestTarget();
        if (target != null) {
            double tx = target.getX();
            double ty = target.getY();
            double tz = target.getZ();

            if (mc.getNetworkHandler() != null) {
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                    tx, ty, tz, true, mc.player.horizontalCollision
                ));
            }

            mc.player.setPosition(tx, ty, tz);

            if (doAttack.get()) {
                attack(target);
            }
        }
    }

    private PlayerEntity getNearestTarget() {
        PlayerEntity best = null;
        double bestDist = Double.MAX_VALUE;

        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player) continue;
            if (!p.isAlive()) continue;
            if (ignoreFriends.get() && Friends.get().isFriend(p)) continue;

            double dx = mc.player.getX() - p.getX();
            double dy = mc.player.getY() - p.getY();
            double dz = mc.player.getZ() - p.getZ();
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

            if (dist < bestDist) {
                bestDist = dist;
                best = p;
            }
        }

        return best;
    }

    private void attack(PlayerEntity target) {
        mc.player.setYaw((float) getYawTo(target));

        for (int i = 0; i < hitsPerTick.get(); i++) {
            if (mc.interactionManager != null) {
                mc.interactionManager.attackEntity(mc.player, target);
                mc.player.swingHand(Hand.MAIN_HAND);
            }
        }
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        if (disableOnDisconnect.get() && this.isActive()) {
            this.toggle();
        }
    }

    private double getYawTo(PlayerEntity target) {
        double dx = target.getX() - mc.player.getX();
        double dz = target.getZ() - mc.player.getZ();
        return Math.toDegrees(Math.atan2(dz, dx)) - 90.0;
    }
}
