package net.kenji.first_person_compat.mixins;

import net.kenji.first_person_compat.client.render.FirstPersonBodyRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.client.events.engine.RenderEngine;
import yesman.epicfight.client.renderer.FirstPersonRenderer;

@Mixin(value = RenderEngine.class, remap = false)
public class MixinRenderEngine {
    @Shadow
    private FirstPersonRenderer firstPersonRenderer;

    @Inject(method = "reloadEntityRenderers", at = @At("TAIL"))
    private void replaceFirstPersonRenderer(EntityRendererProvider.Context context, CallbackInfo ci) {
        // Grab context somehow (store from earlier init) and replace:
        this.firstPersonRenderer = new FirstPersonBodyRenderer(context, EntityType.PLAYER);
    }
}