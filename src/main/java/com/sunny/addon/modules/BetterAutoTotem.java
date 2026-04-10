package com.sunny.addon.modules;

import com.sunny.addon.SunnyAddon;
import java.util.List;
import java.lang.reflect.Field;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.AutoTotem;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class BetterAutoTotem extends Module {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("Mode Of Totem")
        .description("Selecciona el modo de funcionamiento.")
        .defaultValue(Mode.Normal)
        .build());

    private final Setting<Boolean> vinculateCA = sgGeneral.add(new BoolSetting.Builder()
        .name("Vincule Crystal+")
        .description("Apaga Crystal+ automáticamente si la vida es baja.")
        .defaultValue(true)
        .visible(() -> mode.get() == Mode.Integration)
        .build());

    private final Setting<Integer> minHealth = sgGeneral.add(new IntSetting.Builder()
        .name("Min Health")
        .description("Vida mínima para cambiar a tótem en modo Integración.")
        .defaultValue(10)
        .min(1)
        .sliderMax(20)
        .visible(() -> mode.get() == Mode.Integration)
        .build());

    private final Setting<Boolean> offHand = sgGeneral.add(new BoolSetting.Builder()
        .name("Off Hand")
        .defaultValue(true)
        .visible(() -> mode.get() == Mode.Normal)
        .build());

    private final Setting<Boolean> mainHand = sgGeneral.add(new BoolSetting.Builder()
        .name("MainHand")
        .defaultValue(false)
        .visible(() -> mode.get() == Mode.Normal)
        .build());

    private final Setting<Boolean> badPing = sgGeneral.add(new BoolSetting.Builder()
        .name("BadPing")
        .description("Mantiene tótems extra en la hotbar para evitar muertes por lag.")
        .defaultValue(false)
        .visible(() -> mode.get() == Mode.Normal)
        .build());

    private final Setting<List<Item>> ignoreItems = sgGeneral.add(new ItemListSetting.Builder()
        .name("Ignore Items")
        .description("Items que no se quitarán de la mano izquierda.")
        .defaultValue(List.of(Items.END_CRYSTAL))
        .build());

    private boolean crystalWasActive = false;

    public BetterAutoTotem() {
        super(SunnyAddon.CATEGORY, "MegaTotem", "Better totem.");
    }

    @Override
    public void onActivate() {
        AutoTotem meteorTotem = Modules.get().get(AutoTotem.class);
        if (meteorTotem.isActive()) meteorTotem.toggle();
    }

    @Override
    public void onDeactivate() {
        if (vinculateCA.get()) {
            CrystalPlus crystalPlus = Modules.get().get(CrystalPlus.class);
            if (crystalPlus != null && crystalWasActive && !crystalPlus.isActive()) {
                crystalPlus.toggle();
            }
        }
        crystalWasActive = false;
    }

    @EventHandler(priority = 200)
    private void onTick(Pre event) {
        if (mc.player == null || mc.interactionManager == null) return;

        if (mode.get() == Mode.Integration) {
            handleIntegrationMode();
        } else {
            handleNormalMode();
        }
    }

    private void handleNormalMode() {
        Item offhandItem = mc.player.getOffHandStack().getItem();
        if (!ignoreItems.get().contains(offhandItem) && offHand.get() && offhandItem != Items.TOTEM_OF_UNDYING) {
            FindItemResult totem = InvUtils.find(Items.TOTEM_OF_UNDYING);
            if (totem.found()) {
                InvUtils.move().from(totem.slot()).toOffhand();
            }
        }

        if (badPing.get()) {
            int totemsInHotbar = 0;
            for (int i = 0; i < 9; i++) {
                if (mc.player.getInventory().getStack(i).getItem() == Items.TOTEM_OF_UNDYING) totemsInHotbar++;
            }

            if (totemsInHotbar < 2) {
                FindItemResult totemInInv = InvUtils.find(stack -> stack.getItem() == Items.TOTEM_OF_UNDYING, 9, 35);
                FindItemResult emptySlot = InvUtils.find(ItemStack::isEmpty, 0, 8);
                if (totemInInv.found() && emptySlot.found()) {
                    InvUtils.move().from(totemInInv.slot()).to(emptySlot.slot());
                }
            }
        }

        if (mainHand.get()) {
            if (mc.player.getMainHandStack().getItem() == Items.TOTEM_OF_UNDYING) return;

            FindItemResult totemInHotbar = InvUtils.find(stack -> stack.getItem() == Items.TOTEM_OF_UNDYING, 0, 8);
            if (totemInHotbar.found()) {
                InvUtils.swap(totemInHotbar.slot(), true);
            } else {
                FindItemResult totemInInv = InvUtils.find(stack -> stack.getItem() == Items.TOTEM_OF_UNDYING, 9, 35);
                if (totemInInv.found()) {
                    int currentSlot = getSelectedSlotReflective();
                    FindItemResult emptySlot = InvUtils.find(ItemStack::isEmpty, 0, 8);

                    if (emptySlot.found()) {
                        InvUtils.move().from(currentSlot).to(emptySlot.slot());
                        InvUtils.move().from(totemInInv.slot()).to(currentSlot);
                    } else {
                        InvUtils.move().from(totemInInv.slot()).to(currentSlot);
                    }
                }
            }
        }
    }

    private void handleIntegrationMode() {
        float health = mc.player.getHealth() + mc.player.getAbsorptionAmount();
        boolean lowHealth = health <= minHealth.get();
        CrystalPlus crystalPlus = vinculateCA.get() ? Modules.get().get(CrystalPlus.class) : null;

        if (lowHealth) {
            if (crystalPlus != null && crystalPlus.isActive()) {
                crystalWasActive = true;
                crystalPlus.toggle();
            }

            if (mc.player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
                FindItemResult totem = InvUtils.find(Items.TOTEM_OF_UNDYING);
                if (totem.found()) InvUtils.move().from(totem.slot()).toOffhand();
            }
        } else {
            if (crystalPlus != null && crystalWasActive && !crystalPlus.isActive()) {
                crystalPlus.toggle();
                crystalWasActive = false;
            }

            if (mc.player.getOffHandStack().getItem() != Items.END_CRYSTAL) {
                FindItemResult crystal = InvUtils.find(Items.END_CRYSTAL);
                if (crystal.found()) InvUtils.move().from(crystal.slot()).toOffhand();
            }
        }
    }

    private int getSelectedSlotReflective() {
        try {
            Field field = mc.player.getInventory().getClass().getDeclaredField("field_7545");
            field.setAccessible(true);
            return field.getInt(mc.player.getInventory());
        } catch (Exception e) {
            try {
                Field field = mc.player.getInventory().getClass().getDeclaredField("selectedSlot");
                field.setAccessible(true);
                return field.getInt(mc.player.getInventory());
            } catch (Exception ex) {
                return 0;
            }
        }
    }

    public enum Mode {
        Normal,
        Integration
    }
}
