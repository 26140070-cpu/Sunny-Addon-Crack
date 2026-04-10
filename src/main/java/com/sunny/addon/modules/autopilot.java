package com.sunny.addon.modules;

import com.sunny.addon.SunnyAddon;
import java.lang.reflect.Method;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

public class autopilot extends Module {
    private double targetX;
    private double targetZ;
    private boolean starting = false;
    private boolean flying = false;
    private boolean flyingUp = true;
    private boolean elytraRemoved = false;
    private int jumpTimer = 0;
    private int fireworkTimer = 0;
    private boolean climbingAgain = false;

    private static final double BASE_HEIGHT = 350.0;
    private static final double HIGH_HEIGHT = 600.0;

    private final SettingGroup sg = this.settings.getDefaultGroup();
    private final Setting<Boolean> safetyFall = sg.add(new BoolSetting.Builder()
        .name("Safety Fall")
        .description("Evita quitarse las elytras al llegar para no morir por caída.")
        .defaultValue(true)
        .build()
    );

    public autopilot() {
        super(SunnyAddon.CATEGORY, "auto-pilot", "Vuelo automático con Reflexión.");
    }


    private double getPosReflected(Object entity, String method) {
        try {
            Method m = entity.getClass().getMethod(method);
            return (double) m.invoke(entity);
        } catch (Exception e) {
            return 0.0;
        }
    }

    private int getWorldTopY() {
        try {
            Method m = mc.world.getClass().getMethod("getTopY");
            return (int) m.invoke(mc.world);
        } catch (Exception e) {
            return 256;
        }
    }

    public void setTarget(double x, double z) {
        this.targetX = x;
        this.targetZ = z;
        this.starting = true;
        this.flying = false;
        this.flyingUp = true;
        this.jumpTimer = 0;
        this.fireworkTimer = 0;
        this.climbingAgain = false;
        this.elytraRemoved = false;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null) return;
        if (!starting && !flying) return;

        double pX = getPosReflected(mc.player, "getX");
        double pY = getPosReflected(mc.player, "getY");
        double pZ = getPosReflected(mc.player, "getZ");

        if (!mc.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA)) {
            info("¡Elytras no equipadas!");
            toggle();
            return;
        }

        if (!InvUtils.find(Items.FIREWORK_ROCKET).found()) {
            info("¡No se encontraron fuegos artificiales!");
            toggle();
            return;
        }

        BlockPos pos = mc.player.getBlockPos();
        int topY = getWorldTopY();
        for (int y = pos.getY() + 1; y < topY; ++y) {
            if (!mc.world.getBlockState(new BlockPos(pos.getX(), y, pos.getZ())).isAir()) {
                info("¡No hay espacio suficiente arriba!");
                toggle();
                return;
            }
        }

        if (starting) {
            jumpTimer++;
            if (jumpTimer == 1) mc.player.jump();
            if (jumpTimer == 4) {
                mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
            }
            if (jumpTimer == 7) {
                int slot = InvUtils.find(Items.FIREWORK_ROCKET).slot();
                InvUtils.swap(slot, false);
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                flying = true;
                starting = false;
                fireworkTimer = 0;
            }
            return;
        }

        if (flyingUp || climbingAgain) {
            mc.player.setPitch(-90.0f);
            fireworkTimer++;

            if (fireworkTimer >= 20) {
                int slot = InvUtils.find(Items.FIREWORK_ROCKET).slot();
                InvUtils.swap(slot, false);
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                fireworkTimer = 0;
            }

            if (flyingUp && pY >= BASE_HEIGHT) {
                flyingUp = false;
                climbingAgain = false;
                return;
            }
            if (climbingAgain && pY >= HIGH_HEIGHT) {
                climbingAgain = false;
                return;
            }
            return;
        }

        double distance = Math.hypot(targetX - pX, targetZ - pZ);

        if (!flyingUp && !climbingAgain && pY <= BASE_HEIGHT && distance > 200.0) {
            climbingAgain = true;
        }

        double dx = targetX - pX;
        double dz = targetZ - pZ;
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;

        mc.player.setYaw(yaw);
        mc.player.setPitch(0.0f);

        if (distance <= 3.0 && !safetyFall.get() && !elytraRemoved) {
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 6, 0, SlotActionType.PICKUP, mc.player);
            elytraRemoved = true;
        }

        if (mc.player.isOnGround()) {
            toggle();
        }
    }
}
