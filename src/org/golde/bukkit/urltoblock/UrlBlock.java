package org.golde.bukkit.urltoblock;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class UrlBlock {

	private ItemStack handItem;
	private short damage;
	private int data;
	
	public UrlBlock(int data) {
		this.data = data;
		handItem = new ItemStack(Material.DIAMOND_AXE);
		handItem.setDurability((short)data);
		
		ItemMeta meta = handItem.getItemMeta();
		meta.setDisplayName(ChatColor.RESET + "UrlBlock - " + data);
		meta.setUnbreakable(true);
		meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);
		
		handItem.setItemMeta(meta);
		damage = (short)data;
	}
	
	public ItemStack getHandItem() {
		return handItem;
	}
	
	public ItemStack getGuiItem() {
		return handItem.clone();
	}
	
	public short getDamage() {
		return damage;
	}
	
	public void placeBlock(int x, int y, int z) {
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), spawnerCommand(data, x, y, z));
	}
	
	private String spawnerCommand(int data, int x, int y, int z) {
		return "setblock " + x + " " + y + " " + z + " minecraft:mob_spawner 0 replace {SpawnData:{id:minecraft:armor_stand,ArmorItems:[{},{},{},{}],HandItems:[{id:minecraft:diamond_hoe,Count:1,Damage:%d%,tag:{Unbreakable:1}},{}],Pose:{RightArm:[30f,0f,0f],LeftArm:[30f,0f,0f]}},RequiredPlayerRange:0,MaxNearbyEntities:0}".replace("%d%", String.valueOf(data));
	}
}
