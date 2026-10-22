package net.frostwalker.mixin;

import net.frostwalker.Frostwalker;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Enchantment.class)
public abstract class EnchantmentMixin {
    @Inject(method = "runLocationChangedEffects", at = @At("HEAD"), cancellable = true)
    private void frostwalker$disableVanillaFreezing(ServerLevel world, int level,
            EnchantedItemInUse item, LivingEntity wearer, CallbackInfo ci) {
        if (Frostwalker.isEnabled(wearer)) return;
        var frostWalker = world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.FROST_WALKER).value();
        if ((Object) this == frostWalker) ci.cancel();
    }
}
