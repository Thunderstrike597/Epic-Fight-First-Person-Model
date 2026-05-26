package net.kenji.first_person_compat.client.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.kenji.first_person_compat.mixins.AccessorHumanoidArmorLayer;
import net.kenji.first_person_compat.mixins.AccessorWearableItemLayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.jline.utils.Log;
import org.joml.Vector4f;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.client.model.SkinnedMesh;
import yesman.epicfight.api.utils.ColorUtil;
import yesman.epicfight.api.utils.ParseUtil;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.client.mesh.HumanoidMesh;
import yesman.epicfight.client.renderer.patched.layer.WearableItemLayer;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

public class FirstPersonWearableItemLayer<E extends LivingEntity, T extends LivingEntityPatch<E>,
        M extends HumanoidModel<E>, AM extends HumanoidMesh>
        extends WearableItemLayer<E, T, M, AM> {

    public FirstPersonWearableItemLayer(AssetAccessor<AM> meshProvider, ModelManager modelManager) {
        super(meshProvider, false, modelManager); // always false — we handle full body
    }

    @Override
    public void renderLayer(T entitypatch, E livingentity, HumanoidArmorLayer<E, M, M> vanillaLayer, PoseStack poseStack, MultiBufferSource buffers, int packedLight, OpenMatrix4f[] poses, float bob, float yRot, float xRot, float partialTicks) {
        for(EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
                boolean firstPersonChest = false;
                if (entitypatch.isFirstPerson() && ((AccessorWearableItemLayer)this).getIsFirstPerson()) {
                    if (slot != EquipmentSlot.CHEST) {
                        continue;
                    }
                    firstPersonChest = true;
                }

                if (slot != EquipmentSlot.HEAD || !Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
                    ItemStack itemstack = livingentity.getItemBySlot(slot);
                    Item item = itemstack.getItem();
                    if (item instanceof ArmorItem) {
                        ArmorItem armorItem = (ArmorItem)item;
                        if (slot != armorItem.getEquipmentSlot()) {
                            return;
                        }

                        poseStack.pushPose();
                        float head = 0.0F;
                        if (slot == EquipmentSlot.HEAD) {
                            poseStack.translate((double)0.0F, (double)head * 0.055, (double)0.0F);
                        }

                        HumanoidModel vanillaModel = ((AccessorHumanoidArmorLayer)vanillaLayer).invokeGetArmorModel(slot);
                        Model armorModel = ClientHooks.getArmorModel(livingentity, itemstack, slot, vanillaModel);
                        SkinnedMesh armorMesh = ((AccessorWearableItemLayer)this).invokeGetArmorModel(vanillaLayer, vanillaModel, armorModel, livingentity, armorItem, itemstack, slot);
                        if (armorMesh == null) {
                            poseStack.popPose();
                            return;
                        }

                        if (armorModel instanceof HumanoidModel) {
                            HumanoidModel humanoidModel = (HumanoidModel)armorModel;
                            boolean shouldSit = livingentity.isPassenger() && livingentity.getVehicle() != null && livingentity.getVehicle().shouldRiderSit();
                            float f8 = 0.0F;
                            float f5 = 0.0F;
                            if (!shouldSit && livingentity.isAlive()) {
                                f8 = livingentity.walkAnimation.speed(partialTicks);
                                f5 = livingentity.walkAnimation.position(partialTicks);
                                if (livingentity.isBaby()) {
                                    f5 *= 3.0F;
                                }

                                if (f8 > 1.0F) {
                                    f8 = 1.0F;
                                }
                            }

                            try {
                                humanoidModel.setupAnim(livingentity, f8, f5, bob, yRot, xRot);
                            } catch (ClassCastException var33) {
                            }

                            humanoidModel.head.loadPose(humanoidModel.head.getInitialPose());
                            humanoidModel.hat.loadPose(humanoidModel.hat.getInitialPose());
                            humanoidModel.body.loadPose(humanoidModel.body.getInitialPose());
                            humanoidModel.leftArm.loadPose(humanoidModel.leftArm.getInitialPose());
                            humanoidModel.rightArm.loadPose(humanoidModel.rightArm.getInitialPose());
                            humanoidModel.leftLeg.loadPose(humanoidModel.leftLeg.getInitialPose());
                            humanoidModel.rightLeg.loadPose(humanoidModel.rightLeg.getInitialPose());
                        }

                        armorMesh.initialize();
                        if (firstPersonChest) {
                            armorMesh.getAllParts().forEach((part) ->{
                                if(armorMesh.hasPart("head")) {
                                    if(part == armorMesh.getPart("head"))
                                        part.setHidden(true);
                                    else part.setHidden(false);
                                }
                                else if(armorMesh.hasPart("helmet")) {
                                    if(part == armorMesh.getPart("helmet"))
                                        part.setHidden(true);
                                    else part.setHidden(false);
                                }
                                else{
                                    part.setHidden(false);
                                }
                            });
                        }

                        ArmorMaterial armormaterial = (ArmorMaterial)armorItem.getMaterial().value();
                        IClientItemExtensions extensions = IClientItemExtensions.of(itemstack);
                        int fallbackColor = extensions.getDefaultDyeColor(itemstack);
                        boolean innerModel = AccessorWearableItemLayer.getInnerModel(slot);

                        for(int layerIdx = 0; layerIdx < armormaterial.layers().size(); ++layerIdx) {
                            ArmorMaterial.Layer armormaterial$layer = (ArmorMaterial.Layer)armormaterial.layers().get(layerIdx);
                            int packedColor = extensions.getArmorLayerTintColor(itemstack, livingentity, armormaterial$layer, layerIdx, fallbackColor);
                            if (packedColor != 0) {
                                Vector4f color = ColorUtil.unpackToARGBF(packedColor);
                                ResourceLocation texture = (ResourceLocation) ParseUtil.tryGetOr(() -> armorMesh.getRenderProperties().customTexturePath(), () -> ClientHooks.getArmorTexture(livingentity, itemstack, armormaterial$layer, innerModel, slot));
                                ((AccessorWearableItemLayer)this).invokeRenderArmor(poseStack, buffers, packedLight, armorMesh, entitypatch.getArmature(), color.x, color.y, color.z, texture, poses);
                            }
                        }

                        ArmorTrim armorTrim = (ArmorTrim)itemstack.get(DataComponents.TRIM);
                        if (armorTrim != null) {
                            ((AccessorWearableItemLayer)this).invokeRenderTrim(poseStack, buffers, packedLight, armorMesh, entitypatch.getArmature(), armorItem.getMaterial(), armorTrim, slot, poses);
                        }

                        if (itemstack.hasFoil()) {
                            ((AccessorWearableItemLayer)this).invokeRenderGlint(poseStack, buffers, packedLight, armorMesh, entitypatch.getArmature(), poses);
                        }

                        poseStack.popPose();
                    }
                }
            }
        }

    }
}
