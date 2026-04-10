package com.sunny.addon.next;

import com.sunny.addon.SunnyAddon;
import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class AutoMinePlusNext extends Module {
    private final SettingGroup sg = settings.getDefaultGroup();
    private final SettingGroup sgBreaks = settings.createGroup("Breaks");
    private final SettingGroup sgPauses = settings.createGroup("Pauses");

    private final Setting<Double> range = sg.add(new DoubleSetting.Builder().name("range").defaultValue(5.0).min(1.0).sliderMax(10.0).build());
    private final Setting<Boolean> instarebreak = sg.add(new BoolSetting.Builder().name("insta-rebreak").defaultValue(true).build());
    private final Setting<Integer> delay = sg.add(new IntSetting.Builder().name("delay-between-mines").defaultValue(0).build());
    private final Setting<Boolean> silentSwitch = sg.add(new BoolSetting.Builder().name("silent-switch").defaultValue(true).build());
    private final Setting<Boolean> render = sg.add(new BoolSetting.Builder().name("render").defaultValue(true).build());
    private final Setting<SettingColor> sideColor = sg.add(new ColorSetting.Builder().name("side-color").defaultValue(new SettingColor(255, 0, 0, 50)).build());
    private final Setting<SettingColor> lineColor = sg.add(new ColorSetting.Builder().name("line-color").defaultValue(new SettingColor(255, 0, 0, 50)).build());

    private final Setting<Boolean> antiselfcrawl = sgBreaks.add(new BoolSetting.Builder().name("Anti Self Crawl").defaultValue(true).build());
    private final Setting<Boolean> crystalbomber = sgBreaks.add(new BoolSetting.Builder().name("Crystal Bomber").description("Coloca un cristal sobre la cabeza del enemigo y lo explota al romper el bloque.").visible(() -> !instarebreak.get()).defaultValue(false).build());
    private final Setting<Boolean> pauseOnEat = sgPauses.add(new BoolSetting.Builder().name("pause-on-eat").defaultValue(true).build());

    private final BlockPos.Mutable targetPos = new BlockPos.Mutable(0, Integer.MIN_VALUE, 0);
    private Direction mineDir = Direction.UP;
    private int ticks;
    private BlockPos lastTarget, lastEnemyPos, lockedTarget, lockedEnemyPos, manualTarget, crystalPos;
    private boolean manualOverride, lockedIsBurrow, startedBreaking, crystalPlaced;
    private double breakProgress;
    private int currentSlot;

    public static boolean crystalBomberMining = false;

    public AutoMinePlusNext() {
        super(SunnyAddon.CATEGORY, "AutoMinePlus", "Rompe automáticamente el Surround de los enemigos.");
    }

    @Override
    public void onActivate() {
        ticks = 0;
        lastTarget = null;
        targetPos.set(0, Integer.MIN_VALUE, 0);
    }

    @EventHandler
    private void onStartBreaking(StartBreakingBlockEvent event) {
        if (event.blockPos == null) return;
        if (pauseOnEat.get() && mc.player.isUsingItem()) return;

        manualOverride = true;
        manualTarget = event.blockPos;
        mineDir = event.direction;
        targetPos.set(event.blockPos);

        BlockState state = mc.world.getBlockState(event.blockPos);
        int slot = InvUtils.findFastestTool(state).slot();

        mc.interactionManager.cancelBlockBreaking();

        if (silentSwitch.get() && slot != -1) InvUtils.swap(slot, true);

        mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, event.blockPos, mineDir));

        if (silentSwitch.get()) InvUtils.swapBack();
    }

    private void placeCrystal(BlockPos blockPos) {
        int crystalSlot = InvUtils.findInHotbar(Items.END_CRYSTAL).slot();
        if (crystalSlot == -1) return;

        BlockPos placePos = blockPos.up();
        if (!mc.world.getBlockState(placePos).isAir()) return;

        InvUtils.swap(crystalSlot, true);
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.ofCenter(blockPos), Direction.UP, blockPos, false));
        InvUtils.swapBack();

        crystalPlaced = true;
        crystalPos = placePos;
    }

    private void breakCrystal() {
        if (!crystalPlaced || crystalPos == null) return;

        mc.world.getEntities().forEach(entity -> {
            if (entity instanceof EndCrystalEntity crystal && crystal.getBlockPos().equals(crystalPos)) {
                mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(crystal, mc.player.isSneaking()));
            }
        });

        crystalPlaced = false;
        crystalPos = null;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        autoSelect();

        if (targetPos.getY() == Integer.MIN_VALUE) return;
        if (!BlockUtils.canBreak(targetPos)) return;

        BlockState state = mc.world.getBlockState(targetPos);

        if (state.isAir()) {
            crystalBomberMining = false;
            breakProgress = 0;
            startedBreaking = false;
            return;
        }

        if (!instarebreak.get()) {
            int slot = InvUtils.findFastestTool(state).slot();
            if (slot == -1) return;

            if (!startedBreaking) {
                if (crystalbomber.get()) crystalBomberMining = true;
                currentSlot = slot;

                if (silentSwitch.get()) InvUtils.swap(currentSlot, true);
                mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, targetPos, mineDir));
                if (silentSwitch.get()) InvUtils.swapBack();

                startedBreaking = true;
                breakProgress = 0;
                return;
            }

            breakProgress += BlockUtils.getBreakDelta(currentSlot, state);

            double placeThreshold = 1.0 - (BlockUtils.getBreakDelta(currentSlot, state) * 3.0);
            if (crystalbomber.get() && !crystalPlaced && breakProgress >= placeThreshold) {
                placeCrystal(targetPos);
            }

            if (breakProgress >= 1.0) {
                crystalBomberMining = false;
                if (silentSwitch.get()) InvUtils.swap(currentSlot, true);
                mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, targetPos, mineDir));
                if (silentSwitch.get()) InvUtils.swapBack();

                if (crystalbomber.get()) breakCrystal();

                breakProgress = 0;
                startedBreaking = false;
            }
        }
        else {
            if (ticks >= delay.get()) {
                ticks = 0;
                int slot = InvUtils.findFastestTool(state).slot();
                if (silentSwitch.get() && slot != -1) InvUtils.swap(slot, true);
                mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, targetPos, mineDir));
                if (silentSwitch.get()) InvUtils.swapBack();
            } else {
                ticks++;
            }
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (render.get() && targetPos.getY() != Integer.MIN_VALUE) {
            event.renderer.box(targetPos, sideColor.get(), lineColor.get(), ShapeMode.Both, 0);
        }
    }

    private void autoSelect() {
        if (antiselfcrawl.get() && (mc.player.isInSneakingPose() || mc.player.isSwimming())) {
            BlockPos headPos = mc.player.getBlockPos().up();
            if (!mc.world.getBlockState(headPos).isAir() && BlockUtils.canBreak(headPos)) {
                if (!headPos.equals(targetPos)) {
                    targetPos.set(headPos);
                    mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, headPos, Direction.UP));
                }
                return;
            }
        }

        if (manualOverride && manualTarget != null) {
            if (!mc.world.getBlockState(manualTarget).isAir() && BlockUtils.canBreak(manualTarget)) {
                targetPos.set(manualTarget);
                return;
            }
            manualOverride = false;
            manualTarget = null;
        }

        PlayerEntity target = null;
        double bestDist = range.get() * range.get();

        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player || !p.isAlive()) continue;
            double dist = mc.player.squaredDistanceTo(p);
            if (dist < bestDist) {
                bestDist = dist;
                target = p;
            }
        }

        if (target == null) return;

        BlockPos enemyPos = target.getBlockPos();

        if (BlockUtils.canBreak(enemyPos) && !mc.world.getBlockState(enemyPos).isAir()) {
            targetPos.set(enemyPos);
            return;
        }

        BlockPos[] surround = {enemyPos.north(), enemyPos.south(), enemyPos.east(), enemyPos.west()};
        for (BlockPos pos : surround) {
            if (BlockUtils.canBreak(pos) && !mc.world.getBlockState(pos).isAir()) {
                targetPos.set(pos);
                return;
            }
        }
    }
}
