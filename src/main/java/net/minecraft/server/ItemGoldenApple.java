package net.minecraft.server;

import org.bukkit.event.entity.EntityPotionEffectChangeEvent; // Tweakkit

public class ItemGoldenApple extends ItemFood {

    public ItemGoldenApple(int i, float f, boolean flag) {
        super(i, f, flag);
        this.a(true);
    }

    public EnumItemRarity f(ItemStack itemstack) {
        return itemstack.getData() == 0 ? EnumItemRarity.RARE : EnumItemRarity.EPIC;
    }

    protected void c(ItemStack itemstack, World world, EntityHuman entityhuman) {
        if (!world.isStatic) {
            // Tweakkit - Added 'EntityPotionEffectChangeEvent.Cause.GOLDEN_APPLE'
            entityhuman.addEffect(new MobEffect(MobEffectList.ABSORPTION.id, 2400, 0), EntityPotionEffectChangeEvent.Cause.GOLDEN_APPLE);
        }

        if (itemstack.getData() > 0) {
            if (!world.isStatic) {
                // Tweakkit start - Added 'EntityPotionEffectChangeEvent.Cause.ENCHANTED_GOLDEN_APPLE'
                entityhuman.addEffect(new MobEffect(MobEffectList.REGENERATION.id, 600, 4), EntityPotionEffectChangeEvent.Cause.ENCHANTED_GOLDEN_APPLE);
                entityhuman.addEffect(new MobEffect(MobEffectList.RESISTANCE.id, 6000, 0), EntityPotionEffectChangeEvent.Cause.ENCHANTED_GOLDEN_APPLE);
                entityhuman.addEffect(new MobEffect(MobEffectList.FIRE_RESISTANCE.id, 6000, 0), EntityPotionEffectChangeEvent.Cause.ENCHANTED_GOLDEN_APPLE);
                // Tweakkit end
            }
        } else {
            super.c(itemstack, world, entityhuman);
        }
    }
}
