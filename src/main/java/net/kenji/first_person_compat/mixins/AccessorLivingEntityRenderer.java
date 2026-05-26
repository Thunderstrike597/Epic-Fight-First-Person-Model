package net.kenji.first_person_compat.mixins;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(value = LivingEntityRenderer.class, remap = false)
public interface AccessorLivingEntityRenderer<T extends LivingEntity, M extends EntityModel<T>> {
    @Accessor(value = "layers", remap = false)
    List<RenderLayer<T, M>> getLayers();
}
