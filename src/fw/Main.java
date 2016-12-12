package fw;

import api.API;
import api.utils.Config;
import fw.commands.CommandsManager;
import fw.config.ConfigData;
import fw.events.PlayerEvents;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Created by loucass003 on 25/11/16.
 */
public class Main extends JavaPlugin
{

    public static Main instance;

    public API api;
    public Config config;
    public ConfigData configData;
    public CommandsManager commandsManager;
    public PlayerEvents playerEvents;

    public Main()
    {
        instance = this;
        api = API.getInstance();
        api.setMaxPlayers(16);
        api.setMinPlayers(2);
        api.setCountdown(1000L * 15);
        config = new Config(this.getName(), this);
        config.setConfigObject(ConfigData.class);
        commandsManager = new CommandsManager(this);
    }

    @Override
    public void onEnable()
    {
        getServer().getLogger().info("Demarrage du plugin !");
        config.loadConfig();
        configData = config.get(ConfigData.class);
        api.useQueueManager(configData.minimumConfigIsSet());
        this.getServer().getPluginManager().registerEvents(playerEvents = new PlayerEvents(this), this);
        commandsManager.registerCommands();
        for(Player p : getServer().getOnlinePlayers())
            p.teleport(api.getGlobalConfig().getSpawn().getLocation());
        if(configData.minimumConfigIsSet() && api.getQueueManager() != null)
            api.getQueueManager().checkPlayers();
    }

    @Override
    public void onDisable()
    {
        getServer().getLogger().info("Arret du plugin !");
        config.save();
        playerEvents.clear();
    }
}
