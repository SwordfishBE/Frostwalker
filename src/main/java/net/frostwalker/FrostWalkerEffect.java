package net.frostwalker;

import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;

public final class FrostWalkerEffect {
    private static final Set<EntityType<?>> MOUNTS = Set.of(
            EntityTypes.HORSE, EntityTypes.CAMEL, EntityTypes.PIG, EntityTypes.DONKEY,
            EntityTypes.MULE, EntityTypes.SKELETON_HORSE, EntityTypes.ZOMBIE_HORSE,
            EntityTypes.CAMEL_HUSK, EntityTypes.LLAMA, EntityTypes.TRADER_LLAMA);

    private FrostWalkerEffect() {}

    public static int level(LivingEntity entity) {
        var boots = entity.getItemBySlot(EquipmentSlot.FEET);
        if (boots.isEmpty() || boots.getEnchantments().isEmpty()) return 0;
        if (!Frostwalker.isEnabled(entity)) return 0;
        var enchantment = entity.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.FROST_WALKER);
        return EnchantmentHelper.getItemEnchantmentLevel(enchantment, boots);
    }

    public static boolean supportsMount(Entity entity) {
        return MOUNTS.contains(entity.getType());
    }

    public static void tick(LivingEntity wearer) {
        if (!(wearer.level() instanceof ServerLevel world)) return;
        int enchantmentLevel = level(wearer);
        if (enchantmentLevel == 0) return;
        Entity base = wearer.getRootVehicle();
        if (base != wearer && !supportsMount(base)) return;
        // Keep vanilla's grounded requirement: no freezing while jumping, flying or swimming.
        if (!base.onGround()) return;
        int radius = Math.min(16, 2 + enchantmentLevel);
        freeze(world, wearer, base.position(), radius);
    }

    private static void freeze(ServerLevel world, LivingEntity wearer, Vec3 center, int radius) {
        int y = Mth.floor(center.y) - 1;
        BlockPos origin = BlockPos.containing(center.x, y, center.z);
        var ice = Blocks.FROSTED_ICE.defaultBlockState();
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-radius, 0, -radius),
                origin.offset(radius, 0, radius))) {
            double dx = pos.getX() + 0.5 - center.x;
            double dz = pos.getZ() + 0.5 - center.z;
            if (dx * dx + dz * dz >= radius * radius || !world.hasChunkAt(pos)) continue;
            if (!world.getBlockState(pos.above()).isAir()) continue;
            var state = world.getBlockState(pos);
            if (state.is(Blocks.FROSTED_ICE)) {
                // Refresh existing ice so that it remains safe while standing still.
                if (state != ice) world.setBlockAndUpdate(pos, ice);
                continue;
            }
            if (!state.is(Blocks.WATER) || !state.getFluidState().isSource()) continue;
            if (!ice.canSurvive(world, pos) || !world.isUnobstructed(null, Shapes.block().move(pos))) continue;
            if (world.setBlockAndUpdate(pos, ice)) {
                world.gameEvent(wearer, GameEvent.BLOCK_PLACE, pos);
            }
        }
    }
}
