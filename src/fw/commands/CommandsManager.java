package fw.commands;

import fw.Main;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.craftbukkit.v1_8_R1.CraftServer;

/**
 * Created by loucass003 on 26/11/16.
 */
public class CommandsManager {

    public Main main;

    public CommandsManager(Main main)
    {
        this.main = main;
    }

    public void registerCommands()
    {
        this.addCommand("fw", new Fw());
    }

    public void addCommand(String name, BukkitCommand c)
    {
        ((CraftServer)main.getServer()).getCommandMap().register(name, c);
    }

}
