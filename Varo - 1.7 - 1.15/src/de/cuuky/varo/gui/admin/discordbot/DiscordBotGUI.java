package de.cuuky.varo.gui.admin.discordbot;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

import de.cuuky.cfw.item.ItemBuilder;
import de.cuuky.cfw.menu.SuperInventory;
import de.cuuky.cfw.menu.utils.PageAction;
import de.cuuky.varo.Main;
import de.cuuky.varo.configuration.configurations.config.ConfigSetting;
import de.cuuky.varo.gui.admin.AdminMainMenu;

public class DiscordBotGUI extends SuperInventory {

	public DiscordBotGUI(Player opener) {
		super("§2DiscordBot", opener, 18, false);

		this.setModifier = true;
		Main.getCuukyFrameWork().getInventoryManager().registerInventory(this);
		open();
	}

	@Override
	public boolean onBackClick() {
		new AdminMainMenu(opener);
		return true;
	}

	@Override
	public void onClick(InventoryClickEvent event) {
		updateInventory();
	}

	@Override
	public void onClose(InventoryCloseEvent event) {}

	@Override
	public void onInventoryAction(PageAction action) {}

	@Override
	public boolean onOpen() {

		linkItemTo(1, new ItemBuilder().displayname(Main.getBotLauncher().getDiscordbot().isEnabled() ? "§cShutdown" : "§aStart").itemstack(new ItemStack(Main.getBotLauncher().getDiscordbot().isEnabled() ? Material.REDSTONE : Material.EMERALD)).build(), new Runnable() {

			@Override
			public void run() {
				boolean enabled = Main.getBotLauncher().getDiscordbot().isEnabled();
				if (enabled)
					Main.getBotLauncher().getDiscordbot().disconnect();
				else
					Main.getBotLauncher().getDiscordbot().connect();

				if (Main.getBotLauncher().getDiscordbot().isEnabled() == enabled)
					opener.sendMessage(Main.getPrefix() + "§7Could not start DiscordBot.");
				else
					opener.sendMessage(Main.getPrefix() + "§7Erfolg!");
			}
		});

		linkItemTo(7, new ItemBuilder().displayname("§eBotRegister").itemstack(new ItemStack(Material.BOOK)).build(), new Runnable() {

			@Override
			public void run() {
				if (Main.getBotLauncher().getDiscordbot().isEnabled() || !ConfigSetting.DISCORDBOT_VERIFYSYSTEM.getValueAsBoolean()) {
					opener.sendMessage(Main.getPrefix() + "Das System ist nicht aktiviert!");
					return;
				}
			}
		});

		return true;
	}
}
