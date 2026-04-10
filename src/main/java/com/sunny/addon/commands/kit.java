package com.sunny.addon.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.sunny.addon.utils.KitStorage;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.command.CommandSource;
import org.jetbrains.annotations.NotNull;

public final class kit extends Command {

    public kit() {
        super("kit", "Save or load inventory kits.", new String[0]);
    }

    @Override
    public void build(@NotNull LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("save").then(argument("name", StringArgumentType.word()).executes(ctx -> {
            String name = StringArgumentType.getString(ctx, "name");
            KitStorage.saveCurrentKit(name);
            this.info("Kit guardado: " + name);
            return 1;
        })));

        builder.then(literal("load").then(argument("name", StringArgumentType.word()).executes(ctx -> {
            String name = StringArgumentType.getString(ctx, "name");
            if (KitStorage.loadKit(name)) {
                this.info("Cargando Kit: " + name);
            } else {
                this.error("Este kit no existe: " + name);
            }
            return 1;
        })));

        builder.then(literal("rekit").executes(ctx -> {
            if (!KitStorage.hasActiveKit()) {
                this.error("Usa primero: .kit load <nombre>");
            } else {
                KitStorage.requestRekit();
                this.info("Kit seleccionado: " + KitStorage.getActiveKitName());
            }
            return 1;
        }));
    }
}
