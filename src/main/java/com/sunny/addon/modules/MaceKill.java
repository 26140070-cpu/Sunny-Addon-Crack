package com.sunny.addon.modules;

import com.sunny.addon.SunnyAddon;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IPlayerInteractEntityC2SPacket;
import meteordevelopment.meteorclient.mixininterface.IPlayerMoveC2SPacket;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.BlockPos;

import java.lang.reflect.Field;

public class MaceKill extends Module {
    private final SettingGroup sgValues = settings.createGroup("Settings");

    private final Setting<Integer> fallHeight = sgValues.add(new IntSetting.Builder().name("Fall height").defaultValue(23).min(1).sliderRange(1, 1000).build());
    private final Setting<Integer> hit1Height = sgValues.add(new IntSetting.Builder().name("Hit 1").defaultValue(23).min(1).sliderRange(1, 1000).build());
    private final Setting<Integer> hit2Height = sgValues.add(new IntSetting.Builder().name("Hit 2").defaultValue(40).min(1).sliderRange(1, 1000).build());
    private final Setting<Integer> hit3Height = sgValues.add(new IntSetting.Builder().name("Hit 3").defaultValue(60).min(1).sliderRange(1, 1000).build());
    private final Setting<Integer> hit4Height = sgValues.add(new IntSetting.Builder().name("Hit 4").defaultValue(80).min(1).sliderRange(1, 1000).build());
    private final Setting<Integer> hit5Height = sgValues.add(new IntSetting.Builder().name("Hit 5").defaultValue(160).min(1).sliderRange(1, 1000).build());
    private final Setting<Integer> hit6Height = sgValues.add(new IntSetting.Builder().name("Hit 6").defaultValue(200).min(1).sliderRange(1, 1000).build());

    private final Setting<Integer> noGroundPackets = settings.getDefaultGroup().add(new IntSetting.Builder().name("No Ground Packets").defaultValue(4).min(1).sliderRange(1, 10).build());
    private final Setting<Boolean> doTotemFail = settings.getDefaultGroup().add(new BoolSetting.Builder().name("Do TotemFail").defaultValue(true).build());
    private final Setting<Integer> hitAmount = settings.getDefaultGroup().add(new IntSetting.Builder().name("Hit Amount").defaultValue(1).min(0).sliderRange(0, 50).build());
    private final Setting<Boolean> silentSwitch = settings.getDefaultGroup().add(new BoolSetting.Builder().name("Silent Switch").defaultValue(true).build());

    private LivingEntity target;
    private boolean isAttacking;
    private boolean didSwap;
    private int originalSlot = -1;

    private static final Field SLOT_FIELD = findField(PlayerInventory.class, "field_7545", "selectedSlot");
    private static final Field ENT_X = findField(Entity.class, "field_6038", "x");
    private static final Field ENT_Y = findField(Entity.class, "field_5971", "y");
    private static final Field ENT_Z = findField(Entity.class, "field_5989", "z");

    public MaceKill() {
        super(SunnyAddon.CATEGORY, "UltraKill", "MaceKill But Better");
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

    private double getX(Entity e) { try { return ENT_X.getDouble(e); } catch (Exception ex) { return e.getX(); } }
    private double getY(Entity e) { try { return ENT_Y.getDouble(e); } catch (Exception ex) { return e.getY(); } }
    private double getZ(Entity e) { try { return ENT_Z.getDouble(e); } catch (Exception ex) { return e.getZ(); } }

    private void setSlot(int slot) { try { SLOT_FIELD.setInt(mc.player.getInventory(), slot); } catch (Exception ignored) {} }
    private int getSlot() { try { return SLOT_FIELD.getInt(mc.player.getInventory()); } catch (Exception e) { return 0; } }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (!isAttacking && mc.player != null && mc.getNetworkHandler() != null) {
            if (event.packet instanceof IPlayerInteractEntityC2SPacket pkt) {
                if (!(pkt.meteor$getEntity() instanceof LivingEntity living)) return;
                this.target = living;
                this.isAttacking = true;

                if (silentSwitch.get()) performSilentSwitch();

                for (int i = 0; i < hitAmount.get(); i++) {
                    if (doTotemFail.get()) {
                        int[] hs = {hit1Height.get(), hit2Height.get(), hit3Height.get(), hit4Height.get(), hit5Height.get(), hit6Height.get()};
                        for (int h : hs) {
                            performSilentTp(h);
                            mc.getNetworkHandler().sendPacket(event.packet);
                        }
                    } else {
                        performSilentTp(fallHeight.get());
                        mc.getNetworkHandler().sendPacket(event.packet);
                    }
                }
                this.isAttacking = false;
            }
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (didSwap && silentSwitch.get() && mc.player != null) {
            setSlot(originalSlot);
            didSwap = false;
            originalSlot = -1;
        }
    }

    private void performSilentTp(int height) {
        if (mc.player == null || mc.world == null) return;

        double oX = getX(mc.player);
        double oY = getY(mc.player);
        double oZ = getZ(mc.player);

        int fallBlocks = findOpenAirBlocks(height);
        if (fallBlocks <= 0) return;

        for (int i = 0; i < noGroundPackets.get(); i++) {
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(false, mc.player.horizontalCollision));
        }

        PlayerMoveC2SPacket.PositionAndOnGround up = new PlayerMoveC2SPacket.PositionAndOnGround(oX, oY + fallBlocks, oZ, false, mc.player.horizontalCollision);
        PlayerMoveC2SPacket.PositionAndOnGround down = new PlayerMoveC2SPacket.PositionAndOnGround(oX, oY, oZ, false, mc.player.horizontalCollision);

        ((IPlayerMoveC2SPacket) up).meteor$setTag(1337);
        ((IPlayerMoveC2SPacket) down).meteor$setTag(1337);

        mc.getNetworkHandler().sendPacket(up);
        mc.getNetworkHandler().sendPacket(down);
    }

    private int findOpenAirBlocks(int maxHeight) {
        BlockPos base = new BlockPos((int)getX(mc.player), (int)getY(mc.player), (int)getZ(mc.player));
        for (int i = maxHeight; i > 0; i--) {
            BlockPos p = base.up(i);
            if (mc.world.getBlockState(p).isAir() && mc.world.getBlockState(p.up()).isAir()) return i;
        }
        return 0;
    }

    private void performSilentSwitch() {
        FindItemResult r = InvUtils.findInHotbar(s -> s.getItem().toString().toLowerCase().contains("mace"));
        if (r.found()) {
            this.originalSlot = getSlot();
            this.didSwap = true;
            setSlot(r.slot());
        }
    }
}
