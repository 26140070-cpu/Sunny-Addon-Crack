package com.sunny.addon.modules;

import com.sunny.addon.SunnyAddon;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IPlayerMoveC2SPacket;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.*;

public class ProjectileTeleport extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgTargeting = settings.createGroup("Targeting");
    private final SettingGroup sgTP = settings.createGroup("Teleport Options");

    private final Setting<List<Item>> projectileItems = sgGeneral.add(new ItemListSetting.Builder().name("projectile-items").defaultValue(Items.ENDER_PEARL).build());
    private final Setting<Integer> prePackets = sgTP.add(new IntSetting.Builder().name("# spam packets").defaultValue(8).min(0).sliderMax(20).build());
    private final Setting<Double> teleportHeight = sgTP.add(new DoubleSetting.Builder().name("teleport-height").defaultValue(60.0).build());
    private final Setting<Double> maxDistance = sgTP.add(new DoubleSetting.Builder().name("max-distance").defaultValue(120.0).build());
    private final Setting<Set<EntityType<?>>> entities = sgTargeting.add(new EntityTypeListSetting.Builder().name("entities").onlyAttackable().defaultValue(EntityType.PLAYER).build());

    private boolean executingInteract = false;
    private Entity target = null;

    public ProjectileTeleport() {
        super(SunnyAddon.CATEGORY, "ArrowPoP", "Exploit de proyectiles sin usar getPos.");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.world == null || mc.player == null) return;
        this.target = findClosestTarget();
    }

    @EventHandler(priority = 200)
    private void onPacketSend(PacketEvent.Send event) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;

        if (event.packet instanceof PlayerInteractItemC2SPacket packet) {
            ItemStack stack = mc.player.getStackInHand(packet.getHand());
            if (isValidProjectile(stack.getItem())) {
                if (executingInteract) return;
                executingInteract = true;
                event.cancel();
                interact(packet.getHand());
                executingInteract = false;
            }
        }
    }

    private void interact(Hand hand) {
        if (target == null || mc.player == null) return;

        double hX = mc.player.getX();
        double hY = mc.player.getY();
        double hZ = mc.player.getZ();

        double tX = target.getX();
        double tY = target.getY();
        double tZ = target.getZ();

        for (int i = 0; i < prePackets.get(); i++) {
            sendPosPacket(hX, hY, hZ);
        }

        sendPosPacket(hX, hY + teleportHeight.get(), hZ);

        sendPosPacket(tX, tY + teleportHeight.get(), tZ);

        sendPosPacket(tX, tY + 0.1, tZ);
        mc.player.setPosition(tX, tY + 0.1, tZ);

        mc.getNetworkHandler().sendPacket(new PlayerInteractItemC2SPacket(hand, 0, mc.player.getYaw(), 90.0f));

        sendPosPacket(tX, tY + teleportHeight.get(), tZ);

        sendPosPacket(hX, hY + teleportHeight.get(), hZ);

        sendPosPacket(hX, hY, hZ);
        mc.player.setPosition(hX, hY, hZ);
    }

    private void sendPosPacket(double x, double y, double z) {
        if (mc.getNetworkHandler() == null) return;
        PlayerMoveC2SPacket.PositionAndOnGround p = new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, false, false);
        ((IPlayerMoveC2SPacket) p).meteor$setTag(1337);
        mc.getNetworkHandler().sendPacket(p);
    }

    private Entity findClosestTarget() {
        Entity closest = null;
        double distSq = Double.MAX_VALUE;
        for (Entity e : mc.world.getEntities()) {
            if (e == mc.player || !e.isAlive() || !entities.get().contains(e.getType())) continue;
            if (e instanceof PlayerEntity && Friends.get().isFriend((PlayerEntity) e)) continue;

            double dx = mc.player.getX() - e.getX();
            double dy = mc.player.getY() - e.getY();
            double dz = mc.player.getZ() - e.getZ();
            double d = dx * dx + dy * dy + dz * dz;

            if (d < distSq && d <= (maxDistance.get() * maxDistance.get())) {
                distSq = d;
                closest = e;
            }
        }
        return closest;
    }

    private boolean isValidProjectile(Item item) {
        return projectileItems.get().contains(item);
    }
}
