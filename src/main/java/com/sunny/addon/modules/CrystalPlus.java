package com.sunny.addon.modules;

import com.sunny.addon.SunnyAddon;
import java.lang.reflect.Field;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.entity.player.PlayerInventory;

public class CrystalPlus extends Module {

    private static final Field INV_SLOT_FIELD = findField(PlayerInventory.class, "field_7545", "selectedSlot");

    public CrystalPlus() {
        super(SunnyAddon.CATEGORY, "Crystal+", "Crystal Aura But Better.");
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

    private void setReflectiveSlot(int slot) {
        if (INV_SLOT_FIELD == null) return;
        try {
            INV_SLOT_FIELD.setInt(mc.player.getInventory(), slot);
        } catch (Exception e) {
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null) return;

        int slotActual = getReflectiveSlot();

        if (slotActual != 1) {
            setReflectiveSlot(1);
        }
    }
}
