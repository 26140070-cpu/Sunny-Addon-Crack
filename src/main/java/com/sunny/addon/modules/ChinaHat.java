package com.sunny.addon.modules;

import com.sunny.addon.SunnyAddon;
import java.lang.reflect.Field;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.MathHelper;

public class ChinaHat extends Module {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();

    private final Setting<Double> radius = sgGeneral.add(new DoubleSetting.Builder().name("radius").defaultValue(0.8).min(0.1).sliderMax(2.0).build());
    private final Setting<Double> height = sgGeneral.add(new DoubleSetting.Builder().name("height").defaultValue(0.5).min(0.1).sliderMax(1.5).build());
    private final Setting<Double> width = sgGeneral.add(new DoubleSetting.Builder().name("width").defaultValue(1.0).min(0.1).sliderMax(3.0).build());
    private final Setting<Double> length = sgGeneral.add(new DoubleSetting.Builder().name("length").defaultValue(1.0).min(0.1).sliderMax(3.0).build());

    private final Setting<Boolean> firstPerson = sgGeneral.add(new BoolSetting.Builder().name("first-person").defaultValue(false).build());
    private final Setting<Boolean> rainbow = sgGeneral.add(new BoolSetting.Builder().name("rainbow").defaultValue(false).build());

    private final Setting<SettingColor> color1 = sgGeneral.add(new ColorSetting.Builder()
        .name("color-1")
        .defaultValue(new SettingColor(255, 0, 0, 150))
        .visible(() -> !rainbow.get())
        .build());

    private final Setting<SettingColor> color2 = sgGeneral.add(new ColorSetting.Builder()
        .name("color-2")
        .defaultValue(new SettingColor(0, 255, 0, 150))
        .visible(() -> !rainbow.get())
        .build());

    private final Setting<Boolean> gradient = sgGeneral.add(new BoolSetting.Builder()
        .name("gradient")
        .defaultValue(true)
        .visible(() -> !rainbow.get())
        .build());

    public ChinaHat() {
        super(SunnyAddon.CATEGORY, "zChinaHat", "ChinaHat estilo ThunderHack con soporte de reflexión.");
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (mc.player == null) return;

        boolean isFirstPerson = mc.options.getPerspective().isFirstPerson();
        if (isFirstPerson && !firstPerson.get()) return;

        double prevX = getPosReflective("field_6038", "prevX");
        double prevY = getPosReflective("field_5971", "prevY");
        double prevZ = getPosReflective("field_5989", "prevZ");

        double lerpX = MathHelper.lerp(event.tickDelta, prevX, mc.player.getX());
        double lerpY = MathHelper.lerp(event.tickDelta, prevY, mc.player.getY());
        double lerpZ = MathHelper.lerp(event.tickDelta, prevZ, mc.player.getZ());

        double x = lerpX;
        double y = lerpY + mc.player.getHeight() + 0.1;
        double z = lerpZ;

        long time = System.currentTimeMillis();
        int segments = 64;

        for (int i = 0; i < segments; i++) {
            double angle1 = (double) i / segments * Math.PI * 2.0;
            double angle2 = (double) (i + 1) / segments * Math.PI * 2.0;

            double x1 = Math.cos(angle1) * radius.get() * width.get();
            double z1 = Math.sin(angle1) * radius.get() * length.get();
            double x2 = Math.cos(angle2) * radius.get() * width.get();
            double z2 = Math.sin(angle2) * radius.get() * length.get();

            Color c1, c2;

            if (rainbow.get()) {
                c1 = getRainbow(time, i * 10);
                c2 = getRainbow(time, (i + 1) * 10);
            } else if (gradient.get()) {
                double wave1 = (Math.sin(time * 0.004 + (double) i / segments * Math.PI * 2.0) + 1.0) / 2.0;
                double wave2 = (Math.sin(time * 0.004 + (double) (i + 1) / segments * Math.PI * 2.0) + 1.0) / 2.0;
                c1 = interpolate(color1.get(), color2.get(), wave1);
                c2 = interpolate(color1.get(), color2.get(), wave2);
            } else {
                c1 = color1.get();
                c2 = color2.get();
            }

            event.renderer.line(x, y + height.get(), z, x + x1, y, z + z1, c1);
            event.renderer.line(x + x1, y, z + z1, x + x2, y, z + z2, c2);
        }
    }

    private double getPosReflective(String intermediaryName, String yarnName) {
        try {
            Field field;
            try {
                field = mc.player.getClass().getField(intermediaryName);
            } catch (NoSuchFieldException e) {
                field = mc.player.getClass().getField(yarnName);
            }
            field.setAccessible(true);
            return field.getDouble(mc.player);
        } catch (Exception e) {
            if (yarnName.contains("X")) return mc.player.getX();
            if (yarnName.contains("Y")) return mc.player.getY();
            return mc.player.getZ();
        }
    }

    private Color getRainbow(long time, int offset) {
        float hue = (float) ((time + offset) % 5000L) / 5000.0F;
        int rgb = java.awt.Color.HSBtoRGB(hue, 0.8F, 1.0F);
        return new Color(rgb >> 16 & 0xFF, rgb >> 8 & 0xFF, rgb & 0xFF, 150);
    }

    private Color interpolate(Color c1, Color c2, double progress) {
        return new Color(
            (int) (c1.r + (c2.r - c1.r) * progress),
            (int) (c1.g + (c2.g - c1.g) * progress),
            (int) (c1.b + (c2.b - c1.b) * progress),
            (int) (c1.a + (c2.a - c1.a) * progress)
        );
    }
}
