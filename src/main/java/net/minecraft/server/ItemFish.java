package net.minecraft.server;

import org.bukkit.event.entity.EntityPotionEffectChangeEvent; // Tweakkit

public class ItemFish extends ItemFood {

    private final boolean b;

    public ItemFish(boolean flag) {
        super(0, 0.0F, false);
        this.b = flag;
    }

    public int getNutrition(ItemStack itemstack) {
        EnumFish enumfish = EnumFish.a(itemstack);

        return this.b && enumfish.i() ? enumfish.e() : enumfish.c();
    }

    public float getSaturationModifier(ItemStack itemstack) {
        EnumFish enumfish = EnumFish.a(itemstack);

        return this.b && enumfish.i() ? enumfish.f() : enumfish.d();
    }

    public String i(ItemStack itemstack) {
        return EnumFish.a(itemstack) == EnumFish.PUFFERFISH ? PotionBrewer.m : null;
    }

    protected void c(ItemStack itemstack, World world, EntityHuman entityhuman) {
        EnumFish enumfish = EnumFish.a(itemstack);

        if (enumfish == EnumFish.PUFFERFISH) {
            // Tweakkit start - Added 'EntityPotionEffectChangeEvent.Cause.FOOD'
            entityhuman.addEffect(new MobEffect(MobEffectList.POISON.id, 1200, 3), EntityPotionEffectChangeEvent.Cause.FOOD);
            entityhuman.addEffect(new MobEffect(MobEffectList.HUNGER.id, 300, 2), EntityPotionEffectChangeEvent.Cause.FOOD);
            entityhuman.addEffect(new MobEffect(MobEffectList.CONFUSION.id, 300, 1), EntityPotionEffectChangeEvent.Cause.FOOD);
            // Tweakkit end
        }

        //super.c(itemstack, world, entityhuman); Tweakkit - Causes the client to add effects even if events are cancelled
    }

    public String a(ItemStack itemstack) {
        EnumFish enumfish = EnumFish.a(itemstack);

        return this.getName() + "." + enumfish.b() + "." + (this.b && enumfish.i() ? "cooked" : "raw");
    }
}