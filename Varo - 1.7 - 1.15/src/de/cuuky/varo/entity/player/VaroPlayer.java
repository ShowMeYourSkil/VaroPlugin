package de.cuuky.varo.entity.player;

import java.util.ArrayList;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import de.cuuky.cfw.clientadapter.board.nametag.CustomNametag;
import de.cuuky.cfw.clientadapter.board.scoreboard.CustomScoreboard;
import de.cuuky.cfw.clientadapter.board.tablist.CustomTablist;
import de.cuuky.cfw.player.CustomPlayer;
import de.cuuky.cfw.player.connection.NetworkManager;
import de.cuuky.cfw.utils.JavaUtils;
import de.cuuky.cfw.version.BukkitVersion;
import de.cuuky.cfw.version.VersionUtils;
import de.cuuky.varo.Main;
import de.cuuky.varo.alert.Alert;
import de.cuuky.varo.alert.AlertType;
import de.cuuky.varo.bot.discord.VaroDiscordBot;
import de.cuuky.varo.bot.discord.register.BotRegister;
import de.cuuky.varo.configuration.configurations.config.ConfigSetting;
import de.cuuky.varo.configuration.configurations.messages.language.languages.defaults.ConfigMessages;
import de.cuuky.varo.entity.VaroEntity;
import de.cuuky.varo.entity.player.event.BukkitEvent;
import de.cuuky.varo.entity.player.event.BukkitEventType;
import de.cuuky.varo.entity.player.stats.Stats;
import de.cuuky.varo.entity.player.stats.stat.PlayerState;
import de.cuuky.varo.entity.player.stats.stat.Rank;
import de.cuuky.varo.entity.player.stats.stat.offlinevillager.OfflineVillager;
import de.cuuky.varo.entity.team.VaroTeam;
import de.cuuky.varo.event.VaroEvent;
import de.cuuky.varo.event.VaroEventType;
import de.cuuky.varo.game.lobby.LobbyItem;
import de.cuuky.varo.logger.logger.EventLogger.LogType;
import de.cuuky.varo.serialize.identifier.VaroSerializeField;
import de.cuuky.varo.vanish.Vanish;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

public class VaroPlayer extends VaroEntity implements CustomPlayer {

	private static ArrayList<VaroPlayer> varoplayer;

	static {
		varoplayer = new ArrayList<>();
	}

	@VaroSerializeField(path = "id")
	private int id;

	@VaroSerializeField(path = "name")
	private String name;

	@VaroSerializeField(path = "uuid")
	private String uuid;

	@VaroSerializeField(path = "locale")
	private String locale;

	@VaroSerializeField(path = "adminIgnore")
	private boolean adminIgnore;

	@VaroSerializeField(path = "villager")
	private OfflineVillager villager;

	@VaroSerializeField(path = "rank")
	private Rank rank;

	@VaroSerializeField(path = "stats")
	private Stats stats;

	private CustomNametag nametag;
	private CustomScoreboard scoreboard;
	private CustomTablist tablist;
	private NetworkManager networkManager;

	private VaroTeam team;
	private Player player;
	private boolean alreadyHadMassProtectionTime, inMassProtectionTime, massRecordingKick;

	public VaroPlayer() {
		varoplayer.add(this);
	}

	public VaroPlayer(Player player) {
		this.name = player.getName();
		this.uuid = player.getUniqueId().toString();
		this.player = player;
		this.id = generateId();
		this.adminIgnore = false;

		this.stats = new Stats(this);
	}

	public VaroPlayer(String playerName, String uuid) {
		this.name = playerName;
		this.uuid = uuid;

		this.adminIgnore = false;
		this.id = generateId();

		varoplayer.add(this);
		this.stats = new Stats(this);
		stats.loadDefaults();
	}

	private int generateId() {
		int id = JavaUtils.randomInt(1000, 9999999);
		while (getPlayer(id) != null)
			generateId();

		return id;
	}

	private void updateDiscordTeam(VaroTeam oldTeam) {
		VaroDiscordBot db = Main.getBotLauncher().getDiscordbot();
		if (db == null || !db.isEnabled())
			return;

		BotRegister reg = BotRegister.getBotRegisterByPlayerName(name);
		if (reg == null)
			return;

		Member member = reg.getMember();
		if (member == null)
			return;

		if (oldTeam != null) {
			if (db.getMainGuild().getRolesByName("#" + oldTeam.getName(), true).size() > 0) {
				Role role = db.getMainGuild().getRolesByName("#" + oldTeam.getName(), true).get(0);
				db.getMainGuild().removeRoleFromMember(member, role).complete();
			}
		}

		if (this.team != null) {
			Role role = db.getMainGuild().getRolesByName("#" + team.getName(), true).size() > 0 ? db.getMainGuild().getRolesByName("#" + team.getName(), true).get(0) : null;
			if (role == null)
				role = db.getMainGuild().createCopyOfRole(db.getMainGuild().getPublicRole()).setHoisted(true).setName("#" + team.getName()).complete();

			db.getMainGuild().addRoleToMember(member, role).complete();
		}
	}

	/**
	 * @return Returns if a player is nearby
	 */
	public boolean canBeKicked(int noKickDistance) {
		if (noKickDistance < 1)
			return true;

		for (Entity entity : player.getNearbyEntities(noKickDistance, noKickDistance, noKickDistance)) {
			if (!(entity instanceof Player))
				continue;

			VaroPlayer vp = getPlayer((Player) entity);
			if (vp.equals(this))
				continue;

			if (vp.getTeam() != null)
				if (vp.getTeam().equals(team))
					continue;

			if (vp.getStats().isSpectator() || vp.isAdminIgnore())
				continue;

			return false;
		}

		return true;
	}

	public void cleanUpPlayer() {
		player.setHealth(20);
		player.setFoodLevel(20);
		player.getInventory().clear();
		player.getInventory().setArmorContents(new ItemStack[] {});
		player.setExp(0);
		player.setLevel(0);
	}

	public void delete() {
		if (team != null)
			team.removeMember(this);

		if (rank != null)
			rank.remove();

		if (isOnline())
			player.kickPlayer(ConfigMessages.JOIN_KICK_NOT_USER_OF_PROJECT.getValue(this, this));

		if (villager != null)
			villager.remove();

		stats.remove();
		varoplayer.remove(this);
		Main.getVaroGame().getTopScores().update();
	}

	@Override
	public void onDeserializeEnd() {
		this.player = Bukkit.getPlayer(getRealUUID()) != null ? Bukkit.getPlayer(getRealUUID()) : null;
		if (this.player != null)
			setPlayer(this.player);
		if (isOnline()) {
			if (getStats().isSpectator() || isAdminIgnore())
				setSpectacting();

			setNormalAttackSpeed();
			LobbyItem.giveItems(player);
		} else if (isAdminIgnore())
			adminIgnore = false;

		this.stats.setOwner(this);
	}

	public void onEvent(BukkitEventType type) {
		new BukkitEvent(this, type);
	}

	@Override
	public void onSerializeStart() {}

	public void register() {
		if (this.stats == null)
			this.stats = new Stats(this);

		stats.loadDefaults();
		varoplayer.add(this);
	}

	public String getPrefix() {
		String pr = "";
		if (team != null)
			pr = team.getDisplay() + " ";

		if (rank != null)
			pr = rank.getDisplay() + (pr.isEmpty() ? " " : " §8| ") + pr;

		return pr;
	}

	public void setSpectacting() {
		Bukkit.getScheduler().scheduleSyncDelayedTask(Main.getInstance(), new Runnable() {

			@Override
			public void run() {
				new Vanish(player.getPlayer());
				player.getPlayer().setGameMode(GameMode.ADVENTURE);
				player.getPlayer().setAllowFlight(true);
				player.getPlayer().setFlying(true);
				player.getPlayer().setHealth(20);
				player.getPlayer().setFoodLevel(20);

				if (!adminIgnore) {
					player.getInventory().clear();
					player.getInventory().setArmorContents(new ItemStack[] {});
				}
			}
		}, 1);
	}

	public void update() {
		if (VersionUtils.getVersion().isHigherThan(BukkitVersion.ONE_7))
			if (ConfigSetting.TABLIST.getValueAsBoolean())
				this.tablist.update();

		if (ConfigSetting.NAMETAGS_ENABLED.getValueAsBoolean())
			this.nametag.update();

		if (ConfigSetting.SCOREBOARD.getValueAsBoolean())
			this.scoreboard.update();
	}

	public boolean getalreadyHadMassProtectionTime() {
		return alreadyHadMassProtectionTime;
	}

	public int getId() {
		return id;
	}

	public boolean getinMassProtectionTime() {
		return inMassProtectionTime;
	}

	public String getName() {
		return name;
	}

	public CustomNametag getNametag() {
		return this.nametag;
	}

	public CustomScoreboard getScoreboard() {
		return this.scoreboard;
	}

	@Override
	public NetworkManager getNetworkManager() {
		return networkManager;
	}

	@Override
	public String getUUID() {
		return this.uuid;
	}

	@Override
	public String getLocale() {
		return this.networkManager != null && this.networkManager.getLocale() != null ? this.networkManager.getLocale() : this.locale;
	}

	@Override
	public Player getPlayer() {
		return player;
	}

	public Rank getRank() {
		return rank;
	}

	public UUID getRealUUID() {
		return UUID.fromString(uuid);
	}

	public Stats getStats() {
		return stats;
	}

	public VaroTeam getTeam() {
		return team;
	}

	public OfflineVillager getVillager() {
		return villager;
	}

	public boolean isAdminIgnore() {
		return adminIgnore;
	}

	public boolean isInProtection() {
		if (VaroEvent.getEvent(VaroEventType.MASS_RECORDING).isEnabled()) {
			return inMassProtectionTime;
		} else {
			return ConfigSetting.PLAY_TIME.isIntActivated() && stats.getCountdown() >= (ConfigSetting.PLAY_TIME.getValueAsInt() * 60) - ConfigSetting.JOIN_PROTECTIONTIME.getValueAsInt() && Main.getVaroGame().isRunning() && !Main.getVaroGame().isFirstTime() && ConfigSetting.JOIN_PROTECTIONTIME.isIntActivated() && !isAdminIgnore();
		}
	}

	public boolean isMassRecordingKick() {
		return massRecordingKick;
	}

	/**
	 * @return Returns if the Player is online
	 */
	public boolean isOnline() {
		return player != null;
	}

	public boolean isRegistered() {
		return varoplayer.contains(this);
	}

	public void sendMessage(String message) {
		player.sendMessage(message);
	}

	public void setAdminIgnore(boolean adminIgnore) {
		this.adminIgnore = adminIgnore;
	}

	public void setalreadyHadMassProtectionTime(boolean alreadyHadMassProtectionTime) {
		this.alreadyHadMassProtectionTime = alreadyHadMassProtectionTime;
	}

	public void setinMassProtectionTime(boolean inMassProtectionTime) {
		this.inMassProtectionTime = inMassProtectionTime;
	}

	public void setMassRecordingKick(boolean massRecordingKick) {
		this.massRecordingKick = massRecordingKick;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setNormalAttackSpeed() {
		getNetworkManager().setAttributeSpeed(!ConfigSetting.REMOVE_HIT_COOLDOWN.getValueAsBoolean() ? 4D : 100D);
	}

	public void setPlayer(Player player) {
		this.player = player;

		if (player != null) {
			this.networkManager = new NetworkManager(player);
			this.scoreboard = (CustomScoreboard) Main.getCuukyFrameWork().getClientAdapterManager().registerBoard(new CustomScoreboard(this));
			this.nametag = (CustomNametag) Main.getCuukyFrameWork().getClientAdapterManager().registerBoard(new CustomNametag(this));
			this.tablist = (CustomTablist) Main.getCuukyFrameWork().getClientAdapterManager().registerBoard(new CustomTablist(this));
		} else {
			this.scoreboard.remove();
			this.nametag.remove();
			this.tablist.remove();

			this.networkManager = null;
			this.scoreboard = null;
			this.nametag = null;
			this.tablist = null;
		}
	}

	public void setRank(Rank rank) {
		this.rank = rank;

		if (isOnline())
			update();
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public void setVillager(OfflineVillager villager) {
		this.villager = villager;
	}

	public void setTeam(VaroTeam team) {
		VaroTeam oldTeam = this.team;
		this.team = team;

		if (!Main.isBootedUp())
			return;

		try {
			if (ConfigSetting.DISCORDBOT_SET_TEAM_AS_GROUP.getValueAsBoolean()) {
				if (Main.getBotLauncher() == null)
					Bukkit.getScheduler().scheduleAsyncDelayedTask(Main.getInstance(), new Runnable() {

						@Override
						public void run() {
							updateDiscordTeam(oldTeam);
						}
					}, 1);
				else
					updateDiscordTeam(oldTeam);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (isOnline())
			update();

		Main.getVaroGame().getTopScores().update();
	}

	/**
	 * @return Returns all alive Players regardless if they are online
	 */
	public static ArrayList<VaroPlayer> getAlivePlayer() {
		ArrayList<VaroPlayer> alive = new ArrayList<>();
		for (VaroPlayer vp : varoplayer) {
			if (!vp.getStats().isAlive())
				continue;

			alive.add(vp);
		}

		return alive;
	}

	public static ArrayList<VaroPlayer> getDeadPlayer() {
		ArrayList<VaroPlayer> dead = new ArrayList<>();
		for (VaroPlayer vp : varoplayer) {
			if (vp.getStats().getState() != PlayerState.DEAD)
				continue;

			dead.add(vp);
		}

		return dead;
	}

	public static ArrayList<VaroPlayer> getOnlineAndAlivePlayer() {
		ArrayList<VaroPlayer> online = new ArrayList<>();
		for (VaroPlayer vp : varoplayer) {
			if (!vp.isOnline() || !vp.getStats().isAlive())
				continue;

			online.add(vp);
		}

		return online;
	}

	/**
	 * @return Returns all online VaroPlayers regardless if they are alive
	 */
	public static ArrayList<VaroPlayer> getOnlinePlayer() {
		ArrayList<VaroPlayer> online = new ArrayList<>();
		for (VaroPlayer vp : varoplayer) {
			if (!vp.isOnline())
				continue;

			online.add(vp);
		}

		return online;
	}

	public static VaroPlayer getPlayer(int id) {
		for (VaroPlayer vp : varoplayer) {
			if (vp.getId() != id)
				continue;

			return vp;
		}

		return null;
	}

	/**
	 * @return Returns the varoplayer and sets the name right if the player
	 *         changed it before
	 */
	public static VaroPlayer getPlayer(Player player) {
		for (VaroPlayer vp : varoplayer) {
			if (vp.getUUID() != null)
				if (!vp.getUUID().equals(player.getUniqueId().toString()))
					continue;

			if (vp.getUUID() == null && player.getName().equalsIgnoreCase(vp.getName()))
				vp.setUuid(player.getUniqueId().toString());
			else if (vp.getUUID() == null)
				continue;

			if (!vp.getName().equalsIgnoreCase(player.getName())) {
				Main.getDataManager().getVaroLoggerManager().getEventLogger().println(LogType.ALERT, ConfigMessages.ALERT_SWITCHED_NAME.getValue(null, vp).replace("%newName%", player.getName()));
				Bukkit.broadcastMessage("§c" + vp.getName() + " §7hat seinen Namen gewechselt und ist nun unter §c" + player.getName() + " §7bekannt!");
				new Alert(AlertType.NAME_SWITCH, vp.getName() + " §7hat seinen Namen gewechselt und ist nun unter §c" + player.getName() + " §7bekannt!");
				vp.setName(player.getName());
			}

			return vp;
		}

		return null;
	}

	public static VaroPlayer getPlayer(String name) {
		for (VaroPlayer vp : varoplayer) {
			if (!vp.getName().equalsIgnoreCase(name))
				continue;

			return vp;
		}

		return null;
	}

	public static ArrayList<VaroPlayer> getSpectator() {
		ArrayList<VaroPlayer> spectator = new ArrayList<>();
		for (VaroPlayer vp : varoplayer) {
			if (!vp.getStats().isSpectator())
				continue;

			spectator.add(vp);
		}

		return spectator;
	}

	public static ArrayList<VaroPlayer> getVaroPlayer() {
		return varoplayer;
	}
}