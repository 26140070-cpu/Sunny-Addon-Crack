
package com.sunny.addon.modules;

import com.sunny.addon.SunnyAddon;
import java.lang.reflect.Field;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.Hand;

public class Dupes extends Module {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();

    private final Setting<DupeMode> dupesmode = sgGeneral.add(new EnumSetting.Builder<DupeMode>()
        .name("mode")
        .defaultValue(DupeMode.ItemFrame)
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .defaultValue(5)
        .min(1)
        .sliderMax(20)
        .visible(() -> dupesmode.get() == DupeMode.ItemFrame)
        .build()
    );

    private final Setting<Boolean> autohit = sgGeneral.add(new BoolSetting.Builder()
        .name("Auto-Hit")
        .defaultValue(true)
        .visible(() -> dupesmode.get() == DupeMode.ItemFrame)
        .build()
    );

    private int timer = 0;
    private int commandTimer = 0;

    private static final Field INV_SLOT_FIELD = findField(PlayerInventory.class, "field_7545", "selectedSlot");

    public Dupes() {
        super(SunnyAddon.CATEGORY, "Dupes", "Simple Dupes Methods");
    }

    private static Field findField(Class<?> clazz, String intermediary, String yarn) {
        try {
            Field f = clazz.getDeclaredField(intermediary);
            f.setAccessible(true);
            return f;
        } catch (Exception e1) {
            try {
                Field f = clazz.getDeclaredField(yarn);
                f.setAccessible(true);
                return f;
            } catch (Exception e2) {
                return null;
            }
        }
    }

    private int getReflectiveSlot() {
        if (INV_SLOT_FIELD == null) return 0;
        try {
            return INV_SLOT_FIELD.getInt(mc.player.getInventory());
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public void onActivate() {
        this.timer = 0;
        this.commandTimer = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null) return;

        if (dupesmode.get() == DupeMode.ItemFrame) {
            this.timer++;
            if (this.timer < delay.get()) return;
            this.timer = 0;

            for (Entity entity : mc.world.getEntities()) {
                if (entity instanceof ItemFrameEntity frame) {
                    if (mc.player.distanceTo(frame) <= 4.5) {
                        mc.interactionManager.interactEntity(mc.player, frame, Hand.MAIN_HAND);

                        if (autohit.get() && !frame.getHeldItemStack().isEmpty()) {
                            mc.interactionManager.attackEntity(mc.player, frame);
                        }
                    }
                }
            }
        }
        else if (dupesmode.get() == DupeMode.DupeCommand) {
            this.commandTimer++;
            if (this.commandTimer < 80) return;
            this.commandTimer = 0;

            mc.player.networkHandler.sendChatCommand("dupe");
        }
    }

    public enum DupeMode {
        ItemFrame,
        DupeCommand
    }
}
