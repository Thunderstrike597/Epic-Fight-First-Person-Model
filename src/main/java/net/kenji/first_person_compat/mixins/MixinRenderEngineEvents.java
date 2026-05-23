package net.kenji.first_person_compat.mixins;

import net.kenji.first_person_compat.client.render.FirstPersonBodyRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.client.event.RenderHandEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.client.ClientEngine;
import yesman.epicfight.client.events.engine.RenderEngine;
import yesman.epicfight.client.renderer.FirstPersonRenderer;
import yesman.epicfight.client.renderer.patched.item.RenderItemBase;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.config.ClientConfig;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;

@Mixin(value = RenderEngine.Events.class, remap = false)
public class MixinRenderEngineEvents {
    @Shadow
    private static RenderEngine renderEngine;

    @Inject(method = "renderHand", at = @At("HEAD"), cancellable = true)
    private static void replaceFirstPersonRenderer(RenderHandEvent event, CallbackInfo ci) {
        ci.cancel();
        LocalPlayer localPlayer = Minecraft.getInstance().player;
        LocalPlayerPatch playerpatch = EpicFightCapabilities.getLocalPlayerPatch(localPlayer);
        if (playerpatch != null) {
            if (ClientConfig.enableAnimatedFirstPersonModel) {
                RenderItemBase mainhandItemSkin = renderEngine.getItemRenderer((playerpatch.getOriginal()).getMainHandItem());
                RenderItemBase offhandItemSkin = renderEngine.getItemRenderer((playerpatch.getOriginal()).getOffhandItem());
                boolean useEpicFightModel = (mainhandItemSkin == null || !mainhandItemSkin.forceVanillaFirstPerson()) && (offhandItemSkin == null || !offhandItemSkin.forceVanillaFirstPerson());
                if (useEpicFightModel) {
                    if (event.getHand() == InteractionHand.MAIN_HAND) {
                        renderEngine.getFirstPersonRenderer().render((LocalPlayer)playerpatch.getOriginal(), playerpatch, (LivingEntityRenderer)renderEngine.minecraft.getEntityRenderDispatcher().getRenderer((LocalPlayer)playerpatch.getOriginal()), event.getMultiBufferSource(), event.getPoseStack(), event.getPackedLight(), event.getPartialTick());
                    }
                    event.setCanceled(true);
                }
            }
        }

    }
}