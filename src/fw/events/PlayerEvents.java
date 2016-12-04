package fw.events;

import api.API;
import api.config.ConfigLocation;
import api.gui.ScoreboardSign;
import api.gui.Title;
import api.interfaces.QueueListener;
import api.utils.Region;
import fw.Main;
import fw.config.ConfigPlatform;
import net.minecraft.server.v1_9_R1.EnumParticle;
import net.minecraft.server.v1_9_R1.PacketPlayOutWorldParticles;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_9_R1.entity.CraftPlayer;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by loucass003 on 26/11/16.
 */
public class PlayerEvents implements Listener, QueueListener
{

    public Main main;
    public Location firstPoint;
    public Location lastPoint;

    public List<Projectile> fireballs = new ArrayList<>();
    public Map<Player, Long> cooldown = new HashMap<>();
    public Map<Player, ScoreboardSign> scoreboards = new HashMap<>();

    public List<Player> winners = new ArrayList<>();

    public boolean gamePlaying = false;
    public boolean started = false;

    public int startTimerId = -1;
    public int startTimer = 10;

    public Long currentTime = 0L;
    public Long platformTime = 60L * 1000L;
    public int platformOffset = 0;
    public boolean lastPlatform = false;


    public PlayerEvents(Main main)
    {
        this.main = main;

        if(this.main.api.getQueueManager() != null)
        {
            this.main.api.getQueueManager().registerListner(this);
            Bukkit.getScheduler().scheduleSyncRepeatingTask(Main.instance, () ->
            {
                for (Projectile f : fireballs)
                {
                    Location loc = f.getLocation();
                    for (Player p : Bukkit.getOnlinePlayers())
                    {
                        ((CraftPlayer) p).getHandle().playerConnection.sendPacket(new PacketPlayOutWorldParticles(
                                EnumParticle.FLAME, false, (float) loc.getX(), (float) loc.getY(), (float) loc.getZ(),
                                0, 0, 0, 0.05F, 15));
                        ((CraftPlayer) p).getHandle().playerConnection.sendPacket(new PacketPlayOutWorldParticles(
                                EnumParticle.SMOKE_LARGE, false, (float) loc.getX(), (float) loc.getY(), (float) loc.getZ(),
                                0, 0, 0, 0.01F, 10));
                    }
                }

                for (Player p : Bukkit.getOnlinePlayers())
                {
                    if (!cooldown.containsKey(p))
                        continue;
                    Long t = cooldown.get(p) - System.currentTimeMillis();
                    int seconds = (int) (t / 1000) % 60;
                    if (t < 0)
                        seconds = -1;
                    p.setLevel(seconds + 1);
                }

                if(gamePlaying)
                {
                    Bukkit.getOnlinePlayers().forEach(this::updateScoreboard);
                    if(currentTime - System.currentTimeMillis() <= 0)
                    {
                        currentTime = System.currentTimeMillis() + platformTime;
                        if(platformOffset <= Main.instance.configData.platforms.size() - 1)
                        {
                            ConfigPlatform cp = Main.instance.configData.platforms.get(platformOffset);
                            if(cp == null)
                                return;
                            Region r = cp.getRegion();
                            for(Block b : r.getBlocks())
                                b.setType(Material.AIR);
                            platformOffset++;
                        }
                        else
                        {
                            if(lastPlatform)
                            {
                                currentTime = 0L;
                                winners.addAll(Bukkit.getOnlinePlayers().stream().filter(p -> p.getGameMode() == GameMode.SURVIVAL && !API.isSpectator(p)).collect(Collectors.toList()));
                                doWin();
                                return;
                            }
                            currentTime = System.currentTimeMillis() + 10 * 60 * 1000L;
                            lastPlatform = true;
                        }
                    }
                }
                else
                    currentTime = 0L;

            }, 0, 1);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e)
    {
        Player p = e.getPlayer();
        ItemStack i = e.getItem();
        if(i == null)
            return;
        if(i.getType() == Material.DIAMOND_SPADE && i.getItemMeta().getDisplayName().equals("Fw Tool"))
        {
            if(e.getAction() == Action.LEFT_CLICK_BLOCK)
            {
                firstPoint = e.getClickedBlock().getLocation();
                p.sendMessage("Point #1 set !");
            }
            else if(e.getAction() == Action.RIGHT_CLICK_BLOCK)
            {
                lastPoint = e.getClickedBlock().getLocation();
                p.sendMessage("Point #2 set !");
            }

            if(e.getAction() == Action.LEFT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_BLOCK)
                e.setCancelled(true);
        }

        if(i.getType() == Material.FIREBALL)
        {
            if(e.getAction() == Action.RIGHT_CLICK_BLOCK)
                e.setCancelled(true);
            if (gamePlaying && !API.isSpectator(p) && p.getGameMode() == GameMode.SURVIVAL)
            {
                Long t = 0L;
                if (!cooldown.containsKey(p))
                    cooldown.put(p, System.currentTimeMillis() + 3000L);
                else
                    t = cooldown.get(p);
                if (t - System.currentTimeMillis() <= 0) {
                    Fireball b = p.launchProjectile(Fireball.class);
                    b.setIsIncendiary(false);
                    b.setYield(1.2F);
                    p.playSound(p.getLocation(), Sound.ENTITY_ENDERDRAGON_FIREBALL_EXPLODE, 0.5F, 0.5F);
                    cooldown.put(p, System.currentTimeMillis() + 3000L);
                }
            }
        }
    }

    public ConfigLocation getRandomPos(List<ConfigLocation> used)
    {
        int r = new Random().nextInt(Main.instance.configData.gameSpawns.size() - 1);
        ConfigLocation cp = Main.instance.configData.gameSpawns.get(r);
        if(used.size() >= Main.instance.configData.gameSpawns.size())
            used.clear();
        if(cp != null && !used.contains(cp))
        {
            used.add(cp);
            return cp;
        }
        else
            return getRandomPos(used);
    }

    @Override
    public void onGameStart()
    {
        List<ConfigLocation> usedSpawn = new ArrayList<>();
        Bukkit.getWorlds().get(0).setTime(0);
        for(Player p : Bukkit.getOnlinePlayers())
        {
            ConfigLocation cp = getRandomPos(usedSpawn);
            p.teleport(cp.getLocation());
            usedSpawn.add(cp);
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

        this.startTimerId = Bukkit.getScheduler().scheduleSyncRepeatingTask(Main.instance, () ->
        {
            Title title = new Title();
            title.setFadeIn(2);
            title.setFadeOut(2);
            title.setText("Debut dans " + startTimer + "...");
            title.setColor(ChatColor.GOLD);
            if (startTimer < 10 && startTimer > 1)
                title.setFadeIn(0);
            if (startTimer == 0)
                title.setText("Go !");

            Bukkit.getOnlinePlayers().forEach(title::sendTitle);

            if (startTimer <= 0)
            {
                startTimer = 0;
                gamePlaying = true;
                Bukkit.getScheduler().cancelTask(startTimerId);
                currentTime = System.currentTimeMillis() + platformTime;
            }
            else
                startTimer--;

        }, 0, 20);
    }

    private void updateScoreboard(Player p)
    {
        ScoreboardSign s;
        if(scoreboards.containsKey(p))
            s = scoreboards.get(p);
        else
        {
            s = new ScoreboardSign(p, Main.instance.getName());
            s.create();
            scoreboards.put(p, s);
        }

        Long t = currentTime - System.currentTimeMillis();
        int seconds = (int) (t / 1000) % 60 ;
        int minutes = (int) ((t / (1000*60)) % 60);
        String time = String.format("%02d:%02d", minutes, seconds);
        s.setLine(1, time);
    }

    public void clear()
    {
        scoreboards.values().forEach(ScoreboardSign::destroy);
        scoreboards.clear();
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
        if(e.getPlayer().getGameMode() != GameMode.CREATIVE)
            e.setCancelled(true);
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent e)
    {
        if(e.getEntityType() != EntityType.FIREBALL)
            e.setCancelled(true);
        else
        {
            for (Block b : e.blockList())
            {
                b.setType(Material.AIR);
                e.setYield(1.2F);
            }
        }
    }

    @EventHandler
    public void onPlayerMoveEvent(PlayerMoveEvent e)
    {
        Player p = e.getPlayer();
        if(started && p.getGameMode() == GameMode.SURVIVAL && !API.isSpectator(p))
        {
            if(!gamePlaying)
            {
                boolean teleport = e.getFrom().toVector().getBlockX() != e.getTo().toVector().getBlockX();
                if (e.getFrom().toVector().getBlockZ() != e.getTo().toVector().getBlockZ() || teleport)
                        p.teleport(e.getFrom());
            }
            else
            {
                if(p.getLocation().getY() <= main.configData.deathLevel)
                {
                    int c = 0;
                    for(Player pl : Bukkit.getOnlinePlayers())
                        if(pl.getGameMode() == GameMode.SURVIVAL && !API.isSpectator(pl))
                            c++;
                    if(c == 1)
                    {
                        winners.add(p);
                        doWin();
                    }
                    else
                    {
                        API.setSpectator(p);
                        p.getInventory().clear();
                        p.teleport(main.configData.spectatorLoc.getLocation());
                    }
                }
            }
        }
    }

    public void doWin()
    {

    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e)
    {
        e.getPlayer().getInventory().clear();
        if(started && gamePlaying)
        {
            Player p = e.getPlayer();
            API.setSpectator(p);
            p.getInventory().clear();
            p.teleport(main.configData.spectatorLoc.getLocation());
        }
        else
            e.getPlayer().setGameMode(GameMode.SURVIVAL);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e)
    {
        if(scoreboards.containsKey(e.getPlayer()))
            scoreboards.remove(e.getPlayer());
        int c = 0;
        for(Player pl : Bukkit.getOnlinePlayers())
            if(pl.getGameMode() == GameMode.SURVIVAL && !API.isSpectator(pl))
                c++;
        if(c == 1)
        {
            for(Player pl : Bukkit.getOnlinePlayers())
            {
                if (pl.getGameMode() == GameMode.SURVIVAL && !API.isSpectator(pl))
                {
                    winners.add(pl);
                    doWin();
                    return;
                }
            }
        }
        else if (Bukkit.getOnlinePlayers().size() == 0)
            clear();
    }

    @EventHandler (priority = EventPriority.LOW)
    public void onWeatherChange(WeatherChangeEvent event)
    {
        if (event.toWeatherState())
            event.setCancelled(true);
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e)
    {
        if(!(e.getDamager() instanceof Player))
        {
            e.setCancelled(true);
            return;
        }

        Player damager = (Player)e.getDamager();
        if(!gamePlaying || API.isSpectator(damager))
        {
            e.setCancelled(true);
            return;
        }

        e.setDamage(0);
        if(damager.getGameMode() == GameMode.SURVIVAL)
        {
            Player a = (Player)e.getDamager();
            ItemStack is = a.getInventory().getItemInMainHand();
            if(is == null)
                return;
            if(is.getType() == Material.BLAZE_ROD)
            {
                if(e.getEntity() instanceof Player)
                {
                    Player entity = (Player) e.getEntity();
                    if(API.isSpectator(entity))
                       return;
                    double power = 2;
                    Player en = (Player)e.getEntity();
                    Location l = en.getLocation().subtract(a.getLocation());
                    Vector v = l.toVector().normalize().multiply(power);
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
        if(e.getEntityType() == EntityType.FIREBALL)
            this.fireballs.add(e.getEntity());
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
