package com.sunny.addon.modules;

import com.sunny.addon.SunnyAddon;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;

public class SunnyCape extends Module {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();

    private final Setting<Boolean> onlySelf = sgGeneral.add(new BoolSetting.Builder()
        .name("only-self")
        .description("Si se activa, solo tú verás tu capa. Si se desactiva, verás la capa en otros jugadores.")
        .defaultValue(true)
        .build()
    );

    public SunnyCape() {
        super(SunnyAddon.CATEGORY, "Sunny Cape", "Renderiza la capa oficial de Sunny Addon.");
    }

    public boolean onlySelf() {
        return this.onlySelf.get();
    }
}
