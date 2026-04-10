package com.sunny.addon.mixin;

import com.sunny.addon.modules.SunnyCape;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayerEntity.class)
public class AbstractClientPlayerEntityMixin {
    private static final Identifier SUNNY_CAPE = Identifier.of("sunnyaddon", "textures/cape/sunny.png");

    /*
    @Inject(method = "getCapeTexture", at = @At("HEAD"), cancellable = true)
    private void sunny$replaceCapeTexture(CallbackInfoReturnable<Identifier> cir) {
        SunnyCape module = (SunnyCape) Modules.get().get(SunnyCape.class);

        if (module != null && module.isActive()) {
            AbstractClientPlayerEntity player = (AbstractClientPlayerEntity) (Object) this;

            if (!module.onlySelf() || player.isMainPlayer()) {
                cir.setReturnValue(SUNNY_CAPE);
            }
        }
    }
    */
}
