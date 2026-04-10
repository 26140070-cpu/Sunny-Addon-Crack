package com.sunny.addon.commands;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.sunny.addon.modules.autopilot;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.command.CommandSource;

public class autopilotcommand extends Command {
    public autopilotcommand() {
        super("autopilot", "Cords", new String[0]);
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("x", DoubleArgumentType.doubleArg())
            .then(argument("z", DoubleArgumentType.doubleArg())
                .executes(ctx -> {
                    double x = DoubleArgumentType.getDouble(ctx, "x");
                    double z = DoubleArgumentType.getDouble(ctx, "z");

                    autopilot autoPilot = (autopilot) Modules.get().get(autopilot.class);

                    autoPilot.setTarget(x, z);

                    if (!autoPilot.isActive()) {
                        autoPilot.toggle();
                    }

                    this.info("AutoPilot volando hacia X: " + x + " Z: " + z);

                    return 1;
                })));
    }
}
