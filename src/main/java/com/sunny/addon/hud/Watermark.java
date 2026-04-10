package com.sunny.addon.hud;

import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;

public class Watermark extends HudElement {
    public static final HudElementInfo<Watermark> MARKTEXT = new HudElementInfo<>(
        Hud.GROUP,
        "Sunny WaterMark",
        "Shows a Sunny Addon Watermark",
        Watermark::new
    );

    public Watermark() {
        super(MARKTEXT);
    }

    @Override
    public void render(HudRenderer renderer) {
        this.setSize(100.0, 30.0);

        renderer.text("Sunny Addon V2.0.0", x, y, Color.ORANGE, true);
    }
}
