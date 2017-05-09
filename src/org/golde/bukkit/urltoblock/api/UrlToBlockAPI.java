package org.golde.bukkit.urltoblock.api;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.entity.Player;
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
	
	public static UrlBlock getUrlBlockByID(int id) {
		return getUrlBlockByID((short)id);
	}
	public static UrlBlock getUrlBlockByID(short id) {
		return main.getBlockByDamageValue(id);
	}
	
	public static List<UrlBlock> getAllUrlBlocks(){
		return Main.plugin.blocks;
	}
	
	public static ItemStack fixAttackSpeed(Player p, ItemStack item) {
		return main.fixAttackSpeed(p, item);
	}
	
	public static void sendResourcePack(Player... players) {
		main.getResourcePack(players);
	}
	
	public static String getVersion() {
		return main.getDescription().getVersion();
	}
	
}
