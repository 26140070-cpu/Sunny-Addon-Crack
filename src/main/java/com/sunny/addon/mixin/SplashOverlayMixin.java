package com.sunny.addon.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SplashOverlay.class)
public class SplashOverlayMixin {

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V"), index = 4)
    private int changeLoadingBackgroundColor(int color) {
        return -10496;
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void drawPlatiniumText(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();
        String text = "Sunny Addon BETA 4.0.1";

        context.drawCenteredTextWithShadow(
            MinecraftClient.getInstance().textRenderer,
            text,
            width / 2,
            height - 30,
            -16777216
        );
    }
}
