package de.cuuky.varo.gui.saveable;

import java.util.ArrayList;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

import de.cuuky.cfw.item.ItemBuilder;
import de.cuuky.cfw.menu.SuperInventory;
import de.cuuky.cfw.menu.utils.PageAction;
import de.cuuky.cfw.utils.LocationFormat;
import de.cuuky.varo.Main;
import de.cuuky.varo.entity.player.VaroPlayer;
import de.cuuky.varo.entity.player.stats.stat.inventory.VaroSaveable;
import de.cuuky.varo.entity.player.stats.stat.inventory.VaroSaveable.SaveableType;
import de.cuuky.varo.gui.MainMenu;

public class PlayerSaveableChooseGUI extends SuperInventory {

	private VaroPlayer target;

	public PlayerSaveableChooseGUI(Player opener, VaroPlayer target) {
		super("§eÖfen/Kisten", opener, 54, false);

		this.target = target;

		this.setModifier = true;
		Main.getCuukyFrameWork().getInventoryManager().registerInventory(this);
		open();
	}

	@Override
	public boolean onBackClick() {
		new MainMenu(opener);
		return true;
	}

	@Override
	public void onClick(InventoryClickEvent event) {}

	@Override
	public void onClose(InventoryCloseEvent event) {}

	@Override
	public void onInventoryAction(PageAction action) {}

	@Override
	public boolean onOpen() {
		ArrayList<VaroSaveable> list = VaroSaveable.getSaveable(target);

		int start = getSize() * (getPage() - 1);
		for (int i = 0; i != getSize(); i++) {
			VaroSaveable saveable;
			try {
				saveable = list.get(start);
			} catch (IndexOutOfBoundsException e) {
				break;
			}

			linkItemTo(i, new ItemBuilder().displayname(Main.getColorCode() + String.valueOf(saveable.getId())).itemstack(new ItemStack(saveable.getType() == SaveableType.CHEST ? Material.CHEST : Material.FURNACE)).lore("§7Location§8: " + new LocationFormat(saveable.getBlock().getLocation()).format(Main.getColorCode() + "x§7, " + Main.getColorCode() + "y§7, " + Main.getColorCode() + "z§7 in " + Main.getColorCode() + "world")).build(), new Runnable() {

				@Override
				public void run() {
					new PlayerSaveableGUI(opener, saveable);
				}
			});
			start++;
		}

		return calculatePages(list.size(), getSize()) == page;
	}
}
