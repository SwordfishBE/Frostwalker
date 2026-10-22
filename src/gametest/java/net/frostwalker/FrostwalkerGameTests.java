package net.frostwalker;

import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FrostedIceBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.PowderSnowBlock;

public class FrostwalkerGameTests {
    private static final BlockPos WATER = new BlockPos(2, 1, 2);

    private ItemStack boots(GameTestHelper h, int level) {
        var stack = new ItemStack(Items.DIAMOND_BOOTS);
        stack.enchant(h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.FROST_WALKER), level);
        return stack;
    }

    private LivingEntity wearer(GameTestHelper h, int level) {
        var zombie = h.spawn(EntityTypes.ZOMBIE, WATER.above());
        zombie.setNoAi(true);
        zombie.setItemSlot(EquipmentSlot.FEET, boots(h, level));
        var pos = h.absolutePos(WATER.above());
        zombie.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        zombie.setOnGround(true);
        return zombie;
    }

    private void water(GameTestHelper h) {
        h.setBlock(WATER, Blocks.WATER);
        h.setBlock(WATER.above(), Blocks.AIR);
    }

    @GameTest
    public void stationaryIceBothLevels(GameTestHelper h) {
        for (int level = 1; level <= 2; level++) {
            water(h);
            var wearer = wearer(h, level);
            FrostWalkerEffect.tick(wearer);
            h.assertBlockPresent(Blocks.FROSTED_ICE, WATER);
            wearer.discard();
        }
        h.succeed();
    }

    @GameTest
    public void livingTickHookGeneratesIce(GameTestHelper h) {
        water(h);
        var wearer = wearer(h, 1);
        wearer.tick();
        h.assertBlockPresent(Blocks.FROSTED_ICE, WATER);
        h.succeed();
    }

    @GameTest
    public void allTenMountsUseRiderBoots(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setItemSlot(EquipmentSlot.FEET, boots(h, 1));
        for (var type : List.of(EntityTypes.HORSE, EntityTypes.CAMEL, EntityTypes.PIG,
                EntityTypes.DONKEY, EntityTypes.MULE, EntityTypes.SKELETON_HORSE,
                EntityTypes.ZOMBIE_HORSE, EntityTypes.CAMEL_HUSK,
                EntityTypes.LLAMA, EntityTypes.TRADER_LLAMA)) {
            water(h);
            var mount = h.spawn(type, WATER.above());
            mount.setNoAi(true);
            var pos = h.absolutePos(WATER.above());
            mount.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            mount.setOnGround(true);
            h.assertTrue(player.startRiding(mount, true, true), "Rider must mount " + type);
            FrostWalkerEffect.tick(player);
            h.assertBlockPresent(Blocks.FROSTED_ICE, WATER);
            player.stopRiding();
            mount.discard();
        }
        h.succeed();
    }

    @GameTest
    public void stridersBoatsAndOtherVehiclesExcluded(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setItemSlot(EquipmentSlot.FEET, boots(h, 2));
        for (var type : List.of(EntityTypes.STRIDER, EntityTypes.OAK_BOAT, EntityTypes.MINECART,
                EntityTypes.HAPPY_GHAST, EntityTypes.NAUTILUS)) {
            water(h);
            var mount = h.spawn(type, WATER.above());
            mount.setOnGround(true);
            h.assertTrue(player.startRiding(mount, true, true), "Rider must mount " + type);
            FrostWalkerEffect.tick(player);
            h.assertBlockPresent(Blocks.WATER, WATER);
            player.stopRiding();
            mount.discard();
        }
        h.succeed();
    }

    @GameTest
    public void toggleStopsCustomAndVanillaIce(GameTestHelper h) {
        water(h);
        var wearer = wearer(h, 2);
        Frostwalker.setEnabled(wearer, false);
        FrostWalkerEffect.tick(wearer);
        EnchantmentHelper.runLocationChangedEffects(h.getLevel(), wearer);
        h.assertBlockPresent(Blocks.WATER, WATER);
        Frostwalker.setEnabled(wearer, true);
        EnchantmentHelper.runLocationChangedEffects(h.getLevel(), wearer);
        h.assertBlockPresent(Blocks.FROSTED_ICE, WATER);
        h.succeed();
    }

    @GameTest
    public void powderSnowKeepsVanillaBehavior(GameTestHelper h) {
        var wearer = wearer(h, 1);
        h.assertFalse(PowderSnowBlock.canEntityWalkOnPowderSnow(wearer), "Frost Walker must not add snow support");
        Frostwalker.setEnabled(wearer, false);
        h.assertFalse(PowderSnowBlock.canEntityWalkOnPowderSnow(wearer), "Toggle leaves vanilla snow behavior unchanged");
        wearer.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.LEATHER_BOOTS));
        h.assertTrue(PowderSnowBlock.canEntityWalkOnPowderSnow(wearer), "Vanilla leather still supports snow");
        h.succeed();
    }

    @GameTest
    public void airborneAndUnequippedDoNotFreeze(GameTestHelper h) {
        water(h);
        var wearer = wearer(h, 1);
        wearer.setOnGround(false);
        FrostWalkerEffect.tick(wearer);
        h.assertBlockPresent(Blocks.WATER, WATER);
        wearer.setOnGround(true);
        wearer.setItemSlot(EquipmentSlot.MAINHAND, boots(h, 1));
        wearer.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
        FrostWalkerEffect.tick(wearer);
        h.assertBlockPresent(Blocks.WATER, WATER);
        h.assertFalse(PowderSnowBlock.canEntityWalkOnPowderSnow(wearer), "Held boots do not support snow");
        h.succeed();
    }

    @GameTest
    public void flowingWaterLavaAndCoveredWaterUntouched(GameTestHelper h) {
        var wearer = wearer(h, 1);
        h.setBlock(WATER, Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 1));
        FrostWalkerEffect.tick(wearer);
        h.assertBlockProperty(WATER, LiquidBlock.LEVEL, 1);
        h.setBlock(WATER, Blocks.LAVA);
        FrostWalkerEffect.tick(wearer);
        h.assertBlockPresent(Blocks.LAVA, WATER);
        water(h);
        h.setBlock(WATER.above(), Blocks.STONE);
        FrostWalkerEffect.tick(wearer);
        h.assertBlockPresent(Blocks.WATER, WATER);
        h.succeed();
    }

    @GameTest
    public void enchantmentLevelControlsRadius(GameTestHelper h) {
        var edge = WATER.offset(3, 0, 0);
        water(h);
        h.setBlock(edge, Blocks.WATER);
        var wearer = wearer(h, 1);
        FrostWalkerEffect.tick(wearer);
        h.assertBlockPresent(Blocks.WATER, edge);
        wearer.setItemSlot(EquipmentSlot.FEET, boots(h, 2));
        FrostWalkerEffect.tick(wearer);
        h.assertBlockPresent(Blocks.FROSTED_ICE, edge);
        h.succeed();
    }

    @GameTest
    public void mountedToggleAndBootRemoval(GameTestHelper h) {
        water(h);
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var mount = h.spawn(EntityTypes.HORSE, WATER.above());
        mount.setNoAi(true);
        mount.setOnGround(true);
        h.assertTrue(player.startRiding(mount, true, true), "Rider must mount");
        player.setItemSlot(EquipmentSlot.FEET, boots(h, 2));
        Frostwalker.setEnabled(player, false);
        FrostWalkerEffect.tick(player);
        h.assertBlockPresent(Blocks.WATER, WATER);
        Frostwalker.setEnabled(player, true);
        player.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
        FrostWalkerEffect.tick(player);
        h.assertBlockPresent(Blocks.WATER, WATER);
        h.succeed();
    }

    @GameTest
    public void stationaryIceRefreshesAndSchedulesMelting(GameTestHelper h) {
        water(h);
        var wearer = wearer(h, 1);
        FrostWalkerEffect.tick(wearer);
        h.assertTrue(h.getLevel().getBlockTicks().hasScheduledTick(h.absolutePos(WATER), Blocks.FROSTED_ICE),
                "Ice retains vanilla scheduled melting");
        h.setBlock(WATER, Blocks.FROSTED_ICE.defaultBlockState().setValue(FrostedIceBlock.AGE, 2));
        FrostWalkerEffect.tick(wearer);
        h.assertBlockProperty(WATER, FrostedIceBlock.AGE, 0);
        h.succeed();
    }
}
