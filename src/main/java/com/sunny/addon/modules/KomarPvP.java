package com.sunny.addon.modules;

import com.sunny.addon.SunnyAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

import java.lang.reflect.Field;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class KomarPvP extends Module {
    private final SettingGroup sg = settings.getDefaultGroup();

    private final Setting<Weapon> weaponMode = sg.add(new EnumSetting.Builder<Weapon>()
        .name("weapon-mode")
        .defaultValue(Weapon.SWORD)
        .build()
    );

    private final Setting<Double> range = sg.add(new DoubleSetting.Builder().name("range").defaultValue(80.0).min(10.0).sliderMax(200.0).build());
    private final Setting<Double> speed = sg.add(new DoubleSetting.Builder().name("speed").defaultValue(1.8).min(0.2).sliderMax(5.0).build());
    private final Setting<Double> height = sg.add(new DoubleSetting.Builder().name("height").defaultValue(15.0).min(5.0).sliderMax(40.0).build());
    private final Setting<Boolean> silentswitch = sg.add(new BoolSetting.Builder().name("silent-switch").defaultValue(true).visible(() -> weaponMode.get() != Weapon.SPEAR).build());

    private PlayerEntity target;
    private boolean goingUp = true;
    private int spearTimer;
    private int jumptimer;

    private static final Field FIELD_X = findField(Entity.class, "field_6038", "x");
    private static final Field FIELD_Y = findField(Entity.class, "field_5971", "y");
    private static final Field FIELD_Z = findField(Entity.class, "field_5989", "z");

    private static final Field SLOT_REF = findSlotField();

    public enum Weapon { MACE, SPEAR, SWORD }

    public KomarPvP() {
        super(SunnyAddon.CATEGORY, "FlyTarget", "Módulo de combate con vuelo dinámico.");
    }

    private static Field findField(Class<?> clazz, String intermediary, String yarn) {
        try {
            Field f = clazz.getDeclaredField(intermediary);
            f.setAccessible(true);
            return f;
        } catch (Exception e) {
            try {
                Field f = clazz.getDeclaredField(yarn);
                f.setAccessible(true);
                return f;
            } catch (Exception e2) { return null; }
        }
    }

    private static Field findSlotField() {
        Field f = findField(PlayerInventory.class, "field_7545", "selectedSlot");
        if (f != null) return f;

        for (Field field : PlayerInventory.class.getDeclaredFields()) {
            if (field.getType() == int.class && !java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                field.setAccessible(true);
                return field;
            }
        }
        return null;
    }

    private double getVal(Entity e, Field f) {
        try { return f.getDouble(e); } catch (Exception ex) { return 0; }
    }

    private void changeSlot(int slot) {
        if (SLOT_REF != null) {
            try { SLOT_REF.setInt(mc.player.getInventory(), slot); } catch (Exception ignored) {}
        }
    }

    private int getCurrentSlot() {
        if (SLOT_REF != null) {
            try { return SLOT_REF.getInt(mc.player.getInventory()); } catch (Exception ignored) {}
        }
        return 0;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null) return;

        double pX = getVal(mc.player, FIELD_X);
        double pY = getVal(mc.player, FIELD_Y);
        double pZ = getVal(mc.player, FIELD_Z);

        if (!mc.player.getAbilities().flying) {
            jumptimer++;
            if (jumptimer == 1) mc.player.jump();
            if (jumptimer == 4) mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
        } else { jumptimer = 0; }

        List<PlayerEntity> targets = mc.world.getPlayers().stream()
            .filter(p -> p != mc.player && p.isAlive() && !p.isCreative())
            .sorted(Comparator.comparingDouble(p -> mc.player.distanceTo(p)))
            .collect(Collectors.toList());

        if (targets.isEmpty()) {
            target = null;
            return;
        }

        target = targets.get(0);
        double tX = getVal(target, FIELD_X);
        double tY = getVal(target, FIELD_Y);
        double tZ = getVal(target, FIELD_Z);

        if (mc.player.distanceTo(target) > range.get()) return;

        double limitY = tY + height.get();
        if (goingUp) {
            if (pY < limitY) mc.player.setVelocity(0, speed.get(), 0);
            else goingUp = false;
        } else {
            mc.player.lookAt(net.minecraft.command.argument.EntityAnchorArgumentType.EntityAnchor.EYES, new Vec3d(tX, tY, tZ));
            Vec3d dir = new Vec3d(tX - pX, tY - pY, tZ - pZ).normalize();
            double v = (weaponMode.get() == Weapon.SPEAR) ? speed.get() * 1.5 : speed.get();
            mc.player.setVelocity(dir.multiply(v).add(new Vec3d(-dir.z, 0, dir.x).multiply(0.3)));

            if (mc.player.distanceTo(target) < (weaponMode.get() == Weapon.SPEAR ? 4.5 : 3.5)) {
                doAttack();
                goingUp = true;
            }
        }
    }

    private void doAttack() {
        if (target == null) return;

        if (weaponMode.get() == Weapon.MACE && silentswitch.get()) {
            int maceSlot = -1;
            for (int i = 0; i < 9; i++) {
                if (mc.player.getInventory().getStack(i).getItem().toString().toLowerCase().contains("mace")) {
                    maceSlot = i; break;
                }
            }
            if (maceSlot != -1) {
                int old = getCurrentSlot();
                changeSlot(maceSlot);
                mc.interactionManager.attackEntity(mc.player, target);
                mc.player.swingHand(Hand.MAIN_HAND);
                changeSlot(old);
                return;
            }
        }

        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);
    }
}
