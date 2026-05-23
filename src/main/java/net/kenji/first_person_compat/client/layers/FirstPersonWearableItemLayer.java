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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraftforge.client.ForgeHooksClient;
import org.jline.utils.Log;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.client.model.SkinnedMesh;
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
    public void renderLayer(T entitypatch, E entityliving, HumanoidArmorLayer<E, M, M> vanillaLayer,
                            PoseStack poseStack, MultiBufferSource buf, int packedLight, OpenMatrix4f[] poses,
                            float bob, float yRot, float xRot, float partialTicks) {
        Log.info("FirstPersonWearableItemLayer.renderLayer CALLED");

        for(EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.ARMOR) {
                boolean firstPersonChest = false;
                if (entitypatch.isFirstPerson() && ((AccessorWearableItemLayer)this).getIsFirstPerson()) {
                    if (slot != EquipmentSlot.CHEST) {
                        continue;
                    }

                    firstPersonChest = true;
                }

                if (slot != EquipmentSlot.HEAD || !Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
                    ItemStack itemstack = entityliving.getItemBySlot(slot);
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

                        HumanoidModel defaultModel = ((AccessorHumanoidArmorLayer)vanillaLayer).invokeGetArmorModel(slot);
                        Model armorModel = ForgeHooksClient.getArmorModel(entityliving, itemstack, slot, defaultModel);
                        SkinnedMesh armorMesh = ((AccessorWearableItemLayer)this).invokeGetArmorModel(vanillaLayer, defaultModel, armorModel, entityliving, armorItem, itemstack, slot);
                        if (armorMesh == null) {
                            poseStack.popPose();
                            return;
                        }

                        if (armorModel instanceof HumanoidModel) {
                            HumanoidModel humanoidModel = (HumanoidModel)armorModel;
                            boolean shouldSit = entityliving.isPassenger() && entityliving.getVehicle() != null && entityliving.getVehicle().shouldRiderSit();
                            float f8 = 0.0F;
                            float f5 = 0.0F;
                            if (!shouldSit && entityliving.isAlive()) {
                                f8 = entityliving.walkAnimation.speed(partialTicks);
                                f5 = entityliving.walkAnimation.position(partialTicks);
                                if (entityliving.isBaby()) {
                                    f5 *= 3.0F;
                                }

                                if (f8 > 1.0F) {
                                    f8 = 1.0F;
                                }
                            }

                            try {
                                humanoidModel.setupAnim(entityliving, f8, f5, bob, yRot, xRot);
                            } catch (ClassCastException var29) {
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

                        if (armorItem instanceof DyeableLeatherItem) {
                            DyeableLeatherItem dyeableItem = (DyeableLeatherItem)armorItem;
                            int i = dyeableItem.getColor(itemstack);
                            float r = (float)(i >> 16 & 255) / 255.0F;
                            float g = (float)(i >> 8 & 255) / 255.0F;
                            float b = (float)(i & 255) / 255.0F;
                            ((AccessorWearableItemLayer)this).invokeRenderArmor(poseStack, buf, packedLight, armorMesh, entitypatch.getArmature(), r, g, b, ((AccessorWearableItemLayer)this).invokeGetArmorTexture(itemstack, entityliving, armorMesh, slot, (String)null, defaultModel), poses);
                            ((AccessorWearableItemLayer)this).invokeRenderArmor(poseStack, buf, packedLight, armorMesh, entitypatch.getArmature(), 1.0F, 1.0F, 1.0F, ((AccessorWearableItemLayer)this).invokeGetArmorTexture(itemstack, entityliving, armorMesh, slot, "overlay", defaultModel), poses);
                        } else {
                            ((AccessorWearableItemLayer)this).invokeRenderArmor(poseStack, buf, packedLight, armorMesh, entitypatch.getArmature(), 1.0F, 1.0F, 1.0F, ((AccessorWearableItemLayer)this).invokeGetArmorTexture(itemstack, entityliving, armorMesh, slot, (String)null, defaultModel), poses);
                        }

                        ArmorTrim.getTrim(entityliving.level().registryAccess(), itemstack).ifPresent((armorTrim) -> ((AccessorWearableItemLayer)this).invokeRenderTrim(poseStack, buf, packedLight, armorMesh, entitypatch.getArmature(), armorItem.getMaterial(), armorTrim, slot, poses));
                        if (itemstack.hasFoil()) {
                            ((AccessorWearableItemLayer)this).invokeRenderGlint(poseStack, buf, packedLight, armorMesh, entitypatch.getArmature(), poses);
                        }

                        poseStack.popPose();
                        Log.info("Logging First Person Armor!!");

                    }
                }
            }
        }
    }
}
