package com.sunny.addon.modules;

import com.sunny.addon.SunnyAddon;
import meteordevelopment.discordipc.DiscordIPC;
import meteordevelopment.discordipc.RichPresence;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.utils.StarscriptTextBoxRenderer;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringListSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.MeteorStarscript;
import meteordevelopment.orbit.EventHandler;
import org.meteordev.starscript.Script;

import java.util.ArrayList;
import java.util.List;

public class RPC extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<List<String>> line1 = sgGeneral.add(new StringListSetting.Builder()
        .name("Line 1")
        .defaultValue(List.of("Dominate with Sunny Addon! | Join NOW https://discord.gg/n3ffSY7wPb"))
        .renderer(StarscriptTextBoxRenderer.class)
        .build()
    );

    private final Setting<List<String>> line2 = sgGeneral.add(new StringListSetting.Builder()
        .name("Line 2")
        .defaultValue(List.of("{server.player_count} Players online", "{round(player.health, 1)}hp"))
        .renderer(StarscriptTextBoxRenderer.class)
        .build()
    );

    private final Setting<Integer> refreshDelay = sgGeneral.add(new IntSetting.Builder()
        .name("Refresh Delay")
        .defaultValue(100)
        .min(0)
        .sliderRange(0, 1000)
        .build()
    );

    private int ticks = 0;
    private int index1 = 0;
    private int index2 = 0;
    private static final RichPresence presence = new RichPresence();

    public RPC() {
        super(SunnyAddon.CATEGORY, "SunnyRPC", "Discord Rich Presence personalizada para Sunny Addon.");
    }

    @Override
    public void onActivate() {
        DiscordIPC.start(1477490021068963920L, null);
        presence.setStart(System.currentTimeMillis() / 1000L);
        updatePresence();
    }

    @Override
    public void onDeactivate() {
        DiscordIPC.stop();
    }

    @EventHandler(priority = 200)
    private void onTick(TickEvent.Pre event) {
        if (ticks > 0) {
            ticks--;
        } else {
            updatePresence();
        }
    }

    private void updatePresence() {
        ticks = refreshDelay.get();

        List<String> messages1 = getMessages(line1.get());
        List<String> messages2 = getMessages(line2.get());

        if (messages1.isEmpty() || messages2.isEmpty()) return;

        index1 = (index1 < messages1.size() - 1) ? index1 + 1 : 0;
        index2 = (index2 < messages2.size() - 1) ? index2 + 1 : 0;

        boolean inGame = mc.player != null;

        presence.setDetails(inGame ? messages1.get(index1) : "In Main Menu | Sunny Addon!");
        presence.setState(inGame ? messages2.get(index2) : "Idle");

        presence.setLargeImage("logo1", "v.2.0.0");

        DiscordIPC.setActivity(presence);
    }

    private List<String> getMessages(List<String> stateList) {
        List<String> messages = new ArrayList<>();

        for (String msg : stateList) {
            Script script = MeteorStarscript.compile(msg);
            if (script != null) {
                messages.add(MeteorStarscript.run(script));
            }
        }

        return messages;
    }
}
