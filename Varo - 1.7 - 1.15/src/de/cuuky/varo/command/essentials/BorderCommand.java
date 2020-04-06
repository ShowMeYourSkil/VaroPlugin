package de.cuuky.varo.command.essentials;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import de.cuuky.varo.Main;
import de.cuuky.varo.configuration.configurations.messages.ConfigMessages;
import de.cuuky.varo.game.world.VaroWorldHandler;
import de.cuuky.varo.version.BukkitVersion;
import de.cuuky.varo.version.VersionUtils;
import de.cuuky.varo.version.types.Sounds;

public class BorderCommand implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command arg1, String arg2, String[] args) {
		if(!VersionUtils.getVersion().isHigherThan(BukkitVersion.ONE_7)) {
			sender.sendMessage(Main.getPrefix() + "Nicht verfügbar vor der 1.8!");
			return false;
		}

		if(args.length == 0) {
			sender.sendMessage(Main.getPrefix() + "§7Die Border ist " + Main.getColorCode() + (sender instanceof Player ? Main.getVaroGame().getVaroWorldHandler().getVaroWorld(((Player) sender).getWorld()).getVaroBorder().getBorderSize() : Main.getVaroGame().getVaroWorldHandler().getMainWorld().getVaroBorder().getBorderSize()) + " §7Blöcke groß!");
			if(sender instanceof Player)
				sender.sendMessage(Main.getPrefix() + "§7Du bist " + Main.getColorCode() + (int) Main.getVaroGame().getVaroWorldHandler().getVaroWorld(((Player) sender).getWorld()).getVaroBorder().getBorderDistanceTo((Player) sender) + "§7 Blöcke von der Border entfernt!");

			if(sender.hasPermission("varo.setup")) {
				sender.sendMessage(Main.getPrefix() + "§7Du kannst die Größe der Border mit " + Main.getColorCode() + "/border <Größe> [Sekunden] §7setzen!");
				sender.sendMessage(Main.getPrefix() + "§7Der Mittelpunkt der Border wird zu deinem derzeiten Punkt gesetzt");
			}
			return false;
		} else if(args.length >= 1 && sender.hasPermission("varo.setup")) {
			Player p = sender instanceof Player ? (Player) sender : null;
			int borderSize, inSeconds = -1;

			try {
				borderSize = Integer.parseInt(args[0]);
			} catch(NumberFormatException e) {
				p.sendMessage(Main.getPrefix() + "§7Das ist keine Zahl!");
				return false;
			}

			VaroWorldHandler worldHandler = Main.getVaroGame().getVaroWorldHandler();
			if(p != null)
				worldHandler.getVaroWorld(p.getWorld()).getVaroBorder().setBorderCenter(p.getLocation());
			try {
				inSeconds = Integer.parseInt(args[1]);
				worldHandler.setBorderSize(borderSize, inSeconds, p != null ? p.getWorld() : null);
			} catch(ArrayIndexOutOfBoundsException e) {
				worldHandler.setBorderSize(borderSize, 0, p != null ? p.getWorld() : null);
			} catch(NumberFormatException e) {
				sender.sendMessage(Main.getPrefix() + "§7Das ist keine Zahl!");
				return false;
			}

			sender.sendMessage(Main.getPrefix() + ConfigMessages.BORDER_COMMAND_SET_BORDER.getValue().replace("%size%", String.valueOf(borderSize)));
			if(p != null)
				p.playSound(p.getLocation(), Sounds.NOTE_BASS_DRUM.bukkitSound(), 1, 1);
		} else
			sender.sendMessage(ConfigMessages.NOPERMISSION_NO_PERMISSION.getValue());
		return false;
	}
}