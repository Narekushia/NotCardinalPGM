package in.twizmwaz.cardinal.module.modules.motd;

import in.twizmwaz.cardinal.Cardinal;
import in.twizmwaz.cardinal.match.Match;
import in.twizmwaz.cardinal.match.MatchState;
import in.twizmwaz.cardinal.module.Module;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.server.ServerListPingEvent;

public class MOTD implements Module {

	private final Match match;

	protected MOTD(Match match) {
		this.match = match;
	}

	@Override
	public void unload() {
		HandlerList.unregisterAll(this);
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onServerListPing(ServerListPingEvent event) {
		try {
			String name = match.getLoadedMap().getName();
			String message = Cardinal.getInstance().getConfig().getString("server-message");
			String matchstate = "";
			switch (match.getState()) {
			case ENDED:
				matchstate = ChatColor.RED + "Ended";
				break;
			case CYCLING:
				matchstate = ChatColor.AQUA + "Cycling";
				break;
			case PLAYING:
				matchstate = ChatColor.YELLOW + "Playing";
				break;
			case STARTING:
				matchstate = ChatColor.GREEN + "Starting";
				break;
			case WAITING:
				matchstate = ChatColor.GRAY + "Waiting";
				break;
			default:
				matchstate = "";
				break;
			}
			event.setMotd(ChatColor.GOLD + "" + ChatColor.BOLD + message + ChatColor.WHITE + " \u007C " + matchstate + "\n" + ChatColor.WHITE+ "Map : " + ChatColor.AQUA + name);

		} catch (NullPointerException ex) {

		}
	}
}
