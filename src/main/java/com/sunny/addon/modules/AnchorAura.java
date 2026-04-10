package com.sunny.addon.modules;

import com.sunny.addon.SunnyAddon;
import com.sunny.addon.modules.CrystalPlus;
import java.util.List;
import java.lang.reflect.Field;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Box;

public class AnchorAura extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");
    private final SettingGroup sgPauses = settings.createGroup("Pauses");
    private final SettingGroup sgPlacements = settings.createGroup("Placements");

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder().name("place-range").defaultValue(5.0).min(1.0).sliderMax(7.0).build());
    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder().name("delay").defaultValue(0).min(0).sliderMax(20).build());
    private final Setting<Priority> modulepriority = sgGeneral.add(new EnumSetting.Builder<Priority>().name("module-priority").defaultValue(Priority.Crystal).build());
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder().name("rotate").defaultValue(true).build());
    private final Setting<Boolean> movetohotbar = sgGeneral.add(new BoolSetting.Builder().name("move-to-hotbar").defaultValue(false).build());
    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder().name("render").defaultValue(true).build());
    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder().name("side-color").defaultValue(new SettingColor(255, 0, 0, 75)).build());
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder().name("line-color").defaultValue(new SettingColor(255, 0, 0, 255)).build());
    private final Setting<Boolean> antisuicide = sgGeneral.add(new BoolSetting.Builder().name("anti-suicide").defaultValue(false).build());
    private final Setting<Integer> vidamin = sgGeneral.add(new IntSetting.Builder().name("minimum-health").min(1).sliderMax(20).defaultValue(10).visible(antisuicide::get).build());

    private int timer;
    private BlockPos renderPos;
    private BlockPos lockedPos;

    private static Field SELECTED_SLOT_FIELD;

    public AnchorAura() {
        super(SunnyAddon.CATEGORY, "AnchorAura", "Anchor Aura optimizado para combate automático.");
    }

    @Override
    public void onActivate() {
        this.timer = 0;
        this.renderPos = null;
        this.lockedPos = null;

        if (SELECTED_SLOT_FIELD == null) {
            setupReflection();
        }
    }

    private void setupReflection() {
        try {
            String[] names = {"field_7545", "selectedSlot"};
            for (String name : names) {
                try {
                    SELECTED_SLOT_FIELD = PlayerInventory.class.getDeclaredField(name);
                    SELECTED_SLOT_FIELD.setAccessible(true);
                    break;
                } catch (NoSuchFieldException ignored) {}
            }
        } catch (Exception e) {
            error("Fallo crítico al acceder al inventario por reflexión.");
        }
    }

    private int getCurrentSlot() {
        try {
            if (SELECTED_SLOT_FIELD != null) {
                return SELECTED_SLOT_FIELD.getInt(mc.player.getInventory());
            }
        } catch (Exception ignored) {}
        return 0;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        if (antisuicide.get() && (mc.player.getHealth() + mc.player.getAbsorptionAmount() <= vidamin.get())) return;

        PlayerEntity target = TargetUtils.getPlayerTarget(range.get(), SortPriority.LowestDistance);
        if (target == null) return;

        if (timer > 0) {
            timer--;
            return;
        }

        BlockPos bestPos = findBestPos(target);
        renderPos = bestPos;

        if (bestPos != null) {
            BlockState state = mc.world.getBlockState(bestPos);
            if (state.getBlock() != Blocks.RESPAWN_ANCHOR) {
                placeAnchor(bestPos);
            } else {
                int charges = state.get(RespawnAnchorBlock.CHARGES);
                if (charges == 0) chargeAnchor(bestPos);
                else explodeAnchor(bestPos);
            }
        }
        timer = delay.get();
    }

    private void placeAnchor(BlockPos pos) {
        FindItemResult anchor = InvUtils.findInHotbar(Items.RESPAWN_ANCHOR);
        if (!anchor.found() && movetohotbar.get()) {
            anchor = InvUtils.find(Items.RESPAWN_ANCHOR);
            if (anchor.found() && !anchor.isHotbar()) {
                int empty = getEmptySlot();
                int targetSlot = (empty != -1) ? empty : getCurrentSlot();
                InvUtils.move().from(anchor.slot()).toHotbar(targetSlot);
            }
        }
        if (anchor.found()) BlockUtils.place(pos, anchor, rotate.get(), 50);
    }

    private void chargeAnchor(BlockPos pos) {
        FindItemResult glow = InvUtils.findInHotbar(Items.GLOWSTONE);
        if (!glow.found() && movetohotbar.get()) {
            glow = InvUtils.find(Items.GLOWSTONE);
            if (glow.found() && !glow.isHotbar()) {
                int empty = getEmptySlot();
                int targetSlot = (empty != -1) ? empty : getCurrentSlot();
                InvUtils.move().from(glow.slot()).toHotbar(targetSlot);
            }
        }
        if (glow.found()) {
            InvUtils.swap(glow.slot(), true);
            interact(pos);
            InvUtils.swapBack();
        }
    }

    private void explodeAnchor(BlockPos pos) {
        int safeSlot = getSafeSlot();
        if (safeSlot != -1) InvUtils.swap(safeSlot, true);
        interact(pos);
        InvUtils.swapBack();
    }

    private void interact(BlockPos pos) {
        Vec3d hitVec = Vec3d.ofCenter(pos);
        BlockHitResult hitResult = new BlockHitResult(hitVec, Direction.UP, pos, false);
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);
    }

    private BlockPos findBestPos(PlayerEntity target) {
        BlockPos targetPos = target.getBlockPos();
        BlockPos[] checkList = {targetPos.up(2), targetPos.north(), targetPos.south(), targetPos.east(), targetPos.west()};
        for (BlockPos pos : checkList) {
            if (isValid(pos)) return pos;
        }
        return null;
    }

    private boolean isValid(BlockPos pos) {
        if (mc.player.getEyePos().distanceTo(Vec3d.ofCenter(pos)) > range.get()) return false;
        BlockState state = mc.world.getBlockState(pos);
        if (state.getBlock() == Blocks.RESPAWN_ANCHOR) return true;
        if (!state.isReplaceable()) return false;
        Box box = new Box(pos);
        return mc.world.getOtherEntities(null, box, e -> !(e instanceof ItemEntity) && !(e instanceof TntEntity)).isEmpty();
    }

    private int getSafeSlot() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isEmpty()) return i;
            if (mc.player.getInventory().getStack(i).getItem() != Items.GLOWSTONE) return i;
        }
        return -1;
    }

    private int getEmptySlot() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isEmpty()) return i;
        }
        return -1;
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (render.get() && renderPos != null) {
            event.renderer.box(renderPos, sideColor.get(), lineColor.get(), ShapeMode.Both, 0);
        }
    }

    public enum Priority { Crystal, Anchor }
}
