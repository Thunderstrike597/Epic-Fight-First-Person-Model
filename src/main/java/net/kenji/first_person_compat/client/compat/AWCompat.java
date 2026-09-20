package net.kenji.first_person_compat.client.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import moe.plushie.armourers_workshop.compatibility.forge.AbstractForgeEpicFightHandler;
import moe.plushie.armourers_workshop.core.client.layer.SkinWardrobeLayer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.client.renderer.FirstPersonRenderer;
import yesman.epicfight.client.renderer.patched.layer.EmptyLayer;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

import java.util.Arrays;

/** Everything AW-specific lives here. Never call into this class unless FirstPersonBodyRenderer.AW_LOADED is true. */
public final class AWCompat {

    // Same passthrough AW's own ForgeEpicFightRendererMixin registers on PatchedLivingEntityRenderer —
    // SkinWardrobeLayer already resolves its own pose via the render-plugin below, so it just needs
    // to be invoked with the vanilla signature.
    public static void registerWardrobeLayer(FirstPersonRenderer renderer) {
        renderer.addPatchedLayerAlways(SkinWardrobeLayer.class, new EmptyLayer<>() {
            @Override
            public void renderLayer(LocalPlayer entityliving, LocalPlayerPatch entitypatch, RenderLayer<LocalPlayer, PlayerModel<LocalPlayer>> vanillaLayer, PoseStack poseStack, MultiBufferSource buffer, int packedLight, OpenMatrix4f[] poses, float bob, float yRot, float xRot, float partialTicks) {
                vanillaLayer.render(poseStack, buffer, packedLight, entityliving, partialTicks, 0, partialTicks, packedLight, xRot, yRot);
            }
        });
    }

    public static void onRenderPre(LivingEntity entity, int packedLight, float partialTick,
                                   PoseStack poseStack, MultiBufferSource buffer, LivingEntityRenderer<?, ?> renderer) {
        AbstractForgeEpicFightHandler.onRenderPre(entity, packedLight, partialTick, false, poseStack, buffer, renderer);
    }

    // Mirrors aw2$renderEntity — call this right before your own mesh.draw(), and feed the returned
    // array into mesh.draw() instead of armature.getPoseMatrices() directly. This is what lets AW
    // align itself to Epic Fight's current animated pose, and also lets it zero out joints its skin covers.
    public static OpenMatrix4f[] onRenderEntity(LivingEntity entity, Armature armature, int packedLight, float partialTick,
                                                PoseStack poseStack, MultiBufferSource buffer, OpenMatrix4f[] poses) {
        // Feed AW a copy with the head joint collapsed to zero scale, so its own wardrobe bake
        // (masks/hats/helmets skinned to the "Head" joint) never gets a real transform for it —
        // the geometry weighted to that joint disappears, without touching Chest/Torso like
        // setFirstPerson(true) does.
        OpenMatrix4f[] wardrobeInput = poses;
        var headJoint = armature.searchJointByName("Head");
        if (headJoint != null) {
            wardrobeInput = Arrays.copyOf(poses, poses.length);
            wardrobeInput[headJoint.getId()] = OpenMatrix4f.createScale(0F, 0F, 0F);
        }

        var cir = new CallbackInfoReturnable<OpenMatrix4f[]>("poses", true, wardrobeInput);
        AbstractForgeEpicFightHandler.onRenderEntity(entity, armature, packedLight, partialTick, poseStack, buffer, cir);
        return cir.getReturnValue();
    }

    // Mirrors aw2$renderPost — RETURN of the render pass. Clears the transform provider so it
    // doesn't leak into some other renderer's frame.
    public static void onRenderPost(LivingEntity entity, int packedLight, float partialTick,
                                     PoseStack poseStack, MultiBufferSource buffer, LivingEntityRenderer<?, ?> renderer) {
        AbstractForgeEpicFightHandler.onRenderPost(entity, packedLight, partialTick, poseStack, buffer, renderer);
    }
}