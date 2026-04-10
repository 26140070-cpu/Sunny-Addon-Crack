package com.sunny.addon.modules;

import com.sunny.addon.SunnyAddon;
import com.sunny.addon.utils.KitStorage;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.BlockItem;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;

import java.util.Map;

public class ShulkerStealer extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("Ticks entre movimientos de items")
        .defaultValue(1)
        .min(0)
        .sliderMax(25)
        .build()
    );

    private int timer = 0;
    private boolean rekitRunning = false;

    public ShulkerStealer() {
        super(SunnyAddon.CATEGORY, "Shulker Stealer", "Re fill your inventory from your saved kit.");
    }

    @Override
    public void onActivate() {
        timer = 0;
        rekitRunning = false;
    }

    public void requestRekitFromGUI() {
        if (!KitStorage.hasActiveKit()) {
            error("Usa .kit load <nombre> primero");
        } else {
            KitStorage.requestRekit();
            info("Rekit solicitado: " + KitStorage.getActiveKitName());
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        if (mc.player.currentScreenHandler instanceof ShulkerBoxScreenHandler shulker) {
            if (timer > 0) {
                timer--;
            } else {
                if (KitStorage.consumeRekitRequest()) {
                    if (!KitStorage.hasActiveKit()) {
                        error("No hay un kit activo cargado.");
                        return;
                    }
                    rekitRunning = true;
                }

                if (rekitRunning) {
                    boolean moved = moveNextKitItem(shulker);
                    if (moved) {
                        timer = delay.get();
                    } else {
                        rekitRunning = false;
                        info("Rekit completado.");
                    }
                }
            }
        }
    }

    private boolean moveNextKitItem(ShulkerBoxScreenHandler shulker) {
        Map<Integer, String> kit = KitStorage.getActiveKit();
        if (kit == null) return false;

        int syncId = mc.player.currentScreenHandler.syncId;

        for (Map.Entry<Integer, String> entry : kit.entrySet()) {
            int playerSlot = entry.getKey();
            String itemId = entry.getValue();
            Identifier id = Identifier.of(itemId);

            if (id != null) {
                Item targetItem = Registries.ITEM.get(id);
                ItemStack invStack = mc.player.getInventory().getStack(playerSlot);

                if ((invStack.isEmpty() || invStack.getItem() != targetItem)
                    && (invStack.isEmpty() || !(invStack.getItem() instanceof BlockItem bi) || !(bi.getBlock() instanceof ShulkerBoxBlock))) {

                    for (int i = 0; i < 27; i++) {
                        Slot shulkerSlot = shulker.getSlot(i);
                        if (shulkerSlot.hasStack() && shulkerSlot.getStack().getItem() == targetItem) {
                            int targetScreenSlot = playerInventorySlotToScreenSlot(playerSlot);

                            mc.interactionManager.clickSlot(syncId, i, 0, SlotActionType.PICKUP, mc.player);
                            mc.interactionManager.clickSlot(syncId, targetScreenSlot, 0, SlotActionType.PICKUP, mc.player);

                            if (!mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
                                mc.interactionManager.clickSlot(syncId, i, 0, SlotActionType.PICKUP, mc.player);
                            }

                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private int playerInventorySlotToScreenSlot(int slot) {
        if (slot >= 0 && slot <= 8) {
            return 54 + slot;
        } else {
            return (slot >= 9 && slot <= 35) ? 27 + (slot - 9) : 54;
        }
    }
}
