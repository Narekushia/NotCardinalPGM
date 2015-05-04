package in.twizmwaz.cardinal.module.modules.stats;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import in.twizmwaz.cardinal.Cardinal;
import in.twizmwaz.cardinal.GameHandler;
import in.twizmwaz.cardinal.chat.ChatConstant;
import in.twizmwaz.cardinal.chat.UnlocalizedChatMessage;
import in.twizmwaz.cardinal.event.CardinalDeathEvent;
import in.twizmwaz.cardinal.event.MatchEndEvent;
import in.twizmwaz.cardinal.event.MatchStartEvent;
import in.twizmwaz.cardinal.event.PlayerChangeTeamEvent;
import in.twizmwaz.cardinal.module.Module;
import in.twizmwaz.cardinal.module.modules.chatChannels.ChatChannelModule;
import in.twizmwaz.cardinal.module.modules.chatChannels.GlobalChannel;
import in.twizmwaz.cardinal.module.modules.matchTimer.MatchTimer;
import in.twizmwaz.cardinal.module.modules.matchTranscript.MatchTranscript;
import in.twizmwaz.cardinal.module.modules.team.TeamModule;
import in.twizmwaz.cardinal.settings.Settings;
import in.twizmwaz.cardinal.util.StringUtils;
import in.twizmwaz.cardinal.util.TeamUtils;

import org.apache.commons.io.IOUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.*;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class Stats implements Module {


    private List<MatchTracker> stats;
    private Map<OfflinePlayer, TeamModule> playerTeams = Maps.newHashMap();
    private String WinningTeam = "";

    protected Stats() {
        stats = Lists.newArrayList();
    }

    @Override
    public void unload() {
        HandlerList.unregisterAll(this);
    }

    public void add(MatchTracker tracker) {
        stats.add(tracker);
    }

    @EventHandler
    public void onCardinalDeath(CardinalDeathEvent event) {
        this.add(new MatchTracker(event.getPlayer(), event.getKiller(), event.getPlayer().getItemInHand()));
    }

    public int getKillsByPlayer(OfflinePlayer player) {
        int kills = 0;
        if (player == null) return 0;
        for (MatchTracker tracker : this.stats) {
            if (tracker.getKiller() != null && tracker.getKiller().equals(player)) {
                kills++;
            }
        }
        return kills;
    }

    public int getDeathsByPlayer(OfflinePlayer player) {
        int deaths = 0;
        if (player == null) return 0;
        for (MatchTracker tracker : this.stats) {
            if (tracker.getPlayer().equals(player)) {
                deaths++;
            }
        }
        return deaths;
    }

    public int getTotalKills() {
        int kills = 0;
        for (MatchTracker tracker : this.stats) {
            if (tracker.getKiller() != null) {
                kills++;
            }
        }
        return kills;
    }

    public int getTotalDeaths() {
        int deaths = 0;
        for (MatchTracker tracker : this.stats) {
            deaths++;
        }
        return deaths;
    }

    public double getKdByPlayer(OfflinePlayer player) {
        double kd;
        if (player == null) return 0;
        kd = getDeathsByPlayer(player) == 0 ? (double) getKillsByPlayer(player) : ((double) getKillsByPlayer(player) / getDeathsByPlayer(player));
        return kd;
    }

    /**
     * Sends player stats to player
     */
    @EventHandler
    public void onMatchEnd(MatchEndEvent event) {
    	WinningTeam = event.getTeam().getName();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (Settings.getSettingByName("Stats") != null && Settings.getSettingByName("Stats").getValueByPlayer(player).getValue().equalsIgnoreCase("on")) {
                player.sendMessage(ChatColor.GRAY + "Kills: " + ChatColor.GREEN + getKillsByPlayer(player) + ChatColor.AQUA + " | " + ChatColor.GRAY + "Deaths: " + ChatColor.DARK_RED + getDeathsByPlayer(player) + ChatColor.AQUA + " | " + ChatColor.GRAY + "KD: " + ChatColor.GOLD + (Math.round(getKdByPlayer(player) * 100.0) / 100.0));
            }
        }

        if (Cardinal.getInstance().getConfig().getBoolean("html.upload")) {
        	Bukkit.getScheduler().scheduleSyncDelayedTask(GameHandler.getGameHandler().getPlugin(), new Runnable() {
            	public void run() {
                	ChatChannelModule global = GameHandler.getGameHandler().getMatch().getModules().getModule(GlobalChannel.class);
                	global.sendLocalizedMessage(new UnlocalizedChatMessage(ChatColor.GOLD + "{0}", ChatConstant.UI_MATCH_REPORT_UPLOAD.asMessage()));
                	String result = uploadStats();
                	if (result == null || result.contains("error"))
                    	global.sendLocalizedMessage(new UnlocalizedChatMessage(ChatColor.RED + "{0}", ChatConstant.UI_MATCH_REPORT_FAILED.asMessage()));
                	else global.sendLocalizedMessage(new UnlocalizedChatMessage(ChatColor.GREEN + "{0}", ChatConstant.UI_MATCH_REPORT_SUCCESS.asMessage(new UnlocalizedChatMessage(ChatColor.UNDERLINE + "" + result + "" + ChatColor.RESET))));
            	}
        	}, 20);
        }
    }

    @EventHandler
    public void onPlayerJoinTeam(PlayerChangeTeamEvent event) {
        this.playerTeams.put(event.getPlayer(), event.getNewTeam());
    }

    @EventHandler
    public void onMatchStart(MatchStartEvent event) {
        try {
            File template = new File(GameHandler.getGameHandler().getMatchFile() + "/statistics.html");
            OutputStream out = new FileOutputStream(template);
            IOUtils.copy(GameHandler.getGameHandler().getPlugin().getResource("statistics.html"), out);
            out.close();
        } catch (IOException e) {
            Bukkit.getLogger().warning("Unable to copy template statistics file!");
            e.printStackTrace();
        }
    }


    private File generateStats() throws IOException {
        File file = new File(GameHandler.getGameHandler().getMatchFile() + "/statistics.html");
        Document document = Jsoup.parse(file, "utf-8");
        for (Element element : document.getElementsContainingOwnText("%mapName")) {
            element.html(element.html().replace("%mapName", GameHandler.getGameHandler().getMatch().getLoadedMap().getName()).toString());
        }
        for (Element element : document.getElementsContainingOwnText("%date")) {
            element.html(element.html().replace("%date", new Date().toString()));
        }
        for (Element element : document.getElementsContainingOwnText("%kills")) {
            element.html(element.html().replace("%kills", Integer.toString(getTotalKills())));
        }
        for (Element element : document.getElementsContainingOwnText("%deaths")) {
            element.html(element.html().replace("%deaths", Integer.toString(getTotalDeaths())));
        }
        for (Element element : document.getElementsContainingOwnText("%matchTime")) {
            // element.text(element.text().replace("%matchTime", Double.toString(GameHandler.getGameHandler().getMatch().getModules().getModule(MatchTimer.class).getEndTime())));
            element.html(element.html().replace("%matchTime", StringUtils.formatTime(GameHandler.getGameHandler().getMatch().getModules().getModule(MatchTimer.class).getEndTime())));
        }
        
        // Get map.png URL on maps.oc.tc
        Document MapHTMLPage = Jsoup.connect("https://oc.tc/maps/" + GameHandler.getGameHandler().getMatch().getLoadedMap().getName().toLowerCase().replace(" ", "_")).get();
        String MapURL = MapHTMLPage.select("img[src$=.png][class*=thumbnail]").attr("src");
        MapURL = MapURL.replace(" ", "%20");
        document.getElementById("mapimage").appendElement("img").attr("class", "ui medium rounded image").attr("src", MapURL).attr("alt", "Map thumbnail");
        
        Element teams = document.getElementById("teams");
        for (TeamModule team : TeamUtils.getTeams()) {
        	
        	Element TeamGrid = teams.appendElement("div").attr("class", "ui grid");
        	Element TeamTitle =TeamGrid.appendElement("div").attr("class", "sixteen wide column").appendElement("span").attr("class", "ui large header").text(team.getName());
            
        	if(team.getName() == WinningTeam) 
            	TeamTitle.appendElement("span").attr("class", "ui small green label").text("Winning Team");
            else if (!team.isObserver())
            	TeamTitle.appendElement("span").attr("class", "ui small red label").text("Losing Team");
            
            for (Map.Entry<OfflinePlayer, TeamModule> entry : playerTeams.entrySet()) {
                if (entry.getValue() == team) {
                	Element PlayerItem = TeamGrid.appendElement("div").attr("class", "four wide column").appendElement("div").attr("class", "ui items").appendElement("div").attr("class", "item");
                    PlayerItem.appendElement("div").attr("class", "image").attr("style", "width:32px;height:32px;").appendElement("img").attr("src", "https://avatar.oc.tc/" + entry.getKey().getName() + "/32@2x.png").attr("alt", "Avatar " + entry.getKey().getName());
                    
                    Element PlayerItemContent = PlayerItem.appendElement("div").attr("class", "content");
                    PlayerItemContent.appendElement("div").attr("class", "header").text(entry.getKey().getName());
                    
                	if (!team.isObserver()) {
                		Element PlayerItemDescription = PlayerItemContent.appendElement("div").attr("class", "description");
                		PlayerItemDescription.appendElement("p").text("Kills: " + getKillsByPlayer(entry.getKey()));
                		PlayerItemDescription.appendElement("p").text("Deaths: " + getDeathsByPlayer(entry.getKey()));
                		PlayerItemDescription.appendElement("p").text("KD: " + (Math.round(getKdByPlayer(entry.getKey()) * 100.0) / 100.0));
                    }
                }
            }
        }
        Element transcript = document.getElementById("transcript");
        if (GameHandler.getGameHandler().getMatch().getModules().getModule(MatchTranscript.class).getLog() != null)
            transcript.appendElement("pre").text(GameHandler.getGameHandler().getMatch().getModules().getModule(MatchTranscript.class).getLog());
        Writer writer = new PrintWriter(file);
        writer.write(document.html());
        writer.close();
        return file;
    }

    public String uploadStats() {
        try {
            HttpPost post = new HttpPost("https://vps.alan736.ch/scrim/scrim.php");
            NameValuePair id = new BasicNameValuePair("id", GameHandler.getGameHandler().getMatch().getUuid().toString().replaceAll("-", ""));
            MultipartEntityBuilder fileBuilder = MultipartEntityBuilder.create().addBinaryBody("match", generateStats());
            fileBuilder.addPart(id.getName(), new StringBody(id.getValue(), ContentType.TEXT_HTML));
            post.setEntity(fileBuilder.build());
            HttpClient client = HttpClientBuilder.create().build();
            return EntityUtils.toString(client.execute(post).getEntity());
        } catch (Exception e) {
            Bukkit.getLogger().warning("Unable to upload statistics");
            e.printStackTrace();
            return null;
        }
    }
}
