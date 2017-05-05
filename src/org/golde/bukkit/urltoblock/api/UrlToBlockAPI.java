package org.golde.bukkit.urltoblock.api;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.golde.bukkit.urltoblock.Main;
import org.golde.bukkit.urltoblock.UrlBlock;

public class UrlToBlockAPI {

	private static Main main = Main.plugin;
	
	public static boolean isUrlBlock(Location loc) {
		return main.isUrlBlock(loc);
	}
	
	public static boolean isUrlBlockItem(ItemStack item) {
		return main.isUrlBlockItem(item);
	}
	
	public static UrlBlock getUrlBlockByID(short id) {
		return main.getBlockByDamageValue(id);
	}
	
}
