package org.dynmap.griefprevention.commands;

import me.ryanhamshire.griefprevention.GriefPrevention;
import me.ryanhamshire.griefprevention.api.claim.Claim;
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

public class UnhideClaimCommand implements CommandExecutor {
    public static CommandSpec getCommandSpec() {
        return CommandSpec.builder()
                .description(Text.of("Unhides the claim the player is standing in from Dynmap."))
                .permission("dynmapgriefprevention.claims.unhide")
                .executor(new UnhideClaimCommand())
                .build();
    }

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
        if(src instanceof Player) {
            Player player = (Player) src;
            Claim claim = GriefPrevention.getApi().getClaimManager(player.getWorld()).getClaimAt(player.getLocation());

            // Make sure we're not about to unhide the wilderness
            if(claim.isWilderness()) {
                src.sendMessage(Text.of(TextColors.RED, "You're not standing in a claim!"));
                return CommandResult.empty();
            }

            if(DynmapGriefprevention.getInstance().unhideClaim(claim)) {
                src.sendMessage(Text.of(TextColors.GREEN, "The claim you're in has been unhidden."));
                return CommandResult.success();
            } else {
                src.sendMessage(Text.of(TextColors.RED, "This claim is already unhidden!"));
                return CommandResult.empty();
            }
        }
        else {
            throw new CommandException(Text.of("This command can only be run as a player."));
        }
    }
}