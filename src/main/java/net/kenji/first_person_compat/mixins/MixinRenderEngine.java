package net.kenji.first_person_compat.mixins;

import net.kenji.first_person_compat.client.render.FirstPersonBodyRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import org.jline.utils.Log;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.client.events.engine.RenderEngine;
import yesman.epicfight.client.renderer.FirstPersonRenderer;
import yesman.epicfight.client.renderer.patched.item.RenderItemBase;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.config.ClientConfig;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;

@Mixin(value = RenderEngine.class, remap = false)
public abstract class MixinRenderEngine {
    @Shadow
    private FirstPersonRenderer firstPersonRenderer;
    @Shadow
    public abstract RenderItemBase getItemRenderer(ItemStack itemstack);

    @Inject(method = "epicfight$addLayers", at = @At("TAIL"))
    private void replaceFirstPersonRenderer(EntityRenderersEvent.AddLayers event, CallbackInfo ci) {
        this.firstPersonRenderer = new FirstPersonBodyRenderer(event.getContext(), EntityType.PLAYER);
    }

    @Inject(method = "epicfight$renderHand", at = @At("HEAD"), cancellable = true)
    private void interceptFirstPersonRenderer(RenderHandEvent event, CallbackInfo ci) {
        ci.cancel();
        LocalPlayerPatch playerpatch = EpicFightCapabilities.getCachedLocalPlayerPatch();
        if (playerpatch != null && ClientConfig.enableAnimatedFirstPersonModel) {
            RenderItemBase mainhandItemSkin = this.getItemRenderer(((LocalPlayer)playerpatch.getOriginal()).getMainHandItem());
            RenderItemBase offhandItemSkin = this.getItemRenderer(((LocalPlayer)playerpatch.getOriginal()).getOffhandItem());
            boolean useEpicFightModel = (mainhandItemSkin == null || !mainhandItemSkin.forceVanillaFirstPerson()) && (offhandItemSkin == null || !offhandItemSkin.forceVanillaFirstPerson());
            if (useEpicFightModel) {
                if (event.getHand() == InteractionHand.MAIN_HAND) {
                    this.firstPersonRenderer.render((LocalPlayer)playerpatch.getOriginal(), playerpatch, (LivingEntityRenderer) Minecraft.getInstance().getEntityRenderDispatcher().getRenderer((LocalPlayer)playerpatch.getOriginal()), event.getMultiBufferSource(), event.getPoseStack(), event.getPackedLight(), event.getPartialTick());
                }
                event.setCanceled(true);
            }
        }
    }
}