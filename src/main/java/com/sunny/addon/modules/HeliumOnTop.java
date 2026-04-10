package com.sunny.addon.modules;

import com.sunny.addon.SunnyAddon;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;

public class HeliumOnTop extends Module {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    private final SettingGroup sgRender = this.settings.createGroup("Render");

    private final Setting<ClickButton> selectionButton = sgGeneral.add(new EnumSetting.Builder<ClickButton>()
        .name("selection-button").defaultValue(ClickButton.Left).build());

    private final Setting<Double> stopDist = sgGeneral.add(new DoubleSetting.Builder()
        .name("stop-distance").defaultValue(2.5).min(1.0).sliderMax(5.0).build());

    private final Setting<Boolean> autoBridge = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-bridge").defaultValue(true).build());

    private final Setting<Boolean> autoTool = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-tool").defaultValue(true).build());

    private final Setting<SettingColor> areaColor = sgRender.add(new ColorSetting.Builder()
        .name("fill-color").defaultValue(new SettingColor(255, 255, 255, 10)).build());

    private BlockPos pos1, pos2;
    private final List<BlockPos> blocksToMine = new ArrayList<>();
    private long lastClickTime = 0L;

    public HeliumOnTop() {
        super(SunnyAddon.CATEGORY, "HeliumOnTop!", "Minería automática de áreas con navegación inteligente.");
    }

    @Override
    public void onActivate() { resetMemory(); }

    @Override
    public void onDeactivate() {
        stopMovement();
        resetMemory();
    }

    private void resetMemory() {
        pos1 = null; pos2 = null;
        synchronized (blocksToMine) { blocksToMine.clear(); }
    }
    public void onTriggerClick(int button) {
        if (button == selectionButton.get().glfw && System.currentTimeMillis() - lastClickTime >= 300L) {
            if (mc.crosshairTarget instanceof BlockHitResult result) {
                BlockPos hit = result.getBlockPos();
                lastClickTime = System.currentTimeMillis();

                if (pos1 == null || (pos1 != null && pos2 != null)) {
                    pos1 = hit; pos2 = null;
                    synchronized (blocksToMine) { blocksToMine.clear(); }
                    info("Punto A fijado.");
                } else {
                    pos2 = hit;
                    info("Punto B fijado. Iniciando excavación...");
                    calculateArea();
                }
            }
        }
    }

    private void calculateArea() {
        synchronized (blocksToMine) {
            blocksToMine.clear();
            int minX = Math.min(pos1.getX(), pos2.getX());
            int maxX = Math.max(pos1.getX(), pos2.getX());
            int minY = Math.min(pos1.getY(), pos2.getY());
            int maxY = Math.max(pos1.getY(), pos2.getY());
            int minZ = Math.min(pos1.getZ(), pos2.getZ());
            int maxZ = Math.max(pos1.getZ(), pos2.getZ());

            for (int x = minX; x <= maxX; x++) {
                for (int y = maxY; y >= minY; y--) { 
                    for (int z = minZ; z <= maxZ; z++) {
                        blocksToMine.add(new BlockPos(x, y, z));
                    }
                }
            }
        }
    }

    @EventHandler
    private void onTick(Pre event) {
        if (blocksToMine.isEmpty() || mc.player == null) {
            stopMovement();
            if (pos1 != null && pos2 != null) {
                info("Excavación completada.");
                toggle();
            }
            return;
        }

        double pX = mc.player.getX();
        double pY = mc.player.getY();
        double pZ = mc.player.getZ();

        synchronized (blocksToMine) {
            blocksToMine.removeIf(p -> mc.world.getBlockState(p).isAir());
            blocksToMine.sort(Comparator.comparingDouble(p -> p.getSquaredDistance(pX, pY, pZ)));
        }

        if (blocksToMine.isEmpty()) return;

        BlockPos target = blocksToMine.get(0);
        double distSq = target.getSquaredDistance(pX, pY, pZ);

        Rotations.rotate(Rotations.getYaw(target), Rotations.getPitch(target));

        if (distSq > Math.pow(stopDist.get(), 2)) {
            updateMovement(true);

            if (mc.player.isOnGround() && (mc.player.horizontalCollision)) {
                mc.player.jump();
            }

            if (autoBridge.get()) {
                BlockPos underFeet = mc.player.getBlockPos().down();
                if (mc.world.getBlockState(underFeet).isAir() && mc.player.isOnGround()) {
                    updateMovement(false); 
                    placeBlock(underFeet);
                }
            }
        } else {

            stopMovement();
            mineBlock(target);
        }
    }

    private void mineBlock(BlockPos pos) {
        BlockState state = mc.world.getBlockState(pos);
        if (state.isAir()) return;

        if (autoTool.get()) {
            FindItemResult tool = InvUtils.findInHotbar(stack -> stack.getMiningSpeedMultiplier(state) > 1.5f);
            if (tool.found()) InvUtils.swap(tool.slot(), false);
        }

        BlockUtils.breakBlock(pos, true);
    }

    private void placeBlock(BlockPos pos) {
        FindItemResult block = InvUtils.findInHotbar(s -> s.getItem() instanceof BlockItem);
        if (block.found()) {
            InvUtils.swap(block.slot(), false);
            BlockUtils.place(pos, Hand.MAIN_HAND, block.slot(), false, 0, true, true, false);
        }
    }

    private void updateMovement(boolean press) {
        mc.options.forwardKey.setPressed(press);
    }

    private void stopMovement() { updateMovement(false); }

    public enum ClickButton { Left(0), Right(1), Middle(2); public final int glfw; ClickButton(int g) { glfw = g; } }
}
