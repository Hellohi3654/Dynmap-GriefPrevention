package org.dynmap.griefprevention.commands;

import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;

public class DynmapGriefpreventionCommand implements CommandExecutor {
    public static CommandSpec getCommandSpec() {
        return CommandSpec.builder()
                .description(Text.of("Provides information on Dynmap GriefPrevention's commands."))
                .permission("dynmapgriefprevention.base")
                .executor(new DynmapGriefpreventionCommand())
                .child(ReloadCommand.getCommandSpec(), "reload")
                .child(HideClaimCommand.getCommandSpec(), "hideclaim", "hide")
                .child(UnhideClaimCommand.getCommandSpec(), "unhideclaim", "unhide", "showclaim", "show")
                .build();
    }

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
        src.sendMessage(Text.of(TextColors.GOLD, TextStyles.BOLD, "Dynmap GriefPrevention commands:"));
        src.sendMessage(Text.of(TextColors.GOLD, "/dynmapgriefprevention"));
        src.sendMessage(Text.of("  - Displays this help, begins other commands."));
        src.sendMessage(Text.of("  - Aliases: /dynmapgp, /dyngp, /dmgp, /dgp"));
        src.sendMessage(Text.of(TextColors.GOLD, "/dynmapgriefprevention reload"));
        src.sendMessage(Text.of("  - Reloads Dynmap GriefPrevention's configuration."));
        src.sendMessage(Text.of(TextColors.GOLD, "/dynmapgriefprevention hideclaim"));
        src.sendMessage(Text.of("  - Hides the claim the player is standing in from Dynmap."));
        src.sendMessage(Text.of("  - Aliases: hide"));
        src.sendMessage(Text.of(TextColors.GOLD, "/dynmapgriefprevention unhideclaim"));
        src.sendMessage(Text.of("  - Unhides the claim the player is standing in from Dynmap."));
        src.sendMessage(Text.of("  - Aliases: unhide, showclaim, show"));
        return CommandResult.empty();
    }
}
