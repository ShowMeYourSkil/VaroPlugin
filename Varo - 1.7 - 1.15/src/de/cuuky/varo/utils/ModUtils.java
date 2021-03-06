package de.cuuky.varo.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import de.cuuky.cfw.utils.JavaUtils;
import de.cuuky.varo.Main;
import de.cuuky.varo.configuration.configurations.language.languages.ConfigMessages;
import de.cuuky.varo.entity.player.VaroPlayer;

public class ModUtils {

	private static Method getModList;

	static {
		try {
			Class<?> playerApiClass = Class.forName("org.magmafoundation.magma.api.PlayerAPI");
			getModList = playerApiClass.getMethod("getModlist", Player.class);
		} catch (NoSuchMethodException | ClassNotFoundException e) {}
	}

	public static void checkForIllegalMods(Player player) {
		if (getModList == null)
			return;

		boolean kickPlayer = false;
		String modList = null;
		try {
			modList = (String) getModList.invoke(null, player);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			return;
		}

		ArrayList<String> usedMods = new ArrayList<String>();
		for (String mod : Main.getDataManager().getListManager().getBlockedMods().getAsList()) {
			if (modList.toLowerCase().contains(mod.toLowerCase())) {
				kickPlayer = true;
				usedMods.add(mod);
			}
			if (kickPlayer) {
				String illegalMods = JavaUtils.getArgsToString(usedMods, "§7, " + Main.getColorCode());
				if (!player.hasPermission("varo.alwaysjoin")) {
					player.kickPlayer(Main.getPrefix() + ConfigMessages.MODS_BLOCKED_MODS_KICK.getValue(VaroPlayer.getPlayer(player)).replace("%mods%", illegalMods));
				}
				for (Player p : Bukkit.getOnlinePlayers()) {
					VaroPlayer.getPlayer(p).sendMessage(Main.getPrefix() + ConfigMessages.MODS_BLOCKED_MODS_BROADCAST.getValue().replace("%mods%", illegalMods).replace("%player%", player.getName()));
				}
			}
		}
	}
}