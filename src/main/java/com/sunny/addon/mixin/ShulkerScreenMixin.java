package com.sunny.addon.mixin;

import com.sunny.addon.modules.ShulkerStealer;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public abstract class ShulkerScreenMixin<T extends ScreenHandler> extends Screen {
    @Shadow protected int x;
    @Shadow protected int y;
    @Shadow protected int backgroundWidth;
    @Shadow @Final protected T handler;

    @Unique
    private ButtonWidget sunny$rekitButton;

    protected ShulkerScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void sunny$init(CallbackInfo ci) {
        if (this.handler instanceof ShulkerBoxScreenHandler) {
            ShulkerStealer module = (ShulkerStealer) Modules.get().get(ShulkerStealer.class);

            if (module != null && module.isActive()) {
                int buttonX = this.x + this.backgroundWidth + 8;
                int buttonY = this.y + 8;

                this.sunny$rekitButton = ButtonWidget.builder(Text.literal("Rekit"), button -> {
                        module.requestRekitFromGUI();
                    })
                    .dimensions(buttonX, buttonY, 60, 20)
                    .build();

                this.addDrawableChild(this.sunny$rekitButton);
            }
        }
    }
}
