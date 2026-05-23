package net.kenji.first_person_compat.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.kenji.first_person_compat.FirstPersonCompat;
import net.kenji.first_person_compat.client.layers.FirstPersonWearableItemLayer;
import net.kenji.first_person_compat.mixins.AccessorHumanoidArmorLayer;
import net.kenji.first_person_compat.mixins.AccessorLivingEntityRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jline.utils.Log;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;
import yesman.epicfight.api.animation.JointTransform;
import yesman.epicfight.api.animation.Pose;
import yesman.epicfight.api.client.forgeevent.PrepareModelEvent;
import yesman.epicfight.api.client.model.Meshes;
import yesman.epicfight.api.client.model.SkinnedMesh;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.math.MathUtils;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.Vec2i;
import yesman.epicfight.client.ClientEngine;
import yesman.epicfight.client.events.engine.RenderEngine;
import yesman.epicfight.client.mesh.HumanoidMesh;
import yesman.epicfight.client.renderer.FirstPersonRenderer;
import yesman.epicfight.client.renderer.patched.layer.EmptyLayer;
import yesman.epicfight.client.renderer.patched.layer.PatchedItemInHandLayer;
import yesman.epicfight.client.renderer.patched.layer.PatchedLayer;
import yesman.epicfight.client.renderer.patched.layer.WearableItemLayer;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.mixin.client.MixinLivingEntityRenderer;
import yesman.epicfight.model.armature.HumanoidArmature;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class FirstPersonBodyRenderer extends FirstPersonRenderer {


    public FirstPersonBodyRenderer(EntityRendererProvider.Context context, EntityType<?> entityType) {
        super(context, entityType);
        this.addPatchedLayerAlways(HumanoidArmorLayer.class, new FirstPersonWearableItemLayer<>(Meshes.BIPED, context.getModelManager()));

    }

    @Override
    public void render(LocalPlayer entity, LocalPlayerPatch localPlayerPatch,
                       LivingEntityRenderer<LocalPlayer, PlayerModel<LocalPlayer>> renderer,
                       MultiBufferSource buffer, PoseStack poseStack, int packedLight, float partialTicks) {

        if (!Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
            super.render(entity, localPlayerPatch, renderer, buffer, poseStack, packedLight, partialTicks);
            return;
        }
        //super.render(entity, localPlayerPatch, renderer, buffer, poseStack, packedLight, partialTicks);
        Minecraft mc = Minecraft.getInstance();
        MixinLivingEntityRenderer livingEntityRendererAccessor = (MixinLivingEntityRenderer)renderer;
        boolean isVisible = livingEntityRendererAccessor.invokeIsBodyVisible(entity);
        boolean isVisibleToPlayer = !isVisible && !entity.isInvisibleTo(mc.player);
        boolean isGlowing = mc.shouldEntityAppearGlowing(entity);
        RenderType renderType = livingEntityRendererAccessor.invokeGetRenderType(entity, isVisible, isVisibleToPlayer, isGlowing);
        Armature armature = localPlayerPatch.getArmature();
        poseStack.pushPose();
        poseStack.setIdentity();

        if(!(armature instanceof HumanoidArmature humanoidArmature)) return;


        //HERE-------------------------------------------
        float yBodyRot = Mth.rotLerp(partialTicks, entity.yBodyRotO, entity.yBodyRot);
        float yHeadRot = Mth.rotLerp(partialTicks, entity.yHeadRotO, entity.getYRot());
        float xHeadRot = Mth.rotLerp(partialTicks, entity.xRotO, entity.getXRot());

        float yawForMatrix = yHeadRot - yBodyRot;

        poseStack.mulPose(Axis.XP.rotationDegrees(xHeadRot));
        poseStack.mulPose(Axis.YP.rotationDegrees(yawForMatrix));
        //TO-HERE-------------------------------------------

        float standingEyeHeight = entity.getStandingEyeHeight(
                net.minecraft.world.entity.Pose.STANDING,
                entity.getDimensions(net.minecraft.world.entity.Pose.STANDING));

        float correction = 0.25F;

        if (entity.isVisuallySwimming()) correction = 0.25F;
        else if (entity.isFallFlying()) correction = 100.0F;

        //poseStack.translate(0.1F, -standingEyeHeight - 0.05F, correction);

        this.prepareVanillaModel(entity, renderer.getModel(), renderer, partialTicks);
        this.setArmaturePose(localPlayerPatch, armature, partialTicks);

        poseStack.translate(-0.1F, 0, correction);

        OpenMatrix4f chestPoseMatrix = humanoidArmature.getPoseMatrices()[humanoidArmature.chest.getId()];
        poseStack.translate(-chestPoseMatrix.m30, -chestPoseMatrix.m31 * 1.55, -chestPoseMatrix.m32 * 2.25);

        if (renderType != null) {
            HumanoidMesh mesh = (HumanoidMesh) this.getMeshProvider(localPlayerPatch).get();
            this.prepareModel(mesh, entity, localPlayerPatch, renderer);
            PrepareModelEvent prepareModelEvent = new PrepareModelEvent(this, mesh, localPlayerPatch, buffer, poseStack, packedLight, partialTicks);
            if (!MinecraftForge.EVENT_BUS.post(prepareModelEvent)) {
                Vector4f color = new Vector4f(1.0F, 1.0F, 1.0F, isVisibleToPlayer ? 0.15F : 1.0F);
                localPlayerPatch.getEntityDecorations().modifyColor(color, partialTicks);
                int blockLight = (packedLight & 240) >> 4;
                int skyLight = (packedLight & 15728640) >> 20;
                Vec2i lightUv = new Vec2i(blockLight, skyLight);
                localPlayerPatch.getEntityDecorations().modifyLight(lightUv, partialTicks);
                int modifiedLight = LightTexture.pack(lightUv.x, lightUv.y);
                mesh.draw(poseStack, buffer, renderType, modifiedLight, color.x(), color.y(), color.z(), color.w(),
                        this.getOverlayCoord(entity, localPlayerPatch, partialTicks), armature, armature.getPoseMatrices());
                localPlayerPatch.getEntityDecorations().listDecorationOverlays().forEach((decorationOverlay) -> {
                    if (!decorationOverlay.shouldRemove() && decorationOverlay.shouldRender()) {
                        Vector4f overlayColor = decorationOverlay.color(partialTicks);
                        mesh.draw(poseStack, buffer, decorationOverlay.getRenderType(), modifiedLight,
                                overlayColor.x(), overlayColor.y(), overlayColor.z(), overlayColor.w(),
                                OverlayTexture.NO_OVERLAY, armature, armature.getPoseMatrices());
                    }
                });
            }
        }

        if (!entity.isSpectator()) {

            this.renderLayer(renderer, localPlayerPatch, entity, armature.getPoseMatrices(), buffer, poseStack, packedLight, partialTicks);
        }

        if (renderType != null && Minecraft.getInstance().getEntityRenderDispatcher().shouldRenderHitBoxes()) {
            localPlayerPatch.getClientAnimator().renderDebuggingInfoForAllLayers(poseStack, buffer, partialTicks);
        }

        poseStack.popPose();
    }

    @Override
    protected void renderLayer(LivingEntityRenderer<LocalPlayer, PlayerModel<LocalPlayer>> renderer, LocalPlayerPatch entitypatch, LocalPlayer entity, OpenMatrix4f[] poses, MultiBufferSource buffer, PoseStack poseStack, int packedLight, float partialTicks) {
        List<RenderLayer<LocalPlayer, PlayerModel<LocalPlayer>>> layers = ((AccessorLivingEntityRenderer) renderer).getLayers();
        float f = MathUtils.lerpBetween(entity.yBodyRotO, entity.yBodyRot, partialTicks);
        float f1 = MathUtils.lerpBetween(entity.yHeadRotO, entity.yHeadRot, partialTicks);
        float f2 = f1 - f;
        float f7 = entity.getViewXRot(partialTicks);
        float bob = ((MixinLivingEntityRenderer) renderer).invokeGetBob(entity, partialTicks);

        Log.info("LayerCount: " + layers.size());
        for(RenderLayer<LocalPlayer, PlayerModel<LocalPlayer>> layer : layers) {

            // Walk up the class hierarchy to find a match in patchedLayers
            Class<?> rendererClass = layer.getClass();
            if (rendererClass.isAnonymousClass()) {
                rendererClass = rendererClass.getSuperclass();
            }

            // If exact class not found, walk up superclasses
            Class<?> lookupClass = rendererClass;
            while (lookupClass != null && !this.patchedLayers.containsKey(lookupClass)) {
                lookupClass = lookupClass.getSuperclass();
            }
            if (lookupClass != null && this.patchedLayers.containsKey(lookupClass)) {
                ((PatchedLayer) this.patchedLayers.get(lookupClass)).renderLayer(
                        entity, entitypatch, layer, poseStack, buffer, packedLight, poses, bob, f2, f7, partialTicks);
            }
        }
    }

    @Override
    protected void prepareModel(HumanoidMesh mesh, LocalPlayer entity, LocalPlayerPatch entitypatch, LivingEntityRenderer<LocalPlayer, PlayerModel<LocalPlayer>> renderer) {
        super.prepareModel(mesh, entity, entitypatch, renderer);
    }

    @Mod.EventBusSubscriber(modid = FirstPersonCompat.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ClientEvents{
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event){
            if(event.phase != TickEvent.Phase.END) return;

            LocalPlayer localPlayer = Minecraft.getInstance().player;
            if(localPlayer == null) return;
            LocalPlayerPatch localPlayerPatch = EpicFightCapabilities.getLocalPlayerPatch(localPlayer);
            RenderEngine renderEngine = ClientEngine.getInstance().renderEngine;
            FirstPersonRenderer renderer = renderEngine.getFirstPersonRenderer();
           // Log.info("Logging Torso Hidden: " + renderer.getMeshProvider(localPlayerPatch).get().torso.isHidden());
        }
        @SubscribeEvent
        public static void onKeyInput(InputEvent.Key event){

        }
    }
}