package fw.events;

import api.config.ConfigLocation;
import api.gui.Title;
import api.interfaces.QueueListener;
import fw.Main;
import net.minecraft.server.v1_8_R1.EnumParticle;
import net.minecraft.server.v1_8_R1.PacketPlayOutWorldParticles;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_8_R1.entity.CraftPlayer;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * Created by loucass003 on 26/11/16.
 */
public class PlayerEvents implements Listener, QueueListener {

    public Main main;
    public Location firstPoint;
    public Location lastPoint;

    public List<Projectile> fireballs = new ArrayList<>();
    public Map<Player, Long> cooldown = new HashMap<>();

    public boolean gamePlaying = false;
    public boolean started = false;

    public int startTimerId = -1;
    public int startTimer = 10;

    public PlayerEvents(Main main) {
        this.main = main;
        if(this.main.api.getQueueManager() != null) {
            this.main.api.getQueueManager().registerListner(this);
            Bukkit.getScheduler().scheduleSyncRepeatingTask(Main.instance, () -> {
                for (Projectile f : fireballs) {
                    Location loc = f.getLocation();
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        ((CraftPlayer) p).getHandle().playerConnection.sendPacket(new PacketPlayOutWorldParticles(
                                EnumParticle.FLAME, false, (float) loc.getX(), (float) loc.getY(), (float) loc.getZ(),
                                0, 0, 0, 0.05F, 15));
                        ((CraftPlayer) p).getHandle().playerConnection.sendPacket(new PacketPlayOutWorldParticles(
                                EnumParticle.SMOKE_LARGE, false, (float) loc.getX(), (float) loc.getY(), (float) loc.getZ(),
                                0, 0, 0, 0.01F, 10));

                    }
                }
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (!cooldown.containsKey(p))
                        continue;
                    Long t = cooldown.get(p) - System.currentTimeMillis();
                    int seconds = (int) (t / 1000) % 60;
                    if (t < 0)
                        seconds = -1;
                    p.setLevel(seconds + 1);
                }
            }, 0, 1);
        }
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

        if(i.getType() == Material.FIREBALL && gamePlaying)
        {
            if(e.getAction() == Action.RIGHT_CLICK_BLOCK)
                e.setCancelled(true);
            Long t = 0L;
            if(!cooldown.containsKey(p))
                cooldown.put(p, System.currentTimeMillis() + 3000L);
            else
                t = cooldown.get(p);
            if(t - System.currentTimeMillis() <= 0) {
                Fireball b = p.launchProjectile(Fireball.class);
                b.setIsIncendiary(false);
                b.setYield(1.2F);
                p.playSound(p.getLocation(), Sound.GHAST_FIREBALL, 0.5F, 0.5F);
                cooldown.put(p, System.currentTimeMillis() + 3000L);
            }
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

            Inventory i = p.getInventory();
            i.clear();

            ItemStack is = new ItemStack(Material.FIREBALL);
            ItemMeta im = is.getItemMeta();
            im.setDisplayName(ChatColor.RED + "Boule de Feu");
            is.setItemMeta(im);

            ItemStack is2 = new ItemStack(Material.BLAZE_ROD);
            ItemMeta im2 = is2.getItemMeta();
            im2.setDisplayName(ChatColor.RED + "Bâton de Feu");
            is2.setItemMeta(im2);

            i.addItem(is, is2);
            p.setGameMode(GameMode.SURVIVAL);
        }
        started = true;
        this.startTimerId = Bukkit.getScheduler().scheduleSyncRepeatingTask(Main.instance, () -> {

                Title title = new Title();
                title.setFadeIn(2);
                title.setFadeOut(2);
                title.setText("Debut dans " + startTimer + "...");
                title.setColor(ChatColor.GOLD);
                if (startTimer < 10 && startTimer > 1) {
                    title.setFadeIn(0);
                }
                if (startTimer == 0)
                    title.setText("Go !");

                for (Player pl : Bukkit.getOnlinePlayers())
                    title.sendTitle(pl);

                if (startTimer <= 0) {
                    startTimer = 0;
                    gamePlaying = true;
                    Bukkit.getScheduler().cancelTask(startTimerId);
                }
                else
                    startTimer--;

        }, 0, 20);
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent e)
    {
        e.setCancelled(true);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e)
    {
        if(e.getPlayer().getGameMode() != GameMode.CREATIVE)
            e.setCancelled(true);
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent e)
    {
        e.setCancelled(true);
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent e)
    {
        if(e.getEntityType() != EntityType.FIREBALL)
            e.setCancelled(true);
        else {
            for (Block b : e.blockList()) {
                b.setType(Material.AIR);
                e.setYield(1.2F);
            }
        }
    }

    @EventHandler
    public void onPlayerMoveEvent(PlayerMoveEvent e)
    {
        if(started && !gamePlaying && e.getPlayer().getGameMode() == GameMode.SURVIVAL)
                e.setCancelled(true);
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e)
    {
        e.setDamage(0);
        if(e.getDamager().getType() == EntityType.PLAYER && gamePlaying)
        {
            Player a = (Player)e.getDamager();
            if(a.getItemInHand() == null)
                return;
            if(a.getItemInHand().getType() == Material.BLAZE_ROD)
            {
                if(e.getEntity().getType() == EntityType.PLAYER)
                {
                    Player en = (Player)e.getEntity();
                    Location l = en.getLocation().subtract(a.getLocation());
                    double distance = en.getLocation().distance(a.getLocation());
                    Vector v = l.toVector().multiply(2/distance);
                    en.setVelocity(v);
                }
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent e)
    {
        if (e.getCause() == EntityDamageEvent.DamageCause.FALL)
            e.setCancelled(true);
        else
            e.setDamage(0);
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent e)
    {
        if(e.getEntityType() == EntityType.FIREBALL) {
            e.getEntity().setVelocity(new Vector(0,0,0));
            this.fireballs.add(e.getEntity());
        }
    }
    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event)
    {
        if(event.getEntityType() == EntityType.FIREBALL)
        {
            Projectile p = event.getEntity();
            if(this.fireballs.contains(p))
                this.fireballs.remove(p);
        }

    }
}
