package com.sunny.addon.modules;

import com.sunny.addon.SunnyAddon;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

public class JumpCircle extends Module {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();

    private final Setting<SettingColor> color = sgGeneral.add(new ColorSetting.Builder()
        .name("color")
        .defaultValue(new SettingColor(255, 255, 255, 255))
        .build()
    );

    private final Setting<Double> maxRadius = sgGeneral.add(new DoubleSetting.Builder()
        .name("max-radius")
        .defaultValue(2.0)
        .min(0.5)
        .sliderMax(5.0)
        .build()
    );

    private final Setting<Integer> duration = sgGeneral.add(new IntSetting.Builder()
        .name("duration-ms")
        .defaultValue(2000)
        .min(500)
        .sliderMax(5000)
        .build()
    );

    private final Setting<Boolean> onlySelf = sgGeneral.add(new BoolSetting.Builder()
        .name("only-self")
        .defaultValue(false)
        .build()
    );

    private final List<CircleInstance> circles = new ArrayList<>();
    private final List<PlayerEntity> jumpingPlayers = new ArrayList<>();

    private static final Field ENTITY_X = findField(PlayerEntity.class, "field_6038", "x");
    private static final Field ENTITY_Y = findField(PlayerEntity.class, "field_5971", "y");
    private static final Field ENTITY_Z = findField(PlayerEntity.class, "field_5989", "z");

    public JumpCircle() {
        super(SunnyAddon.CATEGORY, "zJumpCircle", "Draw a circle when someone jumps");
    }

    private static Field findField(Class<?> clazz, String intermediary, String yarn) {
        try {
            Field f = clazz.getSuperclass().getDeclaredField(intermediary);
            f.setAccessible(true);
            return f;
        } catch (Exception e) {
            try {
                Field f = clazz.getSuperclass().getDeclaredField(yarn);
                f.setAccessible(true);
                return f;
            } catch (Exception e2) {
                return null;
            }
        }
    }

    private double getReflectivePos(PlayerEntity player, Field field) {
        if (field == null) return 0;
        try {
            return field.getDouble(player);
        } catch (Exception e) {
            return 0;
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.world == null || mc.player == null) return;

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (!onlySelf.get() || player == mc.player) {
                if (!player.isOnGround() && !jumpingPlayers.contains(player)) {
                    double x = getReflectivePos(player, ENTITY_X);
                    double y = getReflectivePos(player, ENTITY_Y);
                    double z = getReflectivePos(player, ENTITY_Z);

                    circles.add(new CircleInstance(x, y, z, System.currentTimeMillis()));
                    jumpingPlayers.add(player);
                } else if (player.isOnGround()) {
                    jumpingPlayers.remove(player);
                }
            }
        }

        circles.removeIf(c -> System.currentTimeMillis() - c.startTime > duration.get());
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (circles.isEmpty()) return;

        for (CircleInstance c : circles) {
            long aliveTime = System.currentTimeMillis() - c.startTime;
            double progress = (double) aliveTime / duration.get();
            double currentRadius = progress * maxRadius.get();

            int alpha = (int) (color.get().a * (1.0 - progress));
            if (alpha < 0) alpha = 0;

            SettingColor renderColor = new SettingColor(color.get().r, color.get().g, color.get().b, alpha);
            drawCircle(event, c, currentRadius, renderColor);
        }
    }

    private void drawCircle(Render3DEvent event, CircleInstance c, double radius, SettingColor col) {
        int segments = 64;

        for (int i = 0; i < segments; i++) {
            double angle = i * 2.0 * Math.PI / segments;
            double nextAngle = (i + 1) * 2.0 * Math.PI / segments;

            double x1 = c.x + Math.cos(angle) * radius;
            double z1 = c.z + Math.sin(angle) * radius;
            double x2 = c.x + Math.cos(nextAngle) * radius;
            double z2 = c.z + Math.sin(nextAngle) * radius;

            event.renderer.line(x1, c.y, z1, x2, c.y, z2, col);
        }
    }

    private static class CircleInstance {
        public final double x, y, z;
        public final long startTime;

        public CircleInstance(double x, double y, double z, long startTime) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.startTime = startTime;
        }
    }
}
