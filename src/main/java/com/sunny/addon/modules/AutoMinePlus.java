package com.sunny.addon.modules;

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
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class AutoMinePlus extends Module {
    private final SettingGroup sg = settings.getDefaultGroup();
    private final SettingGroup sgBreaks = settings.createGroup("Breaks");
    private final SettingGroup sgPauses = settings.createGroup("Pauses");

    private final Setting<Double> range = sg.add(new DoubleSetting.Builder().name("range").defaultValue(6.0).min(1.0).sliderMax(7.0).build());
    private final Setting<Boolean> instarebreak = sg.add(new BoolSetting.Builder().name("insta-rebreak").defaultValue(true).build());
    private final Setting<Integer> delay = sg.add(new IntSetting.Builder().name("delay-between-mines").defaultValue(0).build());
    private final Setting<Boolean> silentSwitch = sg.add(new BoolSetting.Builder().name("silent-switch").defaultValue(true).build());
    private final Setting<Boolean> render = sg.add(new BoolSetting.Builder().name("render").defaultValue(true).build());
    private final Setting<SettingColor> sideColor = sg.add(new ColorSetting.Builder().name("side-color").defaultValue(new SettingColor(255, 0, 0, 50)).build());
    private final Setting<SettingColor> lineColor = sg.add(new ColorSetting.Builder().name("line-color").defaultValue(new SettingColor(255, 0, 0, 50)).build());
    private final Setting<Boolean> antiselfcrawl = sgBreaks.add(new BoolSetting.Builder().name("Anti Self Crawl").defaultValue(true).build());
    private final Setting<Boolean> pauseOnEat = sgPauses.add(new BoolSetting.Builder().name("pause-on-eat").defaultValue(true).build());

    private final BlockPos.Mutable targetPos = new BlockPos.Mutable(0, -1, 0);
    private Direction mineDir = Direction.UP;
    private int ticks;
    private boolean manualOverride = false;
    private BlockPos manualTarget = null;
    private boolean startedBreaking = false;
    private double breakProgress = 0.0;
    private int currentSlot = -1;

    public static BlockPos currentMiningPos = null;
    public static float miningProgress = 0.0f;

    private Field networkHandlerField;
    private Method sendPacketMethod;

    public AutoMinePlus() {
        super(SunnyAddon.CATEGORY, "AutoMinePlus", "Anti Surround.");
    }

    @Override
    public void onActivate() {
        this.ticks = 0;
        this.targetPos.set(0, -1, 0);
        this.currentMiningPos = null;
        this.breakProgress = 0.0;
        this.startedBreaking = false;

        setupReflection();
    }

    private void setupReflection() {
        try {
            try {
                networkHandlerField = mc.player.getClass().getField("field_3944");
            } catch (NoSuchFieldException e) {
                for (Field field : mc.player.getClass().getFields()) {
                    if (field.getType().getName().contains("ClientPlayNetworkHandler")) {
                        networkHandlerField = field;
                        break;
                    }
                }
            }

            if (networkHandlerField != null) {
                networkHandlerField.setAccessible(true);
                Object handler = networkHandlerField.get(mc.player);

                for (Method method : handler.getClass().getMethods()) {
                    if (method.getParameterCount() == 1 && method.getParameterTypes()[0].equals(Packet.class)) {
                        sendPacketMethod = method;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            error("Error al inicializar la reflexión de red.");
        }
    }

    private void sendPacketSafe(Packet<?> packet) {
        try {
            if (sendPacketMethod != null && networkHandlerField != null) {
                Object handler = networkHandlerField.get(mc.player);
                sendPacketMethod.invoke(handler, packet);
            } else {
                mc.player.networkHandler.sendPacket(packet);
            }
        } catch (Exception ignored) {}
    }

    @EventHandler
    private void onStartBreaking(StartBreakingBlockEvent event) {
        if (event.blockPos == null || (pauseOnEat.get() && mc.player.isUsingItem())) return;

        this.manualOverride = true;
        this.manualTarget = event.blockPos;
        this.mineDir = event.direction;
        this.targetPos.set(event.blockPos);

        BlockState state = mc.world.getBlockState(event.blockPos);
        int slot = InvUtils.findFastestTool(state).slot();

        mc.interactionManager.cancelBlockBreaking();

        if (silentSwitch.get() && slot != -1) InvUtils.swap(slot, true);
        sendPacketSafe(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, event.blockPos, this.mineDir));
        if (silentSwitch.get()) InvUtils.swapBack();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (this.targetPos.getY() != -1) {
            double dX = mc.player.getX() - (targetPos.getX() + 0.5);
            double dY = mc.player.getY() - (targetPos.getY() + 0.5);
            double dZ = mc.player.getZ() - (targetPos.getZ() + 0.5);
            double distSq = dX * dX + dY * dY + dZ * dZ;

            if (distSq > Math.pow(range.get(), 2)) {
                resetTarget();
                return;
            }
        }

        autoSelectTarget();

        if (this.targetPos.getY() == -1) {
            currentMiningPos = null;
            return;
        }

        currentMiningPos = this.targetPos.toImmutable();
        miningProgress = (float) this.breakProgress;

        if (this.instarebreak.get()) {
            handleInstaRebreak();
        } else {
            handleNormalMining();
        }
    }

    private void handleNormalMining() {
        BlockState state = mc.world.getBlockState(targetPos);
        if (state.isAir()) {
            this.breakProgress = 0.0;
            this.startedBreaking = false;
            return;
        }

        int slot = InvUtils.findFastestTool(state).slot();
        if (slot == -1) return;

        if (!this.startedBreaking) {
            this.currentSlot = slot;
            if (silentSwitch.get()) InvUtils.swap(this.currentSlot, true);
            sendPacketSafe(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, targetPos, this.mineDir));
            if (silentSwitch.get()) InvUtils.swapBack();
            this.startedBreaking = true;
        } else {
            this.breakProgress += BlockUtils.getBreakDelta(this.currentSlot, state);
            if (this.breakProgress >= 1.0) {
                if (silentSwitch.get()) InvUtils.swap(this.currentSlot, true);
                sendPacketSafe(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, targetPos, this.mineDir));
                if (silentSwitch.get()) InvUtils.swapBack();
                this.breakProgress = 0.0;
                this.startedBreaking = false;
            }
        }
    }

    private void handleInstaRebreak() {
        if (this.ticks >= delay.get()) {
            this.ticks = 0;
            if (mc.world.getBlockState(targetPos).isAir() || !BlockUtils.canBreak(targetPos)) return;

            int slot = InvUtils.findFastestTool(mc.world.getBlockState(targetPos)).slot();
            if (silentSwitch.get() && slot != -1) InvUtils.swap(slot, true);
            sendPacketSafe(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, targetPos, this.mineDir));
            if (silentSwitch.get()) InvUtils.swapBack();
        } else {
            this.ticks++;
        }
    }

    private void autoSelectTarget() {
        if (antiselfcrawl.get() && (mc.player.isCrawling() || mc.player.isSwimming())) {
            BlockPos headPos = new BlockPos(mc.player.getBlockX(), (int) (mc.player.getY() + 1.5), mc.player.getBlockZ());
            if (!mc.world.getBlockState(headPos).isAir() && BlockUtils.canBreak(headPos)) {
                if (!headPos.equals(targetPos)) updateTarget(headPos, Direction.UP);
                return;
            }
        }

        if (this.manualOverride && this.manualTarget != null) {
            if (!mc.world.getBlockState(manualTarget).isAir()) {
                this.targetPos.set(manualTarget);
                return;
            }
            this.manualOverride = false;
        }

        PlayerEntity enemy = getClosestEnemy();
        if (enemy == null) return;

        BlockPos enemyPos = enemy.getBlockPos();

        if (BlockUtils.canBreak(enemyPos) && !mc.world.getBlockState(enemyPos).isAir()) {
            if (!enemyPos.equals(targetPos)) updateTarget(enemyPos, Direction.UP);
            return;
        }

        BlockPos[] surroundBlocks = { enemyPos.north(), enemyPos.south(), enemyPos.east(), enemyPos.west() };
        for (BlockPos pos : surroundBlocks) {
            if (BlockUtils.canBreak(pos) && !mc.world.getBlockState(pos).isAir()) {
                if (!pos.equals(targetPos)) updateTarget(pos, Direction.UP);
                return;
            }
        }
    }

    private PlayerEntity getClosestEnemy() {
        PlayerEntity closest = null;
        double maxDistSq = Math.pow(range.get(), 2);

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player || !player.isAlive() || player.isCreative()) continue;

            double dX = mc.player.getX() - player.getX();
            double dY = mc.player.getY() - player.getY();
            double dZ = mc.player.getZ() - player.getZ();
            double distSq = dX * dX + dY * dY + dZ * dZ;

            if (distSq < maxDistSq) {
                maxDistSq = distSq;
                closest = player;
            }
        }
        return closest;
    }

    private void updateTarget(BlockPos pos, Direction dir) {
        this.targetPos.set(pos);
        this.mineDir = dir;
        mc.interactionManager.cancelBlockBreaking();
        sendPacketSafe(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos, this.mineDir));
    }

    private void resetTarget() {
        this.targetPos.set(0, -1, 0);
        this.manualOverride = false;
        this.startedBreaking = false;
        this.breakProgress = 0.0;
        currentMiningPos = null;
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (render.get() && this.targetPos.getY() != -1) {
            event.renderer.box(targetPos, sideColor.get(), lineColor.get(), ShapeMode.Both, 0);
        }
    }
}
