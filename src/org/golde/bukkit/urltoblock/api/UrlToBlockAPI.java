package org.golde.bukkit.urltoblock.api;

import org.bukkit.Location;
import org.golde.bukkit.urltoblock.Main;

public class UrlToBlockAPI {

	private static Main main = Main.plugin;
	
	public static boolean isUrlBlock(Location loc) {
		return main.isUrlBlock(loc);
	}
	
	@Deprecated
	public static void placeUrlBlock(Location loc, int id) {
		
	}
	
	@Deprecated
	public int[] getAllUrlBlocks() {
		return new int[] {};
	}
	
}
