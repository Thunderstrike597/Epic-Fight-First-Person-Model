package net.kenji.first_person_compat.mixins;

import net.kenji.first_person_compat.client.render.FirstPersonBodyRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
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

    @Inject(method = "epicfight$addLayers", at = @At("TAIL"))
    private void replaceFirstPersonRenderer(EntityRenderersEvent.AddLayers event, CallbackInfo ci) {
        this.firstPersonRenderer = new FirstPersonBodyRenderer(event.getContext(), EntityType.PLAYER);
    }
}