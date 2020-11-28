package org.dynmap.griefprevention.commands;

import org.dynmap.griefprevention.DynmapGriefprevention;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.game.GameReloadEvent;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

public class ReloadCommand implements CommandExecutor {
    public static CommandSpec getCommandSpec() {
        return CommandSpec.builder()
                .description(Text.of("Reloads Dynmap GriefPrevention's configuration."))
                .permission("dynmapgriefprevention.reload")
                .executor(new ReloadCommand())
                .build();
    }

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
        if(DynmapGriefprevention.getInstance().reload()) {
            src.sendMessage(Text.of(TextColors.GREEN, "Plugin reloaded."));
            return CommandResult.success();
        }
        else {
            src.sendMessage(Text.of(TextColors.RED, "Error reloading plugin!"));
            return CommandResult.empty();
        }
    }
}