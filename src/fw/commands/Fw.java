package fw.commands;

import api.config.ConfigLocation;
import api.enchant.EmptyEnchant;
import api.utils.Utils;
import fw.Main;
import fw.config.ConfigPlatform;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;

/**
 * Created by loucass003 on 26/11/16.
 */
public class Fw extends BukkitCommand {

    public Fw() {
        super("fw");
        this.description = "Main command of Firewars";
        this.usageMessage = "/fw [spawn,select,platform]";
        this.setPermission("futurycraft.fw");
        this.setAliases(new ArrayList<>());
    }

    @Override
    public boolean execute(CommandSender cs, String s, String[] args) {

        if(!(cs instanceof Player))
        {
            cs.sendMessage(ChatColor.RED + "The command sender must be a player");
            return false;
        }

        Player sender = (Player)cs;

        if(args.length < 1) {
            sender.sendMessage(ChatColor.RED + this.getUsage());
            return false;
        }
        String command = args[0];
        String[] arguments = new String[args.length-1];
        for(int i = 1; i < args.length; i++)
            arguments[i - 1] = args[i];
        switch (command)
        {
            case "spawn":
                runSpawn(sender, arguments);
                break;
            case "select":
                runSelect(sender, arguments);
                break;
            case "platform":
                runPlatform(sender, arguments);
                break;
            default:
                sender.sendMessage(ChatColor.RED + this.getUsage());
        }

        return true;
    }

    public void runSpawn(Player sender, String[] args)
    {
        if(args.length < 1)
        {
            sender.sendMessage(ChatColor.RED + "/fw spawn [add,rem,list]");
            return;
        }

        String cmd = args[0];
        String[] arguments = new String[args.length-1];
        for(int i = 1; i < args.length; i++)
            arguments[i - 1] = args[i];
        switch (cmd)
        {
            case "add":
                Location pos = sender.getLocation();
                ConfigLocation cl = new ConfigLocation(pos.getWorld().getName(), pos.toVector(), pos.getDirection());
                Main.instance.configData.gameSpawns.add(cl);
                sender.sendMessage(ChatColor.BLUE + "Le spawn a bien été ajouté");
                Main.instance.config.save();
                break;
            case "list":
                sender.sendMessage(ChatColor.BLUE + "--------------------------------");
                int i = 0;
                for(ConfigLocation c : Main.instance.configData.gameSpawns) {
                    int x = (int)c.pos.getX();
                    int y = (int)c.pos.getY();
                    int z = (int)c.pos.getZ();
                    sender.sendMessage(ChatColor.AQUA + "#" + (++i) + ChatColor.WHITE + " - " + ChatColor.RED + x + "/" + y + "/" + z);
                }
                sender.sendMessage(ChatColor.BLUE + "--------------------------------");
                break;
            case "rem":
                if(arguments.length < 1) {
                    sender.sendMessage(ChatColor.RED + "/fw spawn rem <id>");
                    return;
                }

                int id = -1;
                if(Utils.isNumeric(arguments[0]) != null)
                    id = Integer.parseInt(arguments[0]);
                if(id == -1)
                {
                    sender.sendMessage(ChatColor.RED + "l'id doit etre un nombre !");
                    return;
                }

                id = id - 1;

                ConfigLocation c = Main.instance.configData.gameSpawns.get(id);
                if(c == null)
                {
                    sender.sendMessage(ChatColor.RED + "Spawn non trouvé");
                    return;
                }

                Main.instance.configData.gameSpawns.remove(id);
                sender.sendMessage(ChatColor.AQUA + "le spawn a bien été supprimé");
                Main.instance.config.save();
                break;
            default:
                sender.sendMessage(ChatColor.RED + "/fw spawn [add,rem,list]");
        }
    }

    void runSelect(Player sender, String[] args)
    {
        if(args.length != 0)
        {
            sender.sendMessage(ChatColor.RED + "/fw select");
            return;
        }
        Inventory i = sender.getInventory();

        boolean hasTool = false;
        for(ItemStack is : i.getContents())
        {
            if (is != null && is.getType() == Material.DIAMOND_SPADE && is.getItemMeta().getDisplayName().equals("Fw Tool")) {
                hasTool = true;
                break;
            }
        }

        if(!hasTool)
        {
            ItemStack tool = new ItemStack(Material.DIAMOND_SPADE);
            ItemMeta im = tool.getItemMeta();
            im.setDisplayName("Fw Tool");
            im.addEnchant(new EmptyEnchant(tool.hashCode()), 1, true);
            tool.setItemMeta(im);
            i.addItem(tool);
        }
    }

    void runPlatform(Player sender, String[] args)
    {
        if(args.length < 1)
        {
            sender.sendMessage(ChatColor.RED + "/fw platform [add,rem,list]");
            return;
        }

        String cmd = args[0];
        String[] arguments = new String[args.length-1];
        for(int i = 1; i < args.length; i++)
            arguments[i - 1] = args[i];
        switch (cmd)
        {
            case "add":
                Location firstpos = Main.instance.playerEvents.firstPoint;
                Location lastpos = Main.instance.playerEvents.lastPoint;
                if(firstpos == null || lastpos == null)
                {
                    sender.sendMessage(ChatColor.RED + "Les points 1 et 2 doivent etre définis");
                    return;
                }
                ConfigPlatform cp = new ConfigPlatform();
                cp.firstPoint = new ConfigLocation(firstpos.getWorld().getName(), firstpos.toVector(), null);
                cp.lastPoint = new ConfigLocation(lastpos.getWorld().getName(), lastpos.toVector(), null);
                Main.instance.configData.platforms.add(cp);
                sender.sendMessage(ChatColor.BLUE + "La plateforme a bien été ajoutée");
                Main.instance.config.save();
                break;
            case "list":
                sender.sendMessage(ChatColor.BLUE + "--------------------------------");
                int i = 0;
                for(ConfigPlatform p : Main.instance.configData.platforms)
                {
                    int x = (int)p.firstPoint.pos.getX();
                    int y = (int)p.firstPoint.pos.getY();
                    int z = (int)p.firstPoint.pos.getZ();
                    int x2 = (int)p.lastPoint.pos.getX();
                    int y2 = (int)p.lastPoint.pos.getY();
                    int z2 = (int)p.lastPoint.pos.getZ();
                    String first = "" + ChatColor.RED + x + "/" + y + "/" + z;
                    String last = "" + ChatColor.RED + x2 + "/" + y2 + "/" + z2;
                    sender.sendMessage(ChatColor.AQUA + "#" + (++i) + ChatColor.WHITE + " - " + first + ChatColor.WHITE + " - " + last);
                }
                sender.sendMessage(ChatColor.BLUE + "--------------------------------");
                break;
            case "rem":
                if(arguments.length < 1)
                {
                    sender.sendMessage(ChatColor.RED + "/fw platform rem <id>");
                    return;
                }

                int id = -1;
                if(Utils.isNumeric(arguments[0]) != null)
                    id = Integer.parseInt(arguments[0]);
                if(id == -1)
                {
                    sender.sendMessage(ChatColor.RED + "l'id doit etre un nombre !");
                    return;
                }

                id = id - 1;

                ConfigPlatform p = Main.instance.configData.platforms.get(id);
                if(p == null)
                {
                    sender.sendMessage(ChatColor.RED + "Plateforme non trouvée");
                    return;
                }

                Main.instance.configData.platforms.remove(id);
                sender.sendMessage(ChatColor.AQUA + "la plateforme a bien été supprimée");
                Main.instance.config.save();
                break;
            default:
                sender.sendMessage(ChatColor.RED + "/fw platform [add,rem,list]");
        }
    }

}
