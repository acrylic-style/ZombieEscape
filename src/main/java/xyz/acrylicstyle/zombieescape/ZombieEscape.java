package xyz.acrylicstyle.zombieescape;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import de.robingrether.idisguise.api.DisguiseAPI;
import de.robingrether.idisguise.disguise.DisguiseType;
import de.robingrether.idisguise.disguise.ZombieVillagerDisguise;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import util.Collection;
import util.ICollectionList;
import xyz.acrylicstyle.tomeito_api.TomeitoAPI;
import xyz.acrylicstyle.tomeito_api.providers.ConfigProvider;
import xyz.acrylicstyle.tomeito_api.providers.LanguageProvider;
import xyz.acrylicstyle.tomeito_api.utils.Lang;
import xyz.acrylicstyle.tomeito_api.utils.Log;
import xyz.acrylicstyle.zombieescape.commands.ZombieEscapeCommand;
import xyz.acrylicstyle.zombieescape.commands.ZombieEscapeConfig;
import xyz.acrylicstyle.zombieescape.commands.ZombieEscapeGameUtil;
import xyz.acrylicstyle.zombieescape.commands.ZombieEscapeGameUtil.VoteGui;
import xyz.acrylicstyle.zombieescape.data.Constants;
import xyz.acrylicstyle.zombieescape.utils.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

public class ZombieEscape extends JavaPlugin implements Listener {
    public static ConfigProvider finalMapConfig = null;
    public static ConfigProvider config = null;
    public static ConfigProvider mapConfig = null;
    public static HashMap<UUID, Scoreboard> hashMapScoreboard = new HashMap<>();
    public static Collection<UUID, PlayerTeam> hashMapTeam = new Collection<>();
    public static HashMap<UUID, String> hashMapLastScore4 = new HashMap<>();
    public static HashMap<UUID, String> hashMapLastScore8 = new HashMap<>();
    public static HashMap<String, Integer> hashMapBlockState = new HashMap<>();
    public static HashMap<UUID, Boolean> lockActionBar = new HashMap<>();
    /**
     * Not in use.
     */
    public static HashMap<UUID, Boolean> hashMapOriginZombie = new HashMap<>();
    /**
     * Player, Map name
     */
    public static HashMap<UUID, String> hashMapVote = new HashMap<>();
    public static HashMap<String, Integer> votes = new HashMap<>();
    public static HashMap<UUID, Boolean> respawnWait = new HashMap<>();
    public static Map<String, Object> locationWall = null;
    public static List<String> previousZombies = null;
    public static List<String> listZombies = new ArrayList<>();
    public static String mapName = null;
    public static ScoreboardManager manager = null;
    public static ProtocolManager protocol = null;
    public static volatile int zombies = 0;
    public static volatile int players = 0;
    public static int timesLeft = 18000;
    public static int gameTime = 1800; // 30 minutes
    public static int playedTime = 0;
    public static int zombieCheckpoint = 0;
    public static int playerCheckpoint = 0;
    public static int maxCheckpoints = 0;
    public static int fireworked = 0;
    public static int mostVotes = 0;
    public static boolean timerStarted = false;
    public static boolean hasEnoughPlayers = false;
    public static boolean settingsCheck = false;
    public static boolean gameStarted = false;
    public static boolean debug = false;
    public static boolean playersReset = false;
    public static boolean gameEnded = false;
    public static boolean once = false;
    public static boolean init = false;
    public static String ongoingEvent = null;
    public static String mostVotedMap = null;
    public static String defmapString = null;
    public static Map<String, String> ongoingEventMap = new HashMap<>();
    public static Lang language = null;
    public static LanguageProvider lang = null;
    public static DisguiseAPI disguise = null;

    private boolean error = false;

    @Override
    public void onLoad() {
        Logger logger = Bukkit.getLogger();
        if (!Bukkit.getBukkitVersion().contains(Constants.requiredMinecraftVersion)) {
            logger.severe("Your current bukkit/minecraft version(" + Bukkit.getBukkitVersion() + ") is incompatible.");
            logger.severe("Please use spigot 1.12.2 and restart your server.");
            this.error = true;
        }
    }

    @Override
    public void onEnable() {
        new BukkitRunnable() {
            public void run() {
                if (Utils.checkPlugin("CrackShot")) Bukkit.getLogger().warning("[ZombieEscape] Does not exist CrackShot plugin.");
                if (Utils.checkPlugin("Multiverse-Core")) Bukkit.getLogger().warning("[ZombieEscape] Does not exist Multiverse-Core plugin.");
                if (getInstance().error) {
                    Bukkit.getLogger().severe("[ZombieEscape] There was errors when loading plugin.");
                    Bukkit.getPluginManager().disablePlugin(getInstance());
                    return;
                }
                protocol = ProtocolLibrary.getProtocolManager();
                config = new ConfigProvider("./plugins/ZombieEscape/config.yml");
                mapName = config.getString("map", "world");
                mapConfig = new ConfigProvider("./plugins/ZombieEscape/maps/" + mapName + ".yml");
                finalMapConfig = new ConfigProvider("./plugins/ZombieEscape/maps/" + mapName + ".yml");
                debug = config.getBoolean("debug", false);
                Lang.saveResource(getInstance(), "language_ja_JP.yml");
                Lang.saveResource(getInstance(), "language_en_US.yml");
                language = new Lang("ZombieEscape");
                language.addLanguage("ja_JP");
                language.addLanguage("en_US");
                lang = language.get(config.getString("language", "en_US"));
                defmapString = "    " + Lang.format(lang.get("defaultMap"), ChatColor.translateAlternateColorCodes('&', mapConfig.getString("mapname", "???")));
                if (Bukkit.getWorld(mapConfig.getString("spawnPoints.world", "world")) == null) {
                    Log.severe("Failed to load world(probably does not exist), disabling plugin.");
                    Log.severe("Tried to load world: " + mapConfig.getString("spawnPoints.world", "world"));
                    Bukkit.getPluginManager().disablePlugin(getInstance());
                    return;
                }
                manager = Bukkit.getScoreboardManager();
                disguise = Bukkit.getServicesManager().getRegistration(DisguiseAPI.class).getProvider();
                ZombieEscapeConfig zec;
                ZombieEscapeGameUtil zegu;
                try {
                    zec = new ZombieEscapeConfig();
                    zegu = new ZombieEscapeGameUtil();
                } catch (Exception e) {
                    Log.error("Failed to initialize commands! Showing errors below and disabling plugin.");
                    e.printStackTrace();
                    e.getCause().printStackTrace();
                    Bukkit.getPluginManager().disablePlugin(getInstance());
                    return;
                }
                VoteGui votegui = zegu.new VoteGui();
                votegui.initialize();
                Bukkit.getPluginCommand("setspawn").setExecutor(zec.new SetSpawn());
                Bukkit.getPluginCommand("removespawn").setExecutor(zec.new RemoveSpawn());
                Bukkit.getPluginCommand("addwall").setExecutor(zec.new AddWall());
                Bukkit.getPluginCommand("deletewall").setExecutor(zec.new DeleteWall());
                Bukkit.getPluginCommand("setmapname").setExecutor(zec.new SetMapName());
                Bukkit.getPluginCommand("setmap").setExecutor(zec.new SetMap());
                Bukkit.getPluginCommand("setcp").setExecutor(zegu.new SetCheckpoint());
                Bukkit.getPluginCommand("startgame").setExecutor(zegu.new StartGame());
                Bukkit.getPluginCommand("endgame").setExecutor(new EndGame());
                Bukkit.getPluginCommand("vote").setExecutor(zegu.new Vote());
                Bukkit.getPluginCommand("votemap").setExecutor(votegui);
                Bukkit.getPluginCommand("destroywall").setExecutor(zegu.new DestroyWall());
                Bukkit.getPluginCommand("zombieescape").setExecutor(new ZombieEscapeCommand());
                Bukkit.getPluginCommand("resourcepack").setExecutor(zegu.new ResourcePack());
                Bukkit.getPluginCommand("crash").setExecutor((sender, command, label, args) -> {
                    Log.warn(sender.getName() + " requested to crash itself, disabling plugin.");
                    Bukkit.getPluginManager().disablePlugin(getInstance());
                    return true;
                });
                Bukkit.getPluginManager().registerEvents(getInstance(), getInstance());
                Bukkit.getPluginManager().registerEvents(votegui, getInstance());
                Utils.checkConfig();
                maxCheckpoints = Math.min(mapConfig.getStringList("spawnPoints.player").size(), mapConfig.getStringList("spawnPoints.zombie").size());
                locationWall = ConfigProvider.getConfigSectionValue(mapConfig.get("locationWall", new HashMap<String, Object>()), true);
                List<?> list = config.getList("previousZombies") != null ? config.getList("previousZombies") : new ArrayList<String>();
                previousZombies = ICollectionList.asList(list).map(o -> (String) o);
                Constants.mininumPlayers = config.getInt("mininumPlayers", 2);
                init = true;
                Log.info("Enabled Zombie Escape");
            }
        }.runTaskLater(this, 1);
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent e){
        List<String> cmds = config.getStringList("disabledcommands");
        cmds.forEach(cmd -> {
            if (e.getMessage().startsWith(cmd) || e.getMessage().startsWith("/" + cmd)) {
                e.getPlayer().sendMessage(lang.get("disabledCommand"));
                e.setCancelled(true);
            }
        });
    }

    public ZombieEscape getInstance() {
        return this;
    }

    private int count = 0;

    @EventHandler
    public synchronized void onPlayerJoin(final PlayerJoinEvent event) {
        long time = System.currentTimeMillis();
        if (!gameStarted && timesLeft >= 16) players = players + 1;
        World world = Bukkit.getWorld(mapConfig.getString("spawnPoints.world", "world"));
        world.setGameRuleValue("announceAdvancements", "false");
        hashMapTeam.put(event.getPlayer().getUniqueId(), PlayerTeam.PLAYER);
        lockActionBar.put(event.getPlayer().getUniqueId(), false);
        disguise.undisguise(event.getPlayer());
        new BukkitRunnable() {
            public void run() {
                long time = System.currentTimeMillis();
                if (mapConfig.getBoolean("worldborder.enable", false)) Utils.damageIfOutsideOfBorder();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    Set<Material> set = new HashSet<>();
                    set.add(Material.AIR);
                    Block block = player.getTargetBlock(set, 15);
                    if (block == null) {
                        lockActionBar.put(player.getUniqueId(), false);
                        continue;
                    }
                    String location = block.getLocation().getBlockX() + "," + block.getLocation().getBlockY() + "," + block.getLocation().getBlockZ();
                    String wall = (String) locationWall.getOrDefault(location, null);
                    if (wall == null) {
                        lockActionBar.put(player.getUniqueId(), false);
                        continue;
                    }
                    int state = hashMapBlockState.get(wall) != null ? hashMapBlockState.get(wall) : 0;
                    int durability = (int) Math.nextUp(Math.min(Constants.materialDurability.getOrDefault(block.getType(), 5)*((double)players/(double)5), 3000));
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(lang.get("wallDurability") + state + "/" + durability + (ongoingEvent == null ? "" : ChatColor.GREEN + " | " + ChatColor.AQUA + ongoingEvent)));
                    lockActionBar.put(player.getUniqueId(), true);
                }
                if (debug) {
                    long end = System.currentTimeMillis()-time;
                    Log.debug("20 ticks task took " + end + "ms");
                }
            }
        }.runTaskTimer(this, 0, 20);
        final Scoreboard board = manager.getNewScoreboard();
        final Objective hpobjective = board.registerNewObjective("hpdisplay", "dummy");
        hpobjective.setDisplayName("" + ChatColor.RED + Constants.heart);
        Score hp = hpobjective.getScore(event.getPlayer().getName());
        hp.setScore((int) event.getPlayer().getHealth());
        hpobjective.setDisplaySlot(DisplaySlot.BELOW_NAME);
        BukkitRunnable healthBar = new BukkitRunnable() {
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (lockActionBar.getOrDefault(player.getUniqueId(), false)) continue;
                    int maxHealth = (int) player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();
                    int health = (int) player.getHealth();
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText("" + ChatColor.RED + health + "/" + maxHealth + "❤" + (ongoingEvent == null ? "" : ChatColor.GREEN + " | " + ChatColor.AQUA + ongoingEvent)));
                }
            }
        };
        new BukkitRunnable() {
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.isDead()) player.spigot().respawn();
                    if (player.getGameMode() == GameMode.SURVIVAL) player.setGameMode(GameMode.ADVENTURE);
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 100000, 1, false, false));
                    if (hashMapTeam.get(player.getUniqueId()) == PlayerTeam.ZOMBIE) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100000, 0, false, false));
                        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 100000, 0, false, false));
                        player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 100000, 100, false, false));
                        player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 100000, 1, false, false));
                    } else {
                        player.getInventory().setHelmet(new ItemStack(Material.DIAMOND_HELMET));
                        player.getInventory().setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
                        player.getInventory().setLeggings(new ItemStack(Material.DIAMOND_LEGGINGS));
                        player.getInventory().setBoots(new ItemStack(Material.DIAMOND_BOOTS));
                    }
                }
            }
        }.runTaskTimer(this, 200, 200);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "effect " + event.getPlayer().getName() + " clear");
        hashMapOriginZombie.put(event.getPlayer().getUniqueId(), false);
        if (!settingsCheck) {
            event.getPlayer().sendMessage(lang.get("incompleteSettings"));
            event.getPlayer().sendMessage(lang.get("incompleteSettings2"));
        }
        hashMapLastScore4.put(event.getPlayer().getUniqueId(), "");
        hashMapLastScore8.put(event.getPlayer().getUniqueId(), "");
        event.getPlayer().getInventory().clear();
        event.getPlayer().setGameMode(GameMode.ADVENTURE);
        event.getPlayer().setScoreboard(board);
        final Objective objective = board.registerNewObjective("scoreboard", "dummy");
        Score score7 = objective.getScore(" ");
        score7.setScore(7);
        Score score5 = objective.getScore("  ");
        score5.setScore(5);
        Score score3 = objective.getScore("   ");
        score3.setScore(3);
        Score score2 = objective.getScore(defmapString);
        score2.setScore(2);
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.setDisplayName(""+ChatColor.GREEN + ChatColor.BOLD + "Zombie Escape");
        if (Bukkit.getOnlinePlayers().size() >= Constants.mininumPlayers) hasEnoughPlayers = true; else {
            hasEnoughPlayers = false;
            timesLeft = 180;
        }
        hashMapScoreboard.put(event.getPlayer().getUniqueId(), board);
        event.getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(100);
        event.getPlayer().setHealth(100);
        event.getPlayer().setHealthScale(20);
        new BukkitRunnable() {
            @SuppressWarnings("NonAtomicOperationOnVolatileField")
            public void run() {
                event.getPlayer().getInventory().clear();
                event.getPlayer().teleport(world.getSpawnLocation());
                event.getPlayer().setGameMode(GameMode.ADVENTURE);
                event.getPlayer().getInventory().addItem(Utils.generateVoteItem());
                event.getPlayer().getInventory().addItem(Utils.generateResourcePackItem());
                event.getPlayer().sendMessage(ChatColor.BLUE + "--------------------------------------------------");
                event.getPlayer().sendMessage(ChatColor.DARK_GREEN + "          - Zombie Escape " + ChatColor.GRAY + "(" + Constants.version + ")" + ChatColor.DARK_GREEN + " -");
                event.getPlayer().sendMessage("");
                event.getPlayer().sendMessage(lang.get("intro1"));
                event.getPlayer().sendMessage(lang.get("intro2"));
                event.getPlayer().sendMessage("");
                event.getPlayer().sendMessage(lang.get("intro3"));
                event.getPlayer().sendMessage(lang.get("intro4"));
                event.getPlayer().sendMessage(lang.get("intro5"));
                event.getPlayer().sendMessage(lang.get("intro6"));
                event.getPlayer().sendMessage(ChatColor.BLUE + "--------------------------------------------------");
                if (gameStarted) {
                    Player player = event.getPlayer();
                    Bukkit.broadcastMessage(Lang.format(lang.get("becameZombieNaturally"), player.getName()));
                    zombies = zombies + 1;
                    player.getInventory().clear();
                    hashMapTeam.remove(player.getUniqueId());
                    hashMapTeam.put(player.getUniqueId(), PlayerTeam.ZOMBIE);
                    final Objective objective = hashMapScoreboard.get(player.getUniqueId()).getObjective("scoreboard");
                    Score score6 = objective.getScore("    " + ChatColor.GREEN + lang.get("team") + ": " + ChatColor.DARK_GREEN + lang.get("zombie"));
                    objective.getScoreboard().resetScores("    " + ChatColor.GREEN + lang.get("team") + ": " + ChatColor.AQUA + lang.get("player"));
                    score6.setScore(6);
                    String[] spawnLists = ICollectionList.asList(mapConfig.getList("spawnPoints.zombie", new ArrayList<String>())).map(o -> (String) o).get(zombieCheckpoint).split(",");
                    Location location = new Location(Bukkit.getWorld(mapConfig.getString("spawnPoints.world")), Double.parseDouble(spawnLists[0]), Double.parseDouble(spawnLists[1]), Double.parseDouble(spawnLists[2]));
                    player.teleport(location);
                    player.setGameMode(GameMode.ADVENTURE);
                    player.setPlayerListName(ChatColor.DARK_GREEN + player.getName());
                    player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(150);
                    player.setHealth(150);
                    player.setHealthScale(40);
                    new BukkitRunnable() {
                        public void run() {
                            player.addPotionEffect(PotionEffectType.HUNGER.createEffect(100000, 0));
                            player.addPotionEffect(PotionEffectType.SATURATION.createEffect(100000, 1));
                            player.addPotionEffect(PotionEffectType.INCREASE_DAMAGE.createEffect(100000, 100));
                            player.addPotionEffect(PotionEffectType.SPEED.createEffect(100000, 0));
                            player.addPotionEffect(PotionEffectType.JUMP.createEffect(100000, 0));
                            ItemStack item = new ItemStack(Material.IRON_SWORD);
                            ItemMeta meta = item.getItemMeta();
                            meta.setDisplayName("" + ChatColor.RESET + ChatColor.WHITE + lang.get("knife"));
                            item.setItemMeta(meta);
                            item.addUnsafeEnchantment(Enchantment.DURABILITY, 100);
                            item.addUnsafeEnchantment(Enchantment.DAMAGE_ALL, 100);
                            player.getInventory().setItem(0, item);
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "give " + player.getName() + " minecraft:stone_axe 1 0 {CanDestroy:[\"minecraft:planks\"],HideFlags:1,Unbreakable:1,display:{Name:\"" + lang.get("rustedAxe") + "\"},ench:[{id:32,lvl:10}]}");
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "give " + player.getName() + " minecraft:stone_pickaxe 1 0 {CanDestroy:[\"minecraft:gold_block\",\"minecraft:cobblestone\"],HideFlags:1,Unbreakable:1,display:{Name:\"" + lang.get("rustedPickaxe") + "\"},ench:[{id:32,lvl:10}]}");
                            ZombieVillagerDisguise d = (ZombieVillagerDisguise) DisguiseType.ZOMBIE_VILLAGER.newInstance();
                            d.setAdult(true);
                            d.setCustomNameVisible(true);
                            d.setCustomName(player.getName());
                            disguise.disguise(player, d);
                        }
                    }.runTaskLater(getInstance(), 40);
                } else { // timesLeft < 16
                    event.getPlayer().getWorld().getWorldBorder().setSize(mapConfig.getInt("worldborder.initsize", 29999999)); // set border to max
                    event.getPlayer().getWorld().getWorldBorder().setDamageAmount(0);
                }
            }
        }.runTaskLater(this, 40);
        healthBar.runTaskTimer(this, 0, 20);
        for (final Player player : Bukkit.getOnlinePlayers()) {
            for (final Player other : Bukkit.getOnlinePlayers()) {
                player.hidePlayer(this, other);
                player.showPlayer(this, other);
            }
        }
        if (debug) {
            long end = System.currentTimeMillis() - time;
            Log.debug("onPlayerJoin() took " + end + "ms");
        }
        if (timerStarted) return;
        timerStarted = true;
        new BukkitRunnable() {
            @SuppressWarnings("SuspiciousToArrayCall")
            public synchronized void run() {
                long time = System.currentTimeMillis();
                for (final Player player : Bukkit.getOnlinePlayers()) {
                    hashMapScoreboard.get(player.getUniqueId()).resetScores(hashMapLastScore4.get(player.getUniqueId()));
                }
                final String zombieMessage = ChatColor.GREEN + "    " + lang.get("team") + ": " + ChatColor.DARK_GREEN + lang.get("zombie");
                final String playerMessage = ChatColor.GREEN + "    " + lang.get("team") + ": " + ChatColor.AQUA + lang.get("player");
                if (!gameStarted) {
                    if (Bukkit.getOnlinePlayers().size() == 0) {
                        timerStarted = false;
                        this.cancel();
                    }
                    if (Bukkit.getOnlinePlayers().size() < Constants.mininumPlayers) hasEnoughPlayers = false;
                    for (final Player player : Bukkit.getOnlinePlayers()) {
                        // team ----->
                        Scoreboard scoreboard = hashMapScoreboard.get(player.getUniqueId());
                        Objective objective3 = scoreboard.getObjective(DisplaySlot.SIDEBAR);
                        //Objective hpobjective2 = scoreboard.getObjective(DisplaySlot.BELOW_NAME);
                        //Score hp = hpobjective2.getScore(player.getName());
                        //hp.setScore((int) player.getHealth());
                        String lastScore8 = hashMapLastScore8.get(player.getUniqueId());
                        scoreboard.resetScores(lastScore8);
                        lastScore8 = ChatColor.GREEN + "    " + lang.get("player") + ": " + Bukkit.getOnlinePlayers().size() + "/" + Bukkit.getMaxPlayers();
                        Score score8 = objective3.getScore(lastScore8);
                        score8.setScore(8);
                        String lastScore4 = hashMapLastScore4.get(player.getUniqueId());
                        scoreboard.resetScores(lastScore4);
                        if (hasEnoughPlayers && settingsCheck)
                            lastScore4 = "    " + Lang.format(lang.get("startingIn"), TomeitoAPI.secondsToTime(timesLeft));
                        else
                            lastScore4 = "    " + lang.get("waiting");
                        hashMapLastScore8.put(player.getUniqueId(), lastScore8);
                        hashMapLastScore4.put(player.getUniqueId(), lastScore4);
                        Score score4 = objective3.getScore(lastScore4);
                        score4.setScore(4);
                        scoreboard.resetScores(zombieMessage);
                        scoreboard.resetScores(playerMessage);
                        if (hashMapTeam.get(player.getUniqueId()) == PlayerTeam.ZOMBIE) {
                            Score score6 = objective3.getScore(zombieMessage);
                            score6.setScore(6);
                        } else if (hashMapTeam.get(player.getUniqueId()) == PlayerTeam.PLAYER) {
                            Score score6 = objective3.getScore(playerMessage);
                            score6.setScore(6);
                        }
                        // <----- team
                        // vote ----->
                        if (timesLeft >= 11) {
                            hashMapVote.values().forEach(vote -> votes.put(vote, votes.getOrDefault(vote, 0)+1));
                            File maps = new File("./plugins/ZombieEscape/maps/");
                            File[] keys = maps.listFiles();
                            assert keys != null;
                            for (File file : keys) {
                                ConfigProvider map = new ConfigProvider("./plugins/ZombieEscape/maps/" + file.getName().replaceAll(".yml", "") + ".yml");
                                scoreboard.resetScores(ChatColor.GREEN + "    " + lang.get("mapVote") + ": " + map.getString("mapname", "???"));
                                Score score = objective3.getScore(ChatColor.GREEN + "    " + lang.get("mapVote") + ": " + map.getString("mapname", "???"));
                                score.setScore(-votes.getOrDefault(file.getName().replaceAll(".yml", ""), 0));
                            }
                            votes = new HashMap<>(); // re-intialize this map because there's no HashMap#removeAll()
                        }
                        if (timesLeft == 10) {
                            player.getInventory().clear();
                            votes = new HashMap<>();
                            scoreboard.resetScores(defmapString);
                            player.sendMessage(lang.get("closedMapVoting"));
                            hashMapVote.values().forEach(vote -> votes.put(vote, votes.getOrDefault(vote, 0)+1));
                            votes.forEach((name, count) -> {
                                if (mostVotes < count) {
                                    mostVotes = count;
                                    mostVotedMap = name;
                                }
                            });
                            if (mostVotedMap == null) mostVotedMap = mapName; // default map
                            mapName = mostVotedMap;
                            Utils.reload();
                            player.sendMessage(Lang.format(lang.get("mapIsNow"), mapConfig.getString("mapname", "???")));
                            World world = Bukkit.getWorld(mapConfig.getString("spawnPoints.world", "world"));
                            world.setGameRuleValue("announceAdvancements", "false");
                            if (mapConfig == null) mapConfig = ConfigProvider.initWithoutException("./plugins/ZombieEscape/maps/" + mapName + ".yml");
                            if (!playersReset) {
                                playersReset = true;
                                players = 0;
                                zombies = 0;
                            }
                            board.resetScores(playerMessage); // reset team
                            if (hashMapTeam.get(event.getPlayer().getUniqueId()) != PlayerTeam.SPECTATOR) {
                                if ((((int) Math.round((double) Bukkit.getOnlinePlayers().size() / (double) 10) - zombies) >= 0) && !previousZombies.contains(player.getUniqueId().toString())) {
                                    listZombies.add(player.getUniqueId().toString());
                                    hashMapOriginZombie.put(player.getUniqueId(), true);
                                    hashMapTeam.put(player.getUniqueId(), PlayerTeam.ZOMBIE);
                                    zombies = zombies+1;
                                    Score score6 = objective.getScore(ChatColor.GREEN + "    " + lang.get("team") + ": " + ChatColor.DARK_GREEN + lang.get("zombie"));
                                    score6.setScore(6);
                                    player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(250);
                                    player.setHealth(250);
                                    player.setHealthScale(40);
                                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100000, 0, false, false));
                                    player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 100000, 0, false, false));
                                    player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 100000, 100, false, false));
                                    player.setPlayerListName(ChatColor.DARK_GREEN + player.getName());
                                    ZombieVillagerDisguise d = (ZombieVillagerDisguise) DisguiseType.ZOMBIE_VILLAGER.newInstance();
                                    d.setAdult(true);
                                    d.setCustomNameVisible(true);
                                    d.setCustomName(player.getName());
                                    disguise.disguise(player, d);
                                } else {
                                    players = players+1;
                                    hashMapTeam.put(player.getUniqueId(), PlayerTeam.PLAYER);
                                    //player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(2);
                                    //player.setHealth(2);
                                    //player.setHealthScale(2);
                                    player.getInventory().setHelmet(new ItemStack(Material.DIAMOND_HELMET));
                                    player.getInventory().setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
                                    player.getInventory().setLeggings(new ItemStack(Material.DIAMOND_LEGGINGS));
                                    player.getInventory().setBoots(new ItemStack(Material.DIAMOND_BOOTS));
                                    player.removePotionEffect(PotionEffectType.INCREASE_DAMAGE);
                                    Score score6 = objective.getScore(ChatColor.GREEN + "    " + lang.get("team") + ": " + ChatColor.AQUA + lang.get("player"));
                                    score6.setScore(6);
                                    player.setPlayerListName(ChatColor.AQUA + player.getName());
                                }
                            }
                            for (Player player2 : Bukkit.getOnlinePlayers()) respawnWait.put(player2.getUniqueId(), true);
                            Utils.teleportAllPlayers();
                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_PLING, 100, 1);
                            player.sendTitle(ChatColor.GREEN + "10", ChatColor.YELLOW + lang.get("team") + ": " + hashMapTeam.get(player.getUniqueId()), 0, 25, 0);
                        }
                        // <----- vote
                        /* do not edit this line */ player.setScoreboard(hashMapScoreboard.get(player.getUniqueId()));
                        if (timesLeft == 5) {
                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_PLING, 100, 1);
                            player.sendTitle(ChatColor.GREEN + "5", ChatColor.YELLOW + lang.get("team") + ": " + hashMapTeam.get(player.getUniqueId()), 0, 25, 0);
                        } else if (timesLeft == 4) {
                            player.setGameMode(GameMode.ADVENTURE);
                            player.getWorld().setGameRuleValue("doMobLoot", "false");
                            player.getWorld().setGameRuleValue("doDaylightCycle", "false");
                            player.getWorld().setGameRuleValue("keepInventory", "true");
                            player.getWorld().setGameRuleValue("doFireTick", "false");
                            player.getWorld().setGameRuleValue("naturalRegeneration", "false");
                            player.getWorld().setTime(mapConfig.getInt("time", 6000));
                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_PLING, 100, 1);
                            player.sendTitle(ChatColor.AQUA + "4", ChatColor.YELLOW + lang.get("team") + ": " + hashMapTeam.get(player.getUniqueId()), 0, 25, 0);
                        } else if (timesLeft == 3) {
                            ConfigProvider.setThenSave("previousZombies", listZombies, "ZombieEscape");
                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_PLING, 100, 1);
                            player.sendTitle(ChatColor.BLUE + "3", ChatColor.YELLOW + lang.get("team") + ": " + hashMapTeam.get(player.getUniqueId()), 0, 25, 0);
                        } else if (timesLeft == 2) {
                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_PLING, 100, 1);
                            player.sendTitle(ChatColor.YELLOW + "2", ChatColor.YELLOW + lang.get("team") + ": " + hashMapTeam.get(player.getUniqueId()), 0, 25, 0);
                        } else if (timesLeft == 1) {
                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_PLING, 100, 1);
                            player.sendTitle(ChatColor.RED + "1", ChatColor.YELLOW + lang.get("team") + ": " + hashMapTeam.get(player.getUniqueId()), 0, 25, 0);
                        } else if (timesLeft == 0) {
                            player.setGameMode(GameMode.ADVENTURE);
                            gameStarted = true;
                            for (Player player2 : Bukkit.getOnlinePlayers()) if (hashMapTeam.get(player2.getUniqueId()) == PlayerTeam.PLAYER) respawnWait.remove(player2.getUniqueId(), true);
                            player.playSound(player.getLocation(), Sound.ENTITY_ENDERDRAGON_GROWL, 100, 1);
                            if (hashMapTeam.get(player.getUniqueId()) == PlayerTeam.ZOMBIE) {
                                player.sendTitle("" + ChatColor.GREEN + ChatColor.BOLD + "GO!", ChatColor.YELLOW + lang.get("zombieObjective"), 0, 120, 0);
                                player.sendMessage(lang.get("teleportIn")); // move*
                                if (!once) {
                                    once = true;
                                    count = 12;
                                    new BukkitRunnable() {
                                        public void run() {
                                            ZombieEscape.ongoingEventMap.put("zombieRelease", Lang.format(lang.get("releaseZombie"), Integer.toString(count)));
                                            if (count <= 0) {
                                                ZombieEscape.ongoingEventMap.remove("zombieRelease");
                                                this.cancel();
                                                return;
                                            }
                                            count--;
                                        }
                                    }.runTaskTimer(getInstance(), 0, 20);
                                    Utils.doBossBarTick(Bukkit.createBossBar("zombieRelease", BarColor.GREEN, BarStyle.SOLID, BarFlag.DARKEN_SKY), count, "zombieRelease");
                                }
                                new BukkitRunnable() {
                                    @Override
                                    public void run() {
                                        for (Player player2 : Bukkit.getOnlinePlayers()) if (hashMapTeam.get(player2.getUniqueId()) == PlayerTeam.ZOMBIE) respawnWait.remove(player2.getUniqueId(), true);
                                        player.setGameMode(GameMode.ADVENTURE);
                                        player.addPotionEffect(PotionEffectType.SPEED.createEffect(100000, 0));
                                        ItemStack item = new ItemStack(Material.IRON_SWORD);
                                        ItemMeta meta = item.getItemMeta();
                                        meta.setDisplayName("" + ChatColor.RESET + ChatColor.WHITE + lang.get("knife"));
                                        item.setItemMeta(meta);
                                        item.addUnsafeEnchantment(Enchantment.DURABILITY, 100);
                                        item.addUnsafeEnchantment(Enchantment.DAMAGE_ALL, 100);
                                        player.getInventory().setItem(0, item);
                                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "give " + player.getName() + " minecraft:stone_axe 1 0 {CanDestroy:[\"minecraft:planks\"],HideFlags:1,Unbreakable:1,display:{Name:\"" + lang.get("rustedAxe") + "\"},ench:[{id:32,lvl:10}]}");
                                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "give " + player.getName() + " minecraft:stone_pickaxe 1 0 {CanDestroy:[\"minecraft:gold_block\",\"minecraft:cobblestone\",\"lapis_block\"],HideFlags:1,Unbreakable:1,display:{Name:\"" + lang.get("rustedPickaxe") + "\"},ench:[{id:32,lvl:10}]}");
                                    }
                                }.runTaskLater(getInstance(), 20*12);
                            } else if (hashMapTeam.get(player.getUniqueId()) == PlayerTeam.PLAYER) {
                                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "shot give " + player.getName() + " ak-47");
                                //player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(2);
                                player.sendTitle("" + ChatColor.GREEN + ChatColor.BOLD + "GO!", ChatColor.YELLOW + lang.get("playerObjective"), 0, 120, 0);
                                String[] spawnLists = Arrays.asList(mapConfig.getList("spawnPoints.player", new ArrayList<String>()).toArray(new String[0])).get(0).split(",");
                                Location location = new Location(Bukkit.getWorld(mapConfig.getString("spawnPoints.world")), Double.parseDouble(spawnLists[0]), Double.parseDouble(spawnLists[1]), Double.parseDouble(spawnLists[2]));
                                if (!player.teleport(location)) {
                                    player.sendMessage(lang.get("failedWarp"));
                                    return;
                                }
                                for (final Player playerl : Bukkit.getOnlinePlayers()) {
                                    for (final Player other : Bukkit.getOnlinePlayers()) {
                                        playerl.hidePlayer(ZombieEscape.this, other);
                                        playerl.showPlayer(ZombieEscape.this, other);
                                        other.hidePlayer(ZombieEscape.this, playerl);
                                        other.showPlayer(ZombieEscape.this, playerl);
                                    }
                                }
                            } else {
                                player.setGameMode(GameMode.SPECTATOR);
                            }
                        }
                    }
                    if (hasEnoughPlayers && timesLeft >= 0 && settingsCheck) timesLeft--;
                } else {
                    if (playedTime >= gameTime) {
                        endGame(lang.get("zombie"));
                        this.cancel();
                        return;
                    }
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        final Scoreboard scoreboard = hashMapScoreboard.get(player.getUniqueId());
                        //Objective hpobjective2 = scoreboard.getObjective(DisplaySlot.BELOW_NAME);
                        //Score hp = hpobjective2.getScore(player.getName());
                        //hp.setScore((int) player.getHealth());
                        if (playedTime <= 1) {
                            File maps = new File("./plugins/ZombieEscape/maps/");
                            File[] keys = maps.listFiles();
                            assert keys != null;
                            for (File file : keys) {
                                String thisMapName = file.getName().replaceAll(".yml", "");
                                ConfigProvider map = new ConfigProvider("./plugins/ZombieEscape/maps/" + thisMapName + ".yml");
                                scoreboard.resetScores(ChatColor.GREEN + "    " + lang.get("mapVote") + ": " + map.getString("mapname", "???"));
                            }
                        }
                        Objective objective3 = scoreboard.getObjective(DisplaySlot.SIDEBAR);
                        String lastScore4 = hashMapLastScore4.get(event.getPlayer().getUniqueId());
                        scoreboard.resetScores(lastScore4);
                        lastScore4 = ChatColor.GREEN + "    " + secondsToTime(playedTime) + " / " + secondsToTime(gameTime);
                        hashMapLastScore4.put(player.getUniqueId(), lastScore4);
                        Score score4 = objective3.getScore(lastScore4);
                        score4.setScore(4);
                        Score score0 = objective3.getScore("     ");
                        score0.setScore(0);
                        for (int i = 1; i < maxCheckpoints; i++) {
                            String okString = ChatColor.RED + "✕ " + lang.get("checkpoint") + i;
                            String koString = ChatColor.GREEN + "✓ " + lang.get("checkpoint") + i;
                            boolean zombiePassedcp = zombieCheckpoint >= i;
                            boolean zombieIncp = zombieCheckpoint == i;
                            //boolean playerPassedcp = playerCheckpoint >= i;
                            boolean playerIncp = playerCheckpoint == i;
                            String status = "";
                            if (zombieIncp) status += ChatColor.DARK_GREEN + " [Z]";
                            if (playerIncp) status += ChatColor.AQUA + " [P]";
                            scoreboard.resetScores(okString);
                            scoreboard.resetScores(koString);
                            scoreboard.resetScores(koString + ChatColor.AQUA + " <-" + ChatColor.AQUA + " [P]");
                            scoreboard.resetScores(okString + ChatColor.AQUA + " <-" + ChatColor.AQUA + " [P]");
                            scoreboard.resetScores(okString + ChatColor.AQUA + " <-" + ChatColor.DARK_GREEN + " [Z]");
                            scoreboard.resetScores(okString + ChatColor.AQUA + " <-" + ChatColor.DARK_GREEN + " [Z]" + ChatColor.AQUA + " [P]");
                            String s = status.equals("") ? "" : ChatColor.AQUA + " <-" + status;
                            Score score;
                            if (zombiePassedcp) {
                                score = objective3.getScore(okString + s);
                            } else {
                                score = objective3.getScore(koString + s);
                            }
                            score.setScore(-i);
                        }
                        player.setScoreboard(scoreboard);
                    }
                    playedTime++;
                }
                if (debug) {
                    long end = System.currentTimeMillis()-time;
                    Log.debug("Scoreboard update tick took " + end + "ms");
                    Log.debug("Zombies: " + zombies + ", Players: " + players);
                }
            }
        }.runTaskTimer(this, 0, 20);
    }

    @EventHandler
    public synchronized void onPlayerDeath(final PlayerDeathEvent event) {
        if (event.getEntityType() != EntityType.PLAYER) return;
        if (hashMapTeam.get(event.getEntity().getUniqueId()) == PlayerTeam.SPECTATOR) return;
        event.getEntity().getInventory().clear();
        if (hashMapTeam.get(event.getEntity().getUniqueId()) == PlayerTeam.PLAYER) {
            Bukkit.broadcastMessage(Lang.format(lang.get("becameZombieNaturally"), event.getEntity().getName()));
            players--;
        }
        hashMapTeam.remove(event.getEntity().getUniqueId());
        hashMapTeam.put(event.getEntity().getUniqueId(), PlayerTeam.ZOMBIE);
        final Objective objective = hashMapScoreboard.get(event.getEntity().getUniqueId()).getObjective("scoreboard");
        Score score6 = objective.getScore(ChatColor.GREEN + "    " + lang.get("team") + ": " + ChatColor.DARK_GREEN + lang.get("zombie"));
        objective.getScoreboard().resetScores(ChatColor.GREEN + "    " + lang.get("team") + ": " + ChatColor.AQUA + lang.get("player"));
        score6.setScore(6);
        event.getEntity().setPlayerListName(ChatColor.DARK_GREEN + event.getEntity().getName());
        if (players <= 0 && gameStarted) endGame(lang.get("zombie"));
    }

    @SuppressWarnings("SuspiciousToArrayCall")
    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        String[] spawnLists = Arrays.asList(mapConfig.getList("spawnPoints.zombie", new ArrayList<String>()).toArray(new String[0])).get(zombieCheckpoint).split(",");
        Location location = new Location(Bukkit.getWorld(mapConfig.getString("spawnPoints.world")), Double.parseDouble(spawnLists[0]), Double.parseDouble(spawnLists[1]), Double.parseDouble(spawnLists[2]));
        event.setRespawnLocation(location);
        event.getPlayer().setGameMode(GameMode.ADVENTURE);
        event.getPlayer().setPlayerListName(ChatColor.DARK_GREEN + event.getPlayer().getName());
        event.getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(150);
        event.getPlayer().setHealth(150);
        event.getPlayer().setHealthScale(40);
        event.getPlayer().sendMessage(lang.get("respawn"));
        respawnWait.put(event.getPlayer().getUniqueId(), true);
        ZombieVillagerDisguise d = (ZombieVillagerDisguise) DisguiseType.ZOMBIE_VILLAGER.newInstance();
        d.setAdult(true);
        d.setCustomNameVisible(true);
        d.setCustomName(event.getPlayer().getName());
        disguise.disguise(event.getPlayer(), d);
        new BukkitRunnable() {
            public void run() {
                respawnWait.remove(event.getPlayer().getUniqueId());
                event.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 100000, 0, false, false));
                event.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 100000, 1, false, false));
                event.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 100000, 100, false, false));
                event.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100000, 0, false, false));
                event.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 100000, 0, false, false));
                ItemStack item = new ItemStack(Material.IRON_SWORD);
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName("" + ChatColor.RESET + ChatColor.WHITE + lang.get("knife"));
                item.setItemMeta(meta);
                item.addUnsafeEnchantment(Enchantment.DURABILITY, 100);
                item.addUnsafeEnchantment(Enchantment.DAMAGE_ALL, 100);
                event.getPlayer().getInventory().setItem(0, item);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "give " + event.getPlayer().getName() + " minecraft:stone_axe 1 0 {CanDestroy:[\"minecraft:planks\"],HideFlags:1,Unbreakable:1,display:{Name:\"" + lang.get("rustedAxe") + "\"},ench:[{id:32,lvl:10}]}");
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "give " + event.getPlayer().getName() + " minecraft:stone_pickaxe 1 0 {CanDestroy:[\"minecraft:gold_block\",\"minecraft:cobblestone\",\"lapis_block\"],HideFlags:1,Unbreakable:1,display:{Name:\"" + lang.get("rustedPickaxe") + "\"},ench:[{id:32,lvl:10}]}");
            }
        }.runTaskLater(this, 20*5);
    }

    public static String secondsToTime(int seconds) {
        int minutes = (int) Math.floor((float) seconds / 60F);
        String sec = Integer.toString(seconds % 60);
        return minutes + ":" + (sec.length() == 1 ? "0" + sec : sec);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (respawnWait.getOrDefault(event.getPlayer().getUniqueId(), false)) event.setCancelled(true);
    }

    @SuppressWarnings("NonAtomicOperationOnVolatileField")
    @EventHandler(priority=EventPriority.LOWEST)
    public void onPlayerHurt(EntityDamageByEntityEvent event) {
        long time = System.currentTimeMillis();
        if (event.getDamager() instanceof Snowball) return;
        if (!(event.getDamager() instanceof Projectile)) if (event.getEntityType() != EntityType.PLAYER || event.getDamager().getType() != EntityType.PLAYER) return;
        event.setCancelled(true);
        if (!gameStarted || gameEnded) return;
        if (hashMapTeam.get(event.getDamager().getUniqueId()) == PlayerTeam.ZOMBIE && playedTime < 12) return; // zombie can't be damaged others if < 12 seconds
        if (hashMapTeam.get(event.getEntity().getUniqueId()) == PlayerTeam.ZOMBIE) return; // cancel punch player -> zombie
        if (hashMapTeam.get(event.getDamager().getUniqueId()) == hashMapTeam.get(event.getEntity().getUniqueId())) return; // friendly fire
        Player player = (Player) event.getEntity();
        if (hashMapTeam.get(player.getUniqueId()) == PlayerTeam.PLAYER) {
            if (event.getDamager() instanceof Snowball) return;
            players = players - 1;
            zombies = zombies + 1;
        }
        player.getInventory().clear();
        hashMapTeam.remove(player.getUniqueId());
        hashMapTeam.put(player.getUniqueId(), PlayerTeam.ZOMBIE);
        final Objective objective = hashMapScoreboard.get(player.getUniqueId()).getObjective("scoreboard");
        Score score6 = objective.getScore(ChatColor.GREEN + "    " + lang.get("team") + ": " + ChatColor.DARK_GREEN + lang.get("zombie"));
        objective.getScoreboard().resetScores(ChatColor.GREEN + "    " + lang.get("team") + ": " + ChatColor.AQUA + lang.get("player"));
        score6.setScore(6);
        player.setGameMode(GameMode.ADVENTURE);
        player.setPlayerListName(ChatColor.DARK_GREEN + player.getName());
        player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(150);
        player.setHealth(150);
        player.setHealthScale(40);
        ZombieVillagerDisguise d = (ZombieVillagerDisguise) DisguiseType.ZOMBIE_VILLAGER.newInstance();
        d.setAdult(true);
        d.setCustomNameVisible(true);
        d.setCustomName(player.getName());
        disguise.disguise(player, d);
        new BukkitRunnable() {
            public void run() {
                player.addPotionEffect(PotionEffectType.HUNGER.createEffect(100000, 0));
                player.addPotionEffect(PotionEffectType.SATURATION.createEffect(100000, 1));
                player.addPotionEffect(PotionEffectType.INCREASE_DAMAGE.createEffect(100000, 100));
                player.addPotionEffect(PotionEffectType.SPEED.createEffect(100000, 0));
                player.addPotionEffect(PotionEffectType.JUMP.createEffect(100000, 0));
                ItemStack item = new ItemStack(Material.IRON_SWORD);
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName("" + ChatColor.RESET + ChatColor.WHITE + lang.get("knife"));
                item.setItemMeta(meta);
                item.addUnsafeEnchantment(Enchantment.DURABILITY, 100);
                item.addUnsafeEnchantment(Enchantment.DAMAGE_ALL, 100);
                player.getInventory().setItem(0, item);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "give " + player.getName() + " minecraft:stone_axe 1 0 {CanDestroy:[\"minecraft:planks\"],HideFlags:1,Unbreakable:1,display:{Name:\"" + lang.get("rustedAxe") + "\"},ench:[{id:32,lvl:10}]}");
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "give " + player.getName() + " minecraft:stone_pickaxe 1 0 {CanDestroy:[\"minecraft:gold_block\",\"minecraft:cobblestone\",\"lapis_block\"],HideFlags:1,Unbreakable:1,display:{Name:\"" + lang.get("rustedPickaxe") + "\"},ench:[{id:32,lvl:10}]}");
            }
        }.runTaskLater(this, 40);
        for (Player player2 : Bukkit.getOnlinePlayers()) {
            player2.playSound(player2.getLocation(), Sound.ENTITY_ENDERDRAGON_GROWL, 80, 1); // avoid loud sound, it's 80%!
        }
        player.sendTitle(lang.get("youBecameZombie"), "", 0, 40, 0);
        Bukkit.broadcastMessage(Lang.format(lang.get("becameZombie"), player.getName(), event.getDamager().getName()));
        if (players <= 0 && gameStarted) {
            endGame(lang.get("zombie"));
        }
        if (debug) {
            long end = System.currentTimeMillis()-time;
            Log.debug("onPlayerHurt() took " + end + "ms");
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onEntityDamage(EntityDamageEvent event) {
        if (debug) Log.debug("Called onEntityDamage");
        if (event.getDamage() == 5.0) return;
        if (event.getEntityType() != EntityType.PLAYER) return;
        if (!gameStarted || gameEnded) event.setCancelled(true);
        if (event.getCause() == DamageCause.FALL) event.setCancelled(true);
        if (hashMapTeam.get(event.getEntity().getUniqueId()) == PlayerTeam.PLAYER && event.getCause() == DamageCause.PROJECTILE) event.setCancelled(true);
    }

    @EventHandler
    public synchronized void onPlayerLeft(PlayerQuitEvent event) {
        if (Bukkit.getOnlinePlayers().size() >= Constants.mininumPlayers) hasEnoughPlayers = true; else {
            hasEnoughPlayers = false;
            timesLeft = 1800;
        }
        if (hashMapTeam.get(event.getPlayer().getUniqueId()) == PlayerTeam.ZOMBIE) {
            zombies = zombies - 1;
        } else if(hashMapTeam.get(event.getPlayer().getUniqueId()) == PlayerTeam.PLAYER) {
            players = players - 1;
        } // if else, do nothing.
        if (gameStarted && zombies < 0) {
            zombies = 0;
            throw new IllegalStateException("Zombie count is should be 0 or more.");
        }
        if (gameStarted && players < 0) {
            players = 0;
            throw new IllegalStateException("Player count is should be 0 or more.");
        }
        hashMapTeam.remove(event.getPlayer().getUniqueId());
        if (gameStarted && (zombies == 0 || players == 0)) {
            String team = zombies == 0 ? lang.get("player") : lang.get("zombie");
            endGame(team);
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onProjectileHit(ProjectileHitEvent event) {
		/*
		if (event.getHitEntity() != null) {
			if (hashMapTeam.get(event.getHitEntity().getUniqueId()) != PlayerTeam.ZOMBIE) return;
			Damageable d = (Damageable) event.getHitEntity();
			d.damage(7.0);
			d.setVelocity(event.getEntity().getLocation().getDirection().multiply(1));
			return;
		}*/
        long time = System.currentTimeMillis();
        Block block = event.getHitBlock();
        if (block == null) return;
        int durability = (int) Math.nextUp(Math.min(Constants.materialDurability.getOrDefault(block.getType(), 5)*((double)players/(double)5), 3000));
        if (Constants.breakableWall.contains(block.getType())) {
            String location = block.getLocation().getBlockX() + "," + block.getLocation().getBlockY() + "," + block.getLocation().getBlockZ();
            String wall = (String) locationWall.getOrDefault(location, null);
            int state = hashMapBlockState.get(wall) != null ? hashMapBlockState.get(wall) : 0;
            if (state >= durability) {
                mapConfig.getStringList("wallLocation." + wall).forEach(blocation -> {
                    String[] blocationArray = blocation.split(",");
                    Block ablock = event.getEntity().getWorld().getBlockAt(Integer.parseInt(blocationArray[0]), Integer.parseInt(blocationArray[1]), Integer.parseInt(blocationArray[2]));
                    ablock.setType(Material.AIR);
                });
                block.setType(Material.AIR);
                hashMapBlockState.remove(wall);
                block.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, block.getLocation(), 30);
                block.getWorld().playSound(block.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 100, 1);
                return;
            }
            hashMapBlockState.put(wall, state+1);
            block.getWorld().spawnParticle(Particle.BLOCK_CRACK, block.getLocation(), 2, block.getState().getData());
        }
        if (debug) {
            long end = System.currentTimeMillis()-time;
            Log.debug("onProjectileHit() took " + end + "ms");
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        long time = System.currentTimeMillis();
        Block block = event.getBlock();
        if (block == null) return;
        String location = block.getLocation().getBlockX() + "," + block.getLocation().getBlockY() + "," + block.getLocation().getBlockZ();
        String wall = (String) locationWall.getOrDefault(location, null);
        hashMapBlockState.remove(wall);
        if (debug) {
            long end = System.currentTimeMillis()-time;
            Log.debug("onBlockBreak() took " + end + "ms");
        }
    }

    @EventHandler(priority=EventPriority.LOWEST)
    public void onPlayerLogin(PlayerLoginEvent event) {
        if (!init) {
            event.disallow(Result.KICK_OTHER, lang.get("loginFailed_startingServer"));
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (event.getMessage().equalsIgnoreCase("gg") || event.getMessage().equalsIgnoreCase("good game")) {
            event.setMessage(ChatColor.GOLD + event.getMessage());
        }
        if (hashMapTeam.get(event.getPlayer().getUniqueId()) == PlayerTeam.ZOMBIE) {
            Utils.chat(event, PlayerTeam.ZOMBIE, ChatColor.DARK_GREEN + "[Z]");
        } else if (hashMapTeam.get(event.getPlayer().getUniqueId()) == PlayerTeam.PLAYER) {
            Utils.chat(event, PlayerTeam.PLAYER, ChatColor.AQUA + "[P]");
        } else {
            Utils.chat(event, PlayerTeam.SPECTATOR, ChatColor.GRAY + "[S]", true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) { if (event.getSlotType() == SlotType.ARMOR) event.setCancelled(true); }

    @EventHandler
    public void onDropItem(PlayerDropItemEvent event) {
        ItemStack knifeitem = new ItemStack(Material.IRON_SWORD);
        ItemMeta knifemeta = knifeitem.getItemMeta();
        knifemeta.setDisplayName("" + ChatColor.RESET + ChatColor.WHITE + lang.get("knife"));
        knifeitem.setItemMeta(knifemeta);
        knifeitem.addUnsafeEnchantment(Enchantment.DURABILITY, 100); // Always sharp!
        knifeitem.addUnsafeEnchantment(Enchantment.DAMAGE_ALL, 100); // Sharpness like #.
        if (event.getItemDrop().getItemStack().isSimilar(knifeitem)) event.setCancelled(true); // Please don't drop knife, its dangerous
        if (event.getItemDrop().getItemStack().isSimilar(new ItemStack(Material.DIAMOND_HELMET))) event.setCancelled(true); // armor
        if (event.getItemDrop().getItemStack().isSimilar(new ItemStack(Material.DIAMOND_CHESTPLATE))) event.setCancelled(true); // armor
        if (event.getItemDrop().getItemStack().isSimilar(new ItemStack(Material.DIAMOND_LEGGINGS))) event.setCancelled(true); // armor
        if (event.getItemDrop().getItemStack().isSimilar(new ItemStack(Material.DIAMOND_BOOTS))) event.setCancelled(true); // armor
        if (event.getItemDrop().getItemStack().isSimilar(Utils.createLeatherItemStack(Material.LEATHER_HELMET, 0, 100, 0))) event.setCancelled(true); // armor
        if (event.getItemDrop().getItemStack().isSimilar(Utils.createLeatherItemStack(Material.LEATHER_CHESTPLATE, 0, 100, 0))) event.setCancelled(true); // armor
        if (event.getItemDrop().getItemStack().isSimilar(Utils.createLeatherItemStack(Material.LEATHER_LEGGINGS, 0, 100, 0))) event.setCancelled(true); // armor
        if (event.getItemDrop().getItemStack().isSimilar(Utils.createLeatherItemStack(Material.LEATHER_BOOTS, 0, 100, 0))) event.setCancelled(true); // armor
        if (event.getItemDrop().getItemStack().getType() == Material.IRON_BARDING) event.setCancelled(true); // Please don't drop gun
        if (event.getItemDrop().getItemStack().getType() == Material.STONE_PICKAXE) event.setCancelled(true); // Please don't drop pickaxe
        if (event.getItemDrop().getItemStack().getType() == Material.STONE_AXE) event.setCancelled(true); // Please don't drop axe
    }

    private int shutdownCount = 15;

    public void endGame(String team) {
        if (shutdownCount != 15) return;
        gameEnded = true;
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendTitle(Lang.format(lang.get("gameEnd"), team), "", 0, 40, 0);
            new BukkitRunnable() {
                public void run() {
                    if (fireworked*Bukkit.getOnlinePlayers().size() >= 40*Bukkit.getOnlinePlayers().size()) this.cancel();
                    player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_LAUNCH, 100, 1);
                    //TNTPrimed tnt = player.getWorld().spawn(player.getLocation(), TNTPrimed.class);
                    //tnt.setFuseTicks(40);
                    fireworked++;
                }
            }.runTaskTimer(getInstance(), 0, 5);
        }
        Bukkit.broadcastMessage(Lang.format(lang.get("gameEnd"), team));
        new BukkitRunnable() {
            public void run() {
                ZombieEscape.ongoingEvent = Lang.format(lang.get("shutdownIn"), Integer.toString(shutdownCount));
                if (shutdownCount <= 0) {
                    ZombieEscape.ongoingEvent = null;
                    this.cancel();
                    return;
                }
                shutdownCount--;
            }
        }.runTaskTimer(ZombieEscape.getProvidingPlugin(ZombieEscape.class), 0, 20);
    }

    public final class EndGame implements CommandExecutor {
        private int count = 0;

        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (sender instanceof ConsoleCommandSender) {
                sender.sendMessage(ChatColor.RED + "This command must be used in-game.");
                return true;
            }
            if (!ZombieEscape.gameStarted) {
                sender.sendMessage(lang.get("gameIsntStarted"));
                return true;
            }
            Bukkit.broadcastMessage(lang.get("escapeIn"));
            new BukkitRunnable() {
                public void run() {
                    gameEnded = true;
                    Player nearestPlayer;
                    List<Player> players;
                    if (sender instanceof BlockCommandSender) {
                        nearestPlayer = Utils.targetPFindPlayers(((BlockCommandSender)sender).getBlock().getLocation());
                        players = Utils.targetAFindPlayersWithRange(((BlockCommandSender)sender).getBlock().getLocation(), 10);
                    } else if (sender instanceof Player) {
                        nearestPlayer = Utils.targetPFindPlayers(((Player)sender).getLocation());
                        players = Utils.targetAFindPlayersWithRange(((Player)sender).getLocation(), 10);
                    } else { // it shouldn't happen
                        sender.sendMessage(ChatColor.RED + "Unknown type: " + sender.toString() + ", Name: " + sender.getName());
                        return;
                    }
                    ZombieEscape.gameEnded = true;
                    String team = nearestPlayer == null ? lang.get("zombie") : lang.get("player");
                    assert players != null;
                    if (players.size() != 0) {
                        Bukkit.broadcastMessage(lang.get("followingPlayersWon"));
                        players.forEach(player -> Bukkit.broadcastMessage(ChatColor.GREEN + " - " + player.getName()));
                    }
                    endGame(team);
                }
            }.runTaskLater(getInstance(), 20*10);
            count = 10;
            new BukkitRunnable() {
                public void run() {
                    ZombieEscape.ongoingEvent = Lang.format(lang.get("escapeIn2"), Integer.toString(count));
                    if (count <= 0) {
                        ZombieEscape.ongoingEvent = null;
                        this.cancel();
                        return;
                    }
                    count--;
                }
            }.runTaskTimer(ZombieEscape.getProvidingPlugin(ZombieEscape.class), 0, 20);
            return true;
        }
    }
}
