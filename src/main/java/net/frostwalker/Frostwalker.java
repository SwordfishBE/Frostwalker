package net.frostwalker;

import com.mojang.serialization.Codec;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Frostwalker implements ModInitializer {
    public static final String MOD_ID = "frostwalker";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final AttachmentType<Boolean> ENABLED = AttachmentRegistry.create(
            id("enabled"), builder -> builder.initializer(() -> true).persistent(Codec.BOOL)
                    .copyOnDeath());

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    public static boolean isEnabled(LivingEntity entity) {
        return entity.getAttachedOrElse(ENABLED, true);
    }

    public static void setEnabled(LivingEntity entity, boolean enabled) {
        entity.setAttached(ENABLED, enabled);
    }

    public static int toggle(ServerPlayer player) {
        boolean enabled = !isEnabled(player);
        setEnabled(player, enabled);
        player.sendSystemMessage(Component.translatableWithFallback(enabled
                ? "message.frostwalker.enabled" : "message.frostwalker.disabled", enabled ? "Frost Walker enabled" : "Frost Walker disabled"), true);
        return 1;
    }

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(Commands.literal(MOD_ID)
                        .executes(context -> toggle(context.getSource().getPlayerOrException()))
                        .then(Commands.literal("toggle").executes(context ->
                                toggle(context.getSource().getPlayerOrException())))
                        .then(Commands.literal("on").executes(context ->
                                setFromCommand(context.getSource().getPlayerOrException(), true)))
                        .then(Commands.literal("off").executes(context ->
                                setFromCommand(context.getSource().getPlayerOrException(), false)))));
        String version = FabricLoader.getInstance().getModContainer(MOD_ID).orElseThrow()
                .getMetadata().getVersion().getFriendlyString();
        LOGGER.info("[Frostwalker] Mod initialized. Version: {}", version);
    }

    private static int setFromCommand(ServerPlayer player, boolean enabled) {
        setEnabled(player, enabled);
        player.sendSystemMessage(Component.translatableWithFallback(enabled
                ? "message.frostwalker.enabled" : "message.frostwalker.disabled", enabled ? "Frost Walker enabled" : "Frost Walker disabled"), true);
        return 1;
    }
}

