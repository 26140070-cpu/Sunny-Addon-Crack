
package com.sunny.addon.utils;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class SInvUtils {
    public static int[] lastSwappedSlots = new int[]{-1, -1};

    public static boolean invSwitch(int slot) {
        try {
            Object inventory = getAnyField(MeteorClient.mc.player, "inventory", "field_7514");
            int selectedSlot = (int) getAnyField(inventory, "selectedSlot", "field_7545");
            return invSwitch(slot, selectedSlot);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean invSwitch(int inventorySlot, int hotbarSlot) {
        if (inventorySlot < 0 || hotbarSlot < 0 || hotbarSlot > 8) return false;

        try {
            Object handler = MeteorClient.mc.player.currentScreenHandler;

            int syncId = (int) getAnyField(handler, "syncId", "field_7763");
            int revision = 0;
            try {
                revision = (int) callAnyMethod(handler, "getRevision", "method_37421");
            } catch (Exception ignored) {}

            Object emptyStack = getAnyField(ItemStack.class, "EMPTY", "field_8038");

            Int2ObjectArrayMap<Object> changedSlots = new Int2ObjectArrayMap<>();
            changedSlots.put(inventorySlot, emptyStack);

            Constructor<?> constructor = ClickSlotC2SPacket.class.getConstructors()[0];
            Object packet = constructor.newInstance(
                syncId,
                revision,
                (short) inventorySlot,
                (byte) hotbarSlot,
                SlotActionType.SWAP,
                changedSlots,
                emptyStack
            );

            if (MeteorClient.mc.getNetworkHandler() != null) {
                MeteorClient.mc.getNetworkHandler().sendPacket((net.minecraft.network.packet.Packet<?>) packet);
            }

            try {
                Method sync = MeteorClient.mc.interactionManager.getClass().getDeclaredMethod("meteor$syncSelected");
                sync.setAccessible(true);
                sync.invoke(MeteorClient.mc.interactionManager);
            } catch (Exception ignored) {}

            lastSwappedSlots = new int[]{inventorySlot, hotbarSlot};
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    private static Object getAnyField(Object obj, String... names) throws Exception {
        Class<?> clazz = (obj instanceof Class) ? (Class<?>) obj : obj.getClass();
        while (clazz != null) {
            for (String name : names) {
                try {
                    Field f = clazz.getDeclaredField(name);
                    f.setAccessible(true);
                    return f.get(obj instanceof Class ? null : obj);
                } catch (NoSuchFieldException ignored) {}
            }
            clazz = clazz.getSuperclass();
        }
        throw new NoSuchFieldException("Campo no encontrado");
    }

    private static Object callAnyMethod(Object obj, String... names) throws Exception {
        Class<?> clazz = obj.getClass();
        while (clazz != null) {
            for (String name : names) {
                try {
                    Method m = clazz.getDeclaredMethod(name);
                    m.setAccessible(true);
                    return m.invoke(obj);
                } catch (Exception ignored) {}
            }
            clazz = clazz.getSuperclass();
        }
        throw new NoSuchMethodException("Método no encontrado");
    }
}
