package net.kenji.first_person_compat.mixins;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = HumanoidArmorLayer.class, remap = false)
public interface AccessorHumanoidArmorLayer<T extends LivingEntity, M extends HumanoidModel<T>, A extends HumanoidModel<T>>{

    @Invoker("getArmorModel")
    A invokeGetArmorModel(EquipmentSlot slot);
}