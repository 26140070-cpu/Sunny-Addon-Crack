package com.sunny.addon.modules;

import com.sunny.addon.SunnyAddon;
import com.sunny.addon.mixin.Jumping;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import java.util.*;
import java.util.stream.Collectors;

public class AutoFeetTrap extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");
    private final SettingGroup sgDisable = settings.createGroup("Disable");

    private final Setting<List<net.minecraft.block.Block>> blocks = sgGeneral.add(new BlockListSetting.Builder()
        .name("block-list")
        .description("Blocks used for feet trap.")
        .defaultValue(Collections.singletonList(Blocks.OBSIDIAN))
        .build()
    );

    private final Setting<Integer> bpt = sgGeneral.add(new IntSetting.Builder()
        .name("Blocks Per Tick")
        .defaultValue(4)
        .min(1)
        .max(6)
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("Rotate")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> airPlace = sgGeneral.add(new BoolSetting.Builder()
        .name("Air Place")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
        .name("Render")
        .defaultValue(true)
        .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("Side Color")
        .defaultValue(new SettingColor(244, 227, 227, 50))
        .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("Line Color")
        .defaultValue(new SettingColor(255, 209, 209, 255))
        .build()
    );

    private final Setting<Boolean> disableOnJump = sgDisable.add(new BoolSetting.Builder()
        .name("Disable On Jump")
        .defaultValue(true)
        .build()
    );

    public AutoFeetTrap() {
        super(SunnyAddon.CATEGORY, "Feet Trap", "Automatically traps your feet");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        if (disableOnJump.get() && ((Jumping) mc.player).isJumping()) {
            toggle();
            return;
        }

        FindItemResult blockResult = InvUtils.findInHotbar(stack ->
            stack.getItem() instanceof BlockItem bi && blocks.get().contains(bi.getBlock())
        );

        if (!blockResult.found()) return;

        List<BlockPos> targets = getFeetTrapTargets();
        int placed = 0;
        Set<BlockPos> tried = new HashSet<>();

        while (placed < bpt.get()) {
            boolean progressed = false;

            for (BlockPos pos : targets) {
                if (placed >= bpt.get()) break;
                if (!tried.add(pos) || !mc.world.getBlockState(pos).isReplaceable()) continue;

                if (!airPlace.get() && !hasSupport(pos)) {
                    boolean isCorner = (targets.contains(pos.north()) && targets.contains(pos.west()))
                        || (targets.contains(pos.north()) && targets.contains(pos.east()))
                        || (targets.contains(pos.south()) && targets.contains(pos.west()))
                        || (targets.contains(pos.south()) && targets.contains(pos.east()));
                    if (!isCorner) continue;
                }

                if (BlockUtils.place(pos, blockResult, rotate.get(), 50)) {
                    placed++;
                    progressed = true;
                }
            }
            if (!progressed) break;
        }
    }

    private List<BlockPos> getFeetTrapTargets() {
        if (mc.player == null) return Collections.emptyList();

        Box box = mc.player.getBoundingBox();
        int y = (int) Math.floor(box.minY);
        int minX = (int) Math.floor(box.minX + 0.001);
        int maxX = (int) Math.floor(box.maxX - 0.001);
        int minZ = (int) Math.floor(box.minZ + 0.001);
        int maxZ = (int) Math.floor(box.maxZ - 0.001);

        Set<BlockPos> occupied = new LinkedHashSet<>();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                occupied.add(new BlockPos(x, y, z));
            }
        }

        Set<BlockPos> cardinals = new LinkedHashSet<>();
        Set<BlockPos> corners = new LinkedHashSet<>();

        for (BlockPos base : occupied) {
            cardinals.add(base.north());
            cardinals.add(base.south());
            cardinals.add(base.east());
            cardinals.add(base.west());

            corners.add(base.north().east());
            corners.add(base.north().west());
            corners.add(base.south().east());
            corners.add(base.south().west());
        }

        cardinals.removeAll(occupied);
        corners.removeAll(occupied);

        double px = mc.player.getX();
        double pz = mc.player.getZ();

        Comparator<BlockPos> distanceSorter = (a, b) -> {
            double da = Math.pow(a.getX() + 0.5 - px, 2) + Math.pow(a.getZ() + 0.5 - pz, 2);
            double db = Math.pow(b.getX() + 0.5 - px, 2) + Math.pow(b.getZ() + 0.5 - pz, 2);
            return Double.compare(da, db);
        };

        List<BlockPos> floorTargets = disableOnJump.get() ? Collections.emptyList() :
            occupied.stream().sorted(distanceSorter).collect(Collectors.toList());

        List<BlockPos> sortedCardinals = cardinals.stream().sorted(distanceSorter).collect(Collectors.toList());
        List<BlockPos> sortedCorners = corners.stream().sorted(distanceSorter).collect(Collectors.toList());

        List<BlockPos> combined = new ArrayList<>(floorTargets);
        combined.addAll(sortedCardinals);
        combined.addAll(sortedCorners);
        return combined;
    }

    private boolean hasSupport(BlockPos pos) {
        return isSolidSupport(pos.down()) || isSolidSupport(pos.north()) ||
            isSolidSupport(pos.south()) || isSolidSupport(pos.east()) ||
            isSolidSupport(pos.west()) || isSolidSupport(pos.up());
    }

    private boolean isSolidSupport(BlockPos pos) {
        if (mc.world == null) return false;
        BlockState state = mc.world.getBlockState(pos);
        return !state.isAir() && state.isReplaceable();
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (!render.get()) return;

        FindItemResult blockResult = InvUtils.findInHotbar(stack ->
            stack.getItem() instanceof BlockItem bi && blocks.get().contains(bi.getBlock())
        );

        if (blockResult.found()) {
            for (BlockPos pos : getFeetTrapTargets()) {
                event.renderer.box(pos, sideColor.get(), lineColor.get(), ShapeMode.Both, 0);
            }
        }
    }
}
