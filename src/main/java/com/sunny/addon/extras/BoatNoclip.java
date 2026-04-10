package com.sunny.addon.extras;

import com.sunny.addon.SunnyAddon;
import meteordevelopment.meteorclient.events.entity.EntityMoveEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class BoatNoclip extends Module {
    private final SettingGroup sgSpeed = settings.createGroup("Speed");
    private final SettingGroup sgFlight = settings.createGroup("Flight");

    private final Setting<Boolean> speed = sgSpeed.add(new BoolSetting.Builder().name("speed").defaultValue(true).build());
    private final Setting<Double> horizontalSpeed = sgSpeed.add(new DoubleSetting.Builder().name("horizontal-speed").defaultValue(10.0).min(0.0).sliderMax(50.0).visible(speed::get).build());
    private final Setting<Double> horizontalSpeedInsideBlocks = sgSpeed.add(new DoubleSetting.Builder().name("horizontal-speed-inside-blocks").defaultValue(5.0).min(0.0).sliderMax(50.0).visible(speed::get).build());
    private final Setting<Double> verticalSpeed = sgSpeed.add(new DoubleSetting.Builder().name("vertical-speed").defaultValue(6.0).min(0.0).sliderMax(20.0).build());
    private final Setting<Double> fallSpeed = sgFlight.add(new DoubleSetting.Builder().name("fall-speed").defaultValue(0.0).min(0.0).build());
    private final Setting<Boolean> antiKick = sgFlight.add(new BoolSetting.Builder().name("anti-fly-kick").defaultValue(true).build());
    private final Setting<Integer> delay = sgFlight.add(new IntSetting.Builder().name("delay").defaultValue(40).min(1).sliderMax(80).visible(antiKick::get).build());

    private int delayLeft;
    private double lastPacketY = Double.MAX_VALUE;
    private boolean sentPacket;

    public BoatNoclip() {
        super(SunnyAddon.CATEGORY, "boat-noclip", "Fly through anything using boats | Tested on 1.21.11 paper, might not work on older versions.");
    }

    @Override
    public void onActivate() {
        delayLeft = delay.get();
        sentPacket = false;
        lastPacketY = Double.MAX_VALUE;
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        if (mc.player != null && mc.player.getVehicle() instanceof BoatEntity boat) {
            if (sentPacket) {
                Object packet = createVehicleMovePacket(boat);
                if (packet != null) {
                    setPacketY((VehicleMoveC2SPacket) packet, lastPacketY);
                    mc.player.networkHandler.sendPacket((VehicleMoveC2SPacket) packet);
                }
                sentPacket = false;
            }
        }
        delayLeft--;
    }

    @EventHandler
    private void onEntityMove(EntityMoveEvent event) {
        if (!(event.entity instanceof BoatEntity boat) || boat.getControllingPassenger() != mc.player) return;

        double vX = 0, vY = 0, vZ = 0;

        if (speed.get()) {
            double s = isInsideBlock(boat) ? horizontalSpeedInsideBlocks.get() : horizontalSpeed.get();
            Vec3d vel = PlayerUtils.getHorizontalVelocity(s);
            vX = vel.x;
            vZ = vel.z;
        }

        if (mc.currentScreen == null) {
            if (Input.isPressed(mc.options.jumpKey)) vY += verticalSpeed.get() / 20.0;
            else if (Input.isPressed(mc.options.sneakKey)) vY -= verticalSpeed.get() / 20.0;
            else vY -= fallSpeed.get() / 20.0;
        }

        boat.setYaw(mc.player.getYaw());

        try {
            Field movementField = event.getClass().getDeclaredField("movement");
            movementField.setAccessible(true);
            movementField.set(event, new Vec3d(vX, vY, vZ));
        } catch (Exception e) {
            event.movement = new Vec3d(vX, vY, vZ);
        }
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (!(event.packet instanceof VehicleMoveC2SPacket packet) || !antiKick.get()) return;
        if (mc.player == null || !(mc.player.getVehicle() instanceof BoatEntity)) return;

        double currentY = getPacketY(packet);

        if (delayLeft <= 0 && !sentPacket && currentY >= lastPacketY && EntityUtils.isOnAir(mc.player.getVehicle())) {
            setPacketY(packet, lastPacketY - 0.0313);
            sentPacket = true;
            delayLeft = delay.get();
        }
        lastPacketY = currentY;
    }


    private Object createVehicleMovePacket(BoatEntity boat) {
        try {
            double x = (double) callAnyMethod(boat, "getX", "method_23317");
            double y = (double) callAnyMethod(boat, "getY", "method_23318");
            double z = (double) callAnyMethod(boat, "getZ", "method_23321");
            float yaw = (float) callAnyMethod(boat, "getYaw", "method_36454");
            float pitch = (float) callAnyMethod(boat, "getPitch", "method_36455");

            try {
                Constructor<VehicleMoveC2SPacket> con = VehicleMoveC2SPacket.class.getConstructor(
                    double.class, double.class, double.class, float.class, float.class
                );
                return con.newInstance(x, y, z, yaw, pitch);
            } catch (NoSuchMethodException e) {
                Constructor<VehicleMoveC2SPacket> con = VehicleMoveC2SPacket.class.getConstructor(Entity.class);
                return con.newInstance(boat);
            }
        } catch (Exception e) {
            return null;
        }
    }

    private double getPacketY(VehicleMoveC2SPacket packet) {
        try {
            return (double) getAnyField(packet, "y", "field_13034", "comp_3351");
        } catch (Exception e) { return 0; }
    }

    private void setPacketY(VehicleMoveC2SPacket packet, double y) {
        try {
            setAnyField(packet, y, "y", "field_13034", "comp_3351");
        } catch (Exception ignored) {}
    }

    private boolean isInsideBlock(Entity entity) {
        Box box = entity.getBoundingBox();
        BlockPos.Mutable pos = new BlockPos.Mutable();
        for (int x = (int) Math.floor(box.minX); x <= (int) Math.floor(box.maxX); x++) {
            for (int y = (int) Math.floor(box.minY); y <= (int) Math.floor(box.maxY); y++) {
                for (int z = (int) Math.floor(box.minZ); z <= (int) Math.floor(box.maxZ); z++) {
                    pos.set(x, y, z);
                    if (mc.world != null && !mc.world.getBlockState(pos).isAir()) return true;
                }
            }
        }
        return false;
    }

    private Object getAnyField(Object obj, String... names) throws Exception {
        Class<?> clazz = obj.getClass();
        while (clazz != null) {
            for (String name : names) {
                try {
                    Field f = clazz.getDeclaredField(name);
                    f.setAccessible(true);
                    return f.get(obj);
                } catch (NoSuchFieldException ignored) {}
            }
            clazz = clazz.getSuperclass();
        }
        throw new NoSuchFieldException();
    }

    private void setAnyField(Object obj, Object value, String... names) throws Exception {
        Class<?> clazz = obj.getClass();
        while (clazz != null) {
            for (String name : names) {
                try {
                    Field f = clazz.getDeclaredField(name);
                    f.setAccessible(true);
                    f.set(obj, value);
                    return;
                } catch (NoSuchFieldException ignored) {}
            }
            clazz = clazz.getSuperclass();
        }
    }

    private Object callAnyMethod(Object obj, String... names) throws Exception {
        Class<?> clazz = obj.getClass();
        while (clazz != null) {
            for (String name : names) {
                try {
                    Method m = clazz.getDeclaredMethod(name);
                    m.setAccessible(true);
                    return m.invoke(obj);
                } catch (NoSuchMethodException ignored) {}
            }
            clazz = clazz.getSuperclass();
        }
        throw new NoSuchMethodException();
    }
}
