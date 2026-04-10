package com.sunny.addon.modules;

import com.sunny.addon.SunnyAddon;
import com.sunny.addon.mixin.PlayerMoveC2SPacketAccessor;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Send;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.NoFall;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public class AntiSelfPoP extends Module {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();

    public AntiSelfPoP() {
        super(SunnyAddon.CATEGORY, "AntiSelfPoP", "NoFall But better");
    }

    @Override
    public void onActivate() {
        NoFall NoNoFall = (NoFall) Modules.get().get(NoFall.class);
        if (NoNoFall != null && NoNoFall.isActive()) {
            NoNoFall.toggle();
        }
    }

    @EventHandler
    private void onSendPacket(Send event) {
        if (event.packet instanceof PlayerMoveC2SPacket movePacket && movePacket.isOnGround()) {
            try {
                ((PlayerMoveC2SPacketAccessor) movePacket).setOnGround(false);
            } catch (Exception var4) {
                System.err.println("[AntiSelfPoP] Error Desconocido: " + var4.getMessage());
            }
        }
    }
}
