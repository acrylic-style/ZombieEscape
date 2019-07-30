// *Warning* Bugged string may be found while decompiling this source!
package xyz.acrylicstyle.zombieescape;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;

import xyz.acrylicstyle.zombieescape.commands.Sponsor;
import xyz.acrylicstyle.zombieescape.commands.ZombieEscapeConfig;
import xyz.acrylicstyle.zombieescape.commands.ZombieEscapeGameUtil;
import xyz.acrylicstyle.zombieescape.providers.ConfigProvider;

public class ZombieEscape extends JavaPlugin implements Listener {
	public final static int mininumPlayers = 2;
	public static ConfigProvider config = null;
	public static HashMap<UUID, Scoreboard> hashMapScoreboard = new HashMap<UUID, Scoreboard>();
	public static HashMap<UUID, String> hashMapTeam = new HashMap<UUID, String>();
	public static HashMap<UUID, String> hashMapLastScore4 = new HashMap<UUID, String>();
	public static HashMap<UUID, String> hashMapLastScore8 = new HashMap<UUID, String>();
	public static HashMap<Location, Integer> hashMapBlockState = new HashMap<Location, Integer>();
	public static ScoreboardManager manager = null;
	public static ProtocolManager protocol = null;
	public static int zombies = 0;
	public static int players = 0;
	public static int timesLeft = 180;
	public static boolean timerStarted = false;
	public static boolean hasEnoughPlayers = false;
	public static boolean settingsCheck = false;
	public static HashMap<String, Team> teams = new HashMap<String, Team>();
	public static boolean gameStarted = false;
	public static int gameTime = 1800; // 30 minutes
	public static int playedTime = 0;
	public static int checkpoint = 0;
	public static int fireworked = 0;

	@Override
	public void onEnable() {
		protocol = ProtocolLibrary.getProtocolManager();
		try {
			config = new ConfigProvider("./plugins/ZombieEscape/config.yml");
		} catch (IOException | InvalidConfigurationException e1) {
			e1.printStackTrace();
			Bukkit.getLogger().severe("[ZombieEscape] Failed to load config, disabling plugin.");
			Bukkit.getPluginManager().disablePlugin(this);
			return;
		}
		manager = Bukkit.getScoreboardManager();
		Sponsor sponsor = null;
		ZombieEscapeConfig zec = null;
		ZombieEscapeGameUtil zegu = null;
		try {
			sponsor = new Sponsor();
			zec = new ZombieEscapeConfig();
			zegu = new ZombieEscapeGameUtil();
		} catch (Exception e) {
			e.printStackTrace();
			e.getCause().printStackTrace();
		}
		if (sponsor != null && zec != null && zegu != null) {
			Bukkit.getPluginCommand("setsponsor").setExecutor(sponsor.new SetSponsor());
			Bukkit.getPluginCommand("removesponsor").setExecutor(sponsor.new RemoveSponsor());
			Bukkit.getPluginCommand("setspawn").setExecutor(zec.new SetSpawn());
			Bukkit.getPluginCommand("removespawn").setExecutor(zec.new RemoveSpawn());
			Bukkit.getPluginCommand("suicide").setExecutor(zegu.new Suicide());
			Bukkit.getPluginCommand("setcp").setExecutor(zegu.new SetCheckpoint());
			Bukkit.getPluginCommand("startgame").setExecutor(zegu.new StartGame());
			Bukkit.getPluginCommand("endgame").setExecutor(zegu.new EndGame());
			Bukkit.getPluginCommand("zombieescape").setExecutor(new CommandExecutor() {
				@Override
				public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
					if (!(sender instanceof Player)) {
						sender.sendMessage(ChatColor.RED + "This command must be run from in-game.");
						return true;
					}
					((Player) sender).performCommand("bukkit:help ZombieEscape");
					return true;
				}
			});
		} else {
			Bukkit.getLogger().severe("[ZombieEscape] Unable to register commands! Commands are disabled.");
		}
		teams.put("zombie", manager.getNewScoreboard().registerNewTeam("zombie"));
		teams.put("player", manager.getNewScoreboard().registerNewTeam("player"));
		Bukkit.getPluginManager().registerEvents(this, this);
		checkConfig();
		Bukkit.getLogger().info("[ZombieEscape] Enabled Zombie Escape");
	}

	/**
	 * Reload config and check if all configs are set correctly.<br>
	 * If all checks passed, settingsCheck will be true. Otherwise it'll set to false.
	 */
	public static void checkConfig() {
		config.reloadWithoutException();
		if (config.getList("spawnPoints.zombie") != null // check if zombie spawn points exists
				&& config.getList("spawnPoints.player") != null // check if player spawn points exists
				&& config.getString("spawnPoints.world") != null // check if spawn world is set
				&& Bukkit.getWorld(config.getString("spawnPoints.world")) != null) settingsCheck = true;
		else settingsCheck = false;
	}

	@EventHandler
	public void onPlayerJoin(final PlayerJoinEvent event) {
		new BukkitRunnable() {
			public void run() {
				for (Player player : Bukkit.getOnlinePlayers()) {
					if (player.getHealth() <= 0) {
						player.setHealth(0.0);
					}
					if (player.isDead()) player.spigot().respawn();
				}
			}
		}.runTaskTimer(this, 0, 1000);
		if (!settingsCheck) event.getPlayer().sendMessage(ChatColor.RED + "設定がまだ完了してない/エラーが発生したため、ゲームを開始できません。");
		hashMapLastScore4.put(event.getPlayer().getUniqueId(), "");
		hashMapLastScore8.put(event.getPlayer().getUniqueId(), "");
		event.getPlayer().getWorld().setGameRuleValue("doMobLoot", "false");
		event.getPlayer().getWorld().setGameRuleValue("doDaylightCycle", "false");
		event.getPlayer().getWorld().setGameRuleValue("keepInventory", "true");
		event.getPlayer().getWorld().setTime(0);
		event.getPlayer().getInventory().clear();
		event.getPlayer().setGameMode(GameMode.ADVENTURE);
		event.getPlayer().addPotionEffect(PotionEffectType.SATURATION.createEffect(100000, 10));
		final Scoreboard board = manager.getNewScoreboard();
		String team = zombies <= players ? "zombie" : "player";
		teams.get(team).setAllowFriendlyFire(false);
		teams.get(team).addEntry(event.getPlayer().getName());
		event.getPlayer().setScoreboard(board);
		final Objective objective = board.registerNewObjective("scoreboard", "dummy");
		hashMapTeam.put(event.getPlayer().getUniqueId(), team);
		Score score7 = objective.getScore(" ");
		score7.setScore(7);
		Score score5 = objective.getScore("  ");
		score5.setScore(5);
		if (zombies <= players == true) {
			zombies = zombies+1;
			Score score6 = objective.getScore(ChatColor.GREEN + "    チーム: " + ChatColor.DARK_GREEN + "ゾンビ");
			score6.setScore(6);
			event.getPlayer().setMaxHealth(100);
			event.getPlayer().setHealth(100);
			event.getPlayer().getInventory().setHelmet(createLeatherItemStack(Material.LEATHER_HELMET, 0, 100, 0));
			event.getPlayer().getInventory().setChestplate(createLeatherItemStack(Material.LEATHER_CHESTPLATE, 0, 100, 0));
			event.getPlayer().getInventory().setLeggings(createLeatherItemStack(Material.LEATHER_LEGGINGS, 0, 100, 0));
			event.getPlayer().getInventory().setBoots(createLeatherItemStack(Material.LEATHER_BOOTS, 0, 100, 0));
			event.getPlayer().addPotionEffect(PotionEffectType.INCREASE_DAMAGE.createEffect(100000, 100));
			event.getPlayer().setPlayerListName(ChatColor.DARK_GREEN + event.getPlayer().getName());
		} else if (zombies <= players == false) {
			players = players+1;
			event.getPlayer().getInventory().setHelmet(new ItemStack(Material.DIAMOND_HELMET));
			event.getPlayer().getInventory().setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
			event.getPlayer().getInventory().setLeggings(new ItemStack(Material.DIAMOND_LEGGINGS));
			event.getPlayer().getInventory().setBoots(new ItemStack(Material.DIAMOND_BOOTS));
			event.getPlayer().removePotionEffect(PotionEffectType.INCREASE_DAMAGE);
			Score score6 = objective.getScore(ChatColor.GREEN + "    チーム: " + ChatColor.AQUA + "プレイヤー");
			score6.setScore(6);
			event.getPlayer().setMaxHealth(1);
			event.getPlayer().setHealth(1);
			event.getPlayer().setPlayerListName(ChatColor.AQUA + event.getPlayer().getName());
		} else {
			throw new IllegalStateException("Impossible state has occurred");
		}
		objective.setDisplaySlot(DisplaySlot.SIDEBAR);
		objective.setDisplayName(""+ChatColor.GREEN + ChatColor.BOLD + "Zombie Escape");
		if (Bukkit.getOnlinePlayers().size() >= 2) hasEnoughPlayers = true; else {
			hasEnoughPlayers = false;
			timesLeft = 180;
		}
		hashMapScoreboard.put(event.getPlayer().getUniqueId(), board);
		if (timerStarted) return;
		timerStarted = true;
		Timer timer = new Timer();
		TimerTask timerTask = new TimerTask() {
			@SuppressWarnings("deprecation") // player#sendTitle, i can't find non-deprecated methods in 1.8.8.
			public void run() {
				final String zombieMessage = ChatColor.GREEN + "    チーム: " + ChatColor.DARK_GREEN + "ゾンビ";
				final String playerMessage = ChatColor.GREEN + "    チーム: " + ChatColor.AQUA + "プレイヤー";
				if (!gameStarted) {
					for (final Player player : Bukkit.getOnlinePlayers()) {
						Scoreboard scoreboard = hashMapScoreboard.get(player.getUniqueId());
						Objective objective3 = scoreboard.getObjective(DisplaySlot.SIDEBAR);
						String lastScore8 = hashMapLastScore8.get(player.getUniqueId());
						scoreboard.resetScores(lastScore8);
						lastScore8 = ChatColor.GREEN + "    プレイヤー: " + Bukkit.getOnlinePlayers().size() + "/" + Bukkit.getMaxPlayers();
						Score score8 = objective3.getScore(lastScore8);
						score8.setScore(8);
						String leftSecond = Integer.toString(timesLeft % 60);
						if (leftSecond.length() == 1) leftSecond = "0" + leftSecond;
						String lastScore4 = hashMapLastScore4.get(player.getUniqueId());
						scoreboard.resetScores(lastScore4);
						if (hasEnoughPlayers && settingsCheck)
							lastScore4 = ChatColor.GREEN + "    あと" + Math.round(Math.nextDown(timesLeft/60)) + ":" + leftSecond + "で開始";
						else
							lastScore4 = ChatColor.WHITE + "    待機中...";
						hashMapLastScore8.put(player.getUniqueId(), lastScore8);
						hashMapLastScore4.put(player.getUniqueId(), lastScore4);
						Score score4 = objective3.getScore(lastScore4);
						score4.setScore(4);
						scoreboard.resetScores(zombieMessage);
						scoreboard.resetScores(playerMessage);
						if (hashMapTeam.get(player.getUniqueId()) == "zombie") {
							Score score6 = objective3.getScore(zombieMessage);
							score6.setScore(6);
						} else if (hashMapTeam.get(player.getUniqueId()) == "player") {
							Score score6 = objective3.getScore(playerMessage);
							score6.setScore(6);
						}
						player.setScoreboard(hashMapScoreboard.get(player.getUniqueId()));
						if (timesLeft == 5) {
							player.playSound(player.getLocation(), Sound.NOTE_STICKS, 100, 1);
							player.sendTitle(ChatColor.GREEN + "5", "");
						} else if (timesLeft == 4) {
							player.playSound(player.getLocation(), Sound.NOTE_STICKS, 100, 1);
							player.sendTitle(ChatColor.AQUA + "4", "");
						} else if (timesLeft == 3) {
							player.playSound(player.getLocation(), Sound.NOTE_STICKS, 100, 1);
							player.sendTitle(ChatColor.BLUE + "3", "");
						} else if (timesLeft == 2) {
							player.playSound(player.getLocation(), Sound.NOTE_STICKS, 100, 1);
							player.sendTitle(ChatColor.YELLOW + "2", "");
						} else if (timesLeft == 1) {
							player.playSound(player.getLocation(), Sound.NOTE_STICKS, 100, 1);
							player.sendTitle(ChatColor.RED + "1", "");
						} else if (timesLeft == 0) {
							gameStarted = true;
							player.playSound(player.getLocation(), Sound.ENDERDRAGON_GROWL, 100F, 1F);
							if (hashMapTeam.get(player.getUniqueId()) == "zombie") {
								player.sendTitle("" + ChatColor.GREEN + ChatColor.BOLD + "GO!", ChatColor.YELLOW + "目標: プレイヤーをすべて倒す");
								player.sendMessage(ChatColor.GRAY + "あと10秒後にワープします...");
								new BukkitRunnable() {
									@Override
									public void run() {
										player.setGameMode(GameMode.ADVENTURE);
										player.addPotionEffect(PotionEffectType.SPEED.createEffect(100000, 1));
										config.reloadWithoutException();
										String[] spawnLists = Arrays.asList(config.getList("spawnPoints.zombie", new ArrayList<String>()).toArray(new String[0])).get(0).split(",");
										Location location = new Location(Bukkit.getWorld(config.getString("spawnPoints.world")), Double.parseDouble(spawnLists[0]), Double.parseDouble(spawnLists[1]), Double.parseDouble(spawnLists[2]));
										if (!player.teleport(location)) {
											player.sendMessage(ChatColor.RED + "ワープに失敗しました。");
											return;
										}
									}
								}.runTask(new ZombieEscape());
							} else if (hashMapTeam.get(player.getUniqueId()) == "player") {
								player.performCommand("shot give " + player.getName() + " ak-47");
								player.performCommand("shot give " + player.getName() + " hunting");
								player.performCommand("shot give " + player.getName() + " carbine");
								player.performCommand("shot give " + player.getName() + " deagle");
								player.performCommand("shot give " + player.getName() + " olympia");
								player.performCommand("shot give " + player.getName() + " python");
								player.sendTitle("" + ChatColor.GREEN + ChatColor.BOLD + "GO!", ChatColor.YELLOW + "目標: ゾンビを倒し、ゾンビから逃げる");
								config.reloadWithoutException();
								String[] spawnLists = Arrays.asList(config.getList("spawnPoints.player", new ArrayList<String>()).toArray(new String[0])).get(0).split(",");
								Location location = new Location(Bukkit.getWorld(config.getString("spawnPoints.world")), Double.parseDouble(spawnLists[0]), Double.parseDouble(spawnLists[1]), Double.parseDouble(spawnLists[2]));
								if (!player.teleport(location)) {
									player.sendMessage(ChatColor.RED + "ワープに失敗しました。");
									return;
								}
							}
						}
					}
					if (hasEnoughPlayers && timesLeft >= 0 && settingsCheck) timesLeft--;
				} else if (gameStarted) {
					if (playedTime >= gameTime) {
						for (Player player : Bukkit.getOnlinePlayers()) {
							player.playSound(player.getLocation(), Sound.FIREWORK_LAUNCH, 100, 1);
							player.sendTitle("" + ChatColor.GREEN + ChatColor.BOLD + "プレイヤーチームの勝ち！", "");
						}
						Bukkit.broadcastMessage("" + ChatColor.GREEN + ChatColor.BOLD + "プレイヤーチームの勝ち！");
						Bukkit.broadcastMessage(ChatColor.GRAY + "このサーバーはあと15秒でシャットダウンします。");
						TimerTask task = new TimerTask() {
							public void run() {
								Bukkit.broadcastMessage(ChatColor.GRAY + "サーバーをシャットダウン中...");
								Bukkit.shutdown();
							}
						};
						Timer timer = new Timer();
						timer.schedule(task, 1000*15);
						this.cancel();
						return;
					}
					for (Player player : Bukkit.getOnlinePlayers()) {
						Scoreboard scoreboard = hashMapScoreboard.get(player.getUniqueId());
						Objective objective3 = scoreboard.getObjective(DisplaySlot.SIDEBAR);
						String lastScore8 = hashMapLastScore8.get(player.getUniqueId());
						String lastScore4 = hashMapLastScore4.get(player.getUniqueId());
						if (gameTime == 1800) {
							scoreboard.resetScores(lastScore8);
							//board.resetScores(zombieMessage);
							//board.resetScores(playerMessage);
							scoreboard.resetScores(lastScore4);
						}
						String leftSecondPlayed = Integer.toString(playedTime % 60);
						if (leftSecondPlayed.length() == 1) leftSecondPlayed = "0" + leftSecondPlayed;
						String leftSecond = Integer.toString(gameTime % 60);
						if (leftSecond.length() == 1) leftSecond = "0" + leftSecond;
						lastScore4 = ChatColor.GREEN + "    " + Math.round(Math.nextDown(playedTime/60)) + ":" + leftSecondPlayed + " / " + Math.round(Math.nextDown(gameTime/60)) + ":" + leftSecond;
						Score score4 = objective3.getScore(lastScore4);
						score4.setScore(4);
						hashMapLastScore4.put(player.getUniqueId(), lastScore4);
						player.setScoreboard(hashMapScoreboard.get(player.getUniqueId()));
					}
					playedTime++;
				}
			}
		};
		timer.schedule(timerTask, 0, 1000);
	}

	@SuppressWarnings("deprecation")
	@EventHandler
	public void onPlayerDeath(final PlayerDeathEvent event) {
		if (event.getEntityType() != EntityType.PLAYER) return;
		event.getEntity().getInventory().clear();
		if (hashMapTeam.get(event.getEntity().getUniqueId()) == "player") players--;
		hashMapTeam.remove(event.getEntity().getUniqueId());
		hashMapTeam.put(event.getEntity().getUniqueId(), "zombie");
		final Objective objective = hashMapScoreboard.get(event.getEntity().getUniqueId()).getObjective("scoreboard");
		Score score6 = objective.getScore(ChatColor.GREEN + "    チーム: " + ChatColor.DARK_GREEN + "ゾンビ");
		objective.getScoreboard().resetScores(ChatColor.GREEN + "    チーム: " + ChatColor.AQUA + "プレイヤー");
		score6.setScore(6);
		new BukkitRunnable() {
			public void run() {
				event.getEntity().spigot().respawn();
			}
		}.runTaskLater(this, 1000);
		if (players == 0 && gameStarted) {
			for (Player player : Bukkit.getOnlinePlayers()) {
				new BukkitRunnable() {
					public void run() {
						if (fireworked == 20) this.cancel();
						player.playSound(player.getLocation(), Sound.FIREWORK_LAUNCH, 100, 1);
						player.sendTitle("" + ChatColor.GREEN + ChatColor.BOLD + "ゾンビチームの勝ち！", "");
						fireworked++;
					}
				}.runTaskTimer(this, 0, 250);
			}
			Bukkit.broadcastMessage("" + ChatColor.GREEN + ChatColor.BOLD + "ゾンビチームの勝ち！");
			Bukkit.broadcastMessage(ChatColor.GRAY + "このサーバーはあと15秒でシャットダウンします。");
			TimerTask task = new TimerTask() {
				public void run() {
					Bukkit.broadcastMessage(ChatColor.GRAY + "サーバーをシャットダウン中...");
					Bukkit.shutdown();
				}
			};
			Timer timer = new Timer();
			timer.schedule(task, 1000*15);
		}
	}

	@EventHandler
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		event.getPlayer().setGameMode(GameMode.ADVENTURE);
		event.getPlayer().addPotionEffect(PotionEffectType.SATURATION.createEffect(100000, 10));
		event.getPlayer().addPotionEffect(PotionEffectType.INCREASE_DAMAGE.createEffect(100000, 100));
		event.getPlayer().setPlayerListName(ChatColor.DARK_GREEN + event.getPlayer().getName());
		event.getPlayer().getInventory().setHelmet(createLeatherItemStack(Material.LEATHER_HELMET, 0, 100, 0));
		event.getPlayer().getInventory().setChestplate(createLeatherItemStack(Material.LEATHER_CHESTPLATE, 0, 100, 0));
		event.getPlayer().getInventory().setLeggings(createLeatherItemStack(Material.LEATHER_LEGGINGS, 0, 100, 0));
		event.getPlayer().getInventory().setBoots(createLeatherItemStack(Material.LEATHER_BOOTS, 0, 100, 0));
		event.getPlayer().setMaxHealth(100);
		event.getPlayer().setHealth(100);
		config.reloadWithoutException();
		String[] spawnLists = Arrays.asList(config.getList("spawnPoints.zombie", new ArrayList<String>()).toArray(new String[0])).get(checkpoint).split(",");
		Location location = new Location(Bukkit.getWorld(config.getString("spawnPoints.world")), Double.parseDouble(spawnLists[0]), Double.parseDouble(spawnLists[1]), Double.parseDouble(spawnLists[2]));
		event.setRespawnLocation(location);
	}

	@EventHandler
	public void onPlayerHurt(EntityDamageByEntityEvent event) {
		if (event.getEntityType() != EntityType.PLAYER || event.getDamager().getType() != EntityType.PLAYER) return;
		if (!gameStarted) event.setCancelled(true);
		if (hashMapTeam.get(event.getDamager().getUniqueId()) == hashMapTeam.get(event.getEntity().getUniqueId())) event.setCancelled(true);
	}

	@EventHandler
	public void onEntityDamage(EntityDamageEvent event) {
		if (event.getEntityType() != EntityType.PLAYER) return;
		if (!gameStarted) event.setCancelled(true);
		if (event.getCause() == DamageCause.FALL) event.setCancelled(true);
	}

	@EventHandler
	public void onPlayerLeft(PlayerQuitEvent event) {
		if (Bukkit.getOnlinePlayers().size() >= 2) hasEnoughPlayers = true; else {
			hasEnoughPlayers = false;
			timesLeft = 180;
		}
		if (hashMapTeam.get(event.getPlayer().getUniqueId()) == "zombie") zombies--; else players--;
		if (zombies < 0) throw new IllegalStateException("Zombie count is should be 0 or more.");
		if (players < 0) throw new IllegalStateException("Player count is should be 0 or more.");
		hashMapTeam.remove(event.getPlayer().getUniqueId());
	}

	public ItemStack createLeatherItemStack(Material material, int red, int green, int blue) {
		ItemStack item = new ItemStack(material);
		LeatherArmorMeta lam = (LeatherArmorMeta) item.getItemMeta();
		lam.setColor(Color.fromRGB(red, green, blue));
		item.setItemMeta(lam);
		return item;
	}

	@EventHandler
	public void onProjectileHit(ProjectileHitEvent event) {
		Location location = new Location(
				event.getEntity().getLocation().getWorld(),
				Math.nextUp(event.getEntity().getLocation().getX()),
				Math.nextUp(event.getEntity().getLocation().getY()),
				Math.nextUp(event.getEntity().getLocation().getZ()+0.6));
		Bukkit.getLogger().info("[DEBUG] Location: " + location.getX() + ", " + location.getY() + ", " + location.getZ());
		Block block = event.getEntity().getWorld().getBlockAt(location);
		if (block == null) return;
		if (block.getType() == Material.DIRT || block.getType() == Material.GRASS || block.getType() == Material.WOOD) {
			Integer state = hashMapBlockState.get(block.getLocation()) != null ? hashMapBlockState.get(block.getLocation()) : 0;
			if (state >= 3) {
				block.setType(Material.AIR);
				hashMapBlockState.remove(block.getLocation());
				PacketContainer packet1 = protocol.createPacket(PacketType.Play.Server.BLOCK_BREAK_ANIMATION);
				packet1.getBlockPositionModifier().write(0, new BlockPosition(block.getX(), block.getY()-1, block.getZ()));
				packet1.getIntegers().write(0, new Random().nextInt(2000));
				packet1.getIntegers().write(1, 0); // remove animation
				for (Player player : Bukkit.getOnlinePlayers()) {
					try {
						protocol.sendServerPacket(player, packet1);
					} catch (InvocationTargetException e) {
						e.printStackTrace();
					}
				}
				return;
			}
			hashMapBlockState.put(block.getLocation(), state+1);
			PacketContainer packet1 = protocol.createPacket(PacketType.Play.Server.BLOCK_BREAK_ANIMATION);
			packet1.getBlockPositionModifier().write(0, new BlockPosition(block.getX(), block.getY(), block.getZ()));
			packet1.getIntegers().write(0, new Random().nextInt(2000));
			packet1.getIntegers().write(1, (state+1)*3);
			for (Player player : Bukkit.getOnlinePlayers()) {
				try {
					protocol.sendServerPacket(player, packet1);
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				}
			}
		}
	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		Block block = event.getBlock();
		if (block == null) return;
		hashMapBlockState.remove(block.getLocation());
		PacketContainer packet1 = protocol.createPacket(PacketType.Play.Server.BLOCK_BREAK_ANIMATION);
		packet1.getBlockPositionModifier().write(0, new BlockPosition(block.getX(), block.getY(), block.getZ()));
		packet1.getIntegers().write(0, new Random().nextInt(2000));
		packet1.getIntegers().write(1, 0); // remove animation
		for (Player player : Bukkit.getOnlinePlayers()) {
			try {
				protocol.sendServerPacket(player, packet1);
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		}
	}

	@EventHandler(priority=EventPriority.LOWEST)
	public void onPlayerLogin(PlayerLoginEvent event) {
		if (Bukkit.getOnlinePlayers().size() < Bukkit.getMaxPlayers()) {
			event.allow();
			return;
		}
		try {
			ConfigProvider config = new ConfigProvider("./plugins/ZombieEscape/config.yml");
			List<String> sponsors = Arrays.asList(config.getList("sponsors", new ArrayList<String>()).toArray(new String[0]));
			if (sponsors.contains(event.getPlayer().getUniqueId().toString()) == true) {
				event.allow();
			} else if (sponsors.contains(event.getPlayer().getUniqueId().toString()) == false) {
				event.disallow(Result.KICK_OTHER, ChatColor.RED + "満員のサーバーに参加するにはスポンサーが必要です！");
			}
		} catch (Exception e) {
			event.disallow(Result.KICK_OTHER, ChatColor.RED + "設定の読み込みに失敗しました。あとでやり直してください。");
		}
	}

	@EventHandler(priority=EventPriority.HIGHEST)
	public void onPlayerChat(AsyncPlayerChatEvent event) {
		if (event.getMessage().equalsIgnoreCase("gg") || event.getMessage().equalsIgnoreCase("good game")) {
			event.setMessage(ChatColor.GOLD + event.getMessage());
		}
		if (hashMapTeam.get(event.getPlayer().getUniqueId()) == "zombie") {
			event.setFormat(ChatColor.DARK_GREEN + "[Z] " + event.getPlayer().getName() + ChatColor.RESET + ChatColor.WHITE + ": " + event.getMessage());
		} else {
			event.setFormat(ChatColor.AQUA + "[P] " + event.getPlayer().getName() + ChatColor.RESET + ChatColor.WHITE + ": " + event.getMessage());
		}
	}
}
