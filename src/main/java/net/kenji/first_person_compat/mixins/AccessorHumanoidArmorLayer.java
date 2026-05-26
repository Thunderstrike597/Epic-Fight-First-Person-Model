package net.kenji.first_person_compat.mixins;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import yesman.epicfight.api.client.model.SkinnedMesh;

@Mixin(value = HumanoidArmorLayer.class, remap = false)
public interface AccessorHumanoidArmorLayer<T extends LivingEntity, M extends HumanoidModel<T>, A extends HumanoidModel<T>>{

    @Invoker(value = "getArmorModel", remap = false)
    A invokeGetArmorModel(EquipmentSlot slot);
}