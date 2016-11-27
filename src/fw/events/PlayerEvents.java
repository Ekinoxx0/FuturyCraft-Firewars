package fw.events;

import api.config.ConfigLocation;
import api.interfaces.QueueListener;
import fw.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Created by loucass003 on 26/11/16.
 */
public class PlayerEvents implements Listener, QueueListener {

    public Main main;
    public Location firstPoint;
    public Location lastPoint;



    public PlayerEvents(Main main) {
        this.main = main;
        if(this.main.api.getQueueManager() != null)
            this.main.api.getQueueManager().registerListner(this);
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEvent e)
    {
        Player p = e.getPlayer();
        ItemStack i = e.getItem();
        if(i == null)
            return;
        if(i.getType() == Material.DIAMOND_SPADE && i.getItemMeta().getDisplayName().equals("Fw Tool"))
        {
            if(e.getAction() == Action.LEFT_CLICK_BLOCK) {
                firstPoint = e.getClickedBlock().getLocation();
                p.sendMessage("Point #1 set !");
            } else if(e.getAction() == Action.RIGHT_CLICK_BLOCK) {
                lastPoint = e.getClickedBlock().getLocation();
                p.sendMessage("Point #2 set !");
            }

            if(e.getAction() == Action.LEFT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_BLOCK)
                e.setCancelled(true);
        }
    }

    @Override
    public void onGameStart() {
        int count = 0;
        for(Player p : Bukkit.getOnlinePlayers())
        {
            if(count >= Main.instance.configData.gameSpawns.size())
                count = 0;
            ConfigLocation cp = Main.instance.configData.gameSpawns.get(count);
            if(cp != null)
                p.teleport(cp.getLocation());
            count++;
        }
    }
}
