package in.twizmwaz.cardinal.command;

import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissionsException;
import in.twizmwaz.cardinal.GameHandler;
import in.twizmwaz.cardinal.module.GameObjective;
import in.twizmwaz.cardinal.module.modules.team.TeamModule;
import in.twizmwaz.cardinal.module.modules.wools.WoolObjective;
import in.twizmwaz.cardinal.util.MiscUtil;
import in.twizmwaz.cardinal.util.Teams;
import org.apache.commons.lang.WordUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ProximityCommand {

    @Command(aliases = {"proximity"}, desc = "Shows the proximity of the objectives in the match.")
    public static void proximity(final CommandContext cmd, CommandSender sender) throws CommandException {
        if (!(sender instanceof Player) || (Teams.getTeamByPlayer((Player) sender).isPresent() && Teams.getTeamByPlayer((Player) sender).get().isObserver()) || !GameHandler.getGameHandler().getMatch().isRunning() || sender.hasPermission("cardinal.proximity")) {
            for (TeamModule team : Teams.getTeams()) {
                if (!team.isObserver()) {
                    sender.sendMessage(team.getCompleteName());
                    for (GameObjective objective : Teams.getShownObjectives(team)) {
                        if (objective.isComplete()) {
                            if (objective instanceof WoolObjective) {
                                WoolObjective wool = (WoolObjective) objective;
                                sender.sendMessage("  " + MiscUtil.convertDyeColorToChatColor(wool.getColor()) + WordUtils.capitalizeFully(objective.getName().replaceAll("_", " ")) + "  " + ChatColor.GREEN + "COMPLETE");
                            } else {
                                sender.sendMessage("  " + WordUtils.capitalizeFully(objective.getName().replaceAll("_", " ")) + "  " + ChatColor.GREEN + "COMPLETE");
                            }
                        }
                    }
                }
            }
        } else throw new CommandPermissionsException();
    }
}