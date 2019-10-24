package org.dynmap.griefprevention.commands;

import me.ryanhamshire.griefprevention.GriefPrevention;
import me.ryanhamshire.griefprevention.api.claim.Claim;
import org.dynmap.griefprevention.Config;
import org.dynmap.griefprevention.DynmapGriefprevention;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

public class HideClaimCommand implements CommandExecutor {
    public static CommandSpec getCommandSpec() {
        return CommandSpec.builder()
                .description(Text.of("Hides the claim the player is standing in from Dynmap."))
                .permission("dynmapgriefprevention.claims.hide")
                .executor(new HideClaimCommand())
                .build();
    }

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
        if(src instanceof Player) {
            Player player = (Player) src;
            Claim claim = GriefPrevention.getApi().getClaimManager(player.getWorld()).getClaimAt(player.getLocation());

            // Make sure we're not about to hide the wilderness
            if(claim.isWilderness()) {
                src.sendMessage(Text.of(TextColors.RED, "You're not standing in a claim!"));
                return CommandResult.empty();
            }

            if(DynmapGriefprevention.getInstance().hideClaim(claim)) {
                src.sendMessage(Text.of(TextColors.GREEN, "The claim you're in has been hidden."));
                return CommandResult.success();
            } else {
                src.sendMessage(Text.of(TextColors.RED, "This claim is already hidden!"));
                return CommandResult.empty();
            }
        }
        else {
            throw new CommandException(Text.of("This command can only be run as a player."));
        }
    }
}
