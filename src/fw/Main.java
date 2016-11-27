package fw;

import api.API;
import api.utils.Config;
import fw.commands.CommandsManager;
import fw.config.ConfigData;
import fw.events.PlayerEvents;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Created by loucass003 on 25/11/16.
 */
public class Main extends JavaPlugin {

    public static Main instance;

    public API api;
    public Config config;
    public ConfigData configData;
    public CommandsManager commandsManager;
    public PlayerEvents playerEvents;

    public Main()
    {
        instance = this;
        api = new API(this);
        api.setMaxPlayers(5);
        api.setMinPlayers(1);
        api.setCountdown(5000L);
        config = new Config(this.getName());
        config.setConfigObject(ConfigData.class);
        commandsManager = new CommandsManager(this);
    }

    @Override
    public void onEnable()
    {
        getServer().getLogger().info("Demarrage du plugin !");
        config.loadConfig();
        configData = config.get(ConfigData.class);
        if(!configData.minimumConfigIsSet())
            api.useQueueManager(false);
        api.init();
        this.getServer().getPluginManager().registerEvents(playerEvents = new PlayerEvents(this), this);
        commandsManager.registerCommands();
    }

    @Override
    public void onDisable()
    {
        getServer().getLogger().info("Arret du plugin !");
        api.unload();
        config.save();
    }
}
