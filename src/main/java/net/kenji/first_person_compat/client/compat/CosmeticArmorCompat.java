package net.kenji.first_person_compat.client.compat;

import lain.mods.cos.api.CosArmorAPI;
import lain.mods.cos.api.inventory.CAStacksBase;
import lain.mods.cos.impl.client.PlayerRenderHandler;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Everything Cosmetic Armor Reworked-specific lives here.
 * Never call into this class unless CAR is loaded (guard with ModList.get().isLoaded("cosmeticarmorreworked")).
 *
 * CAR's own renderer hook temporarily swaps the player's real armor for the cosmetic armor while
 * the vanilla PlayerRenderer runs. Our first person path never goes through that window, so we
 * resolve the visible stack ourselves using the same rules CAR uses.
 */
public final class CosmeticArmorCompat {

    private CosmeticArmorCompat() {}

    // CAStacksBase slot order: 0 = FEET, 1 = LEGS, 2 = CHEST, 3 = HEAD
    private static int toCosIndex(EquipmentSlot slot) {
        return switch (slot) {
            case FEET -> 0;
            case LEGS -> 1;
            case CHEST -> 2;
            case HEAD -> 3;
            default -> -1;
        };
    }

    /**
     * @param realStack what entity.getItemBySlot(slot) returned
     * @return the stack that should actually be drawn for this slot
     */
    public static ItemStack getVisibleArmor(LivingEntity entity, EquipmentSlot slot, ItemStack realStack) {
        if (!(entity instanceof Player player)) return realStack;

        // Client-side "stop CAR from functioning" toggle (the PvP option)
        if (PlayerRenderHandler.Disabled) return realStack;

        int index = toCosIndex(slot);
        if (index < 0) return realStack;

        CAStacksBase cosStacks = CosArmorAPI.getCAStacksClient(player.getUUID());

        // Player chose to hide the armor for this slot and show their skin instead
        if (cosStacks.isSkinArmor(index)) return ItemStack.EMPTY;

        ItemStack cosmetic = cosStacks.getStackInSlot(index);
        return cosmetic.isEmpty() ? realStack : cosmetic;
    }
}