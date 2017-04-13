package org.golde.bukkit.urltoblock;

import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.golde.bukkit.urltoblock.NbtFactory.NbtCompound;

@SuppressWarnings("deprecation")
public class Block {

	public ItemStack block;
	public ItemStack hoeItem;
	public short damage;
	
	
	public Block(int data) {
		block = cmdBlockWithCommand(spawnerCommand(data), data);
		hoeItem = new ItemStack(Material.DIAMOND_HOE);
		hoeItem.setDurability((short)data);
		ItemMeta meta = hoeItem.getItemMeta();
		meta.setUnbreakable(true);
		meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);
		hoeItem.setItemMeta(meta);
		damage = (short)data;
	}
	
	private ItemStack cmdBlockWithCommand(String cmd, int id) {
		ItemStack item = new ItemStack(Material.COMMAND);
		
		NbtCompound sub = NbtFactory.createCompound();
		sub.put("auto", 1);
		sub.put("Command", cmd);
		NbtCompound main = NbtFactory.createCompound();
		main.put("BlockEntityTag", sub);
		item = NbtFactory.getCraftItemStack(item);
		NbtFactory.setItemTag(item, main);
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName("CustomBlock - ID: " + id);
		item.setItemMeta(meta);
		return item;
	}

	private String spawnerCommand(int data) {
		return "setblock ~ ~ ~ minecraft:mob_spawner 0 replace {SpawnData:{id:minecraft:armor_stand,ArmorItems:[{},{},{},{}],HandItems:[{id:minecraft:diamond_hoe,Count:1,Damage:%d%,tag:{Unbreakable:1}},{}],Pose:{RightArm:[30f,0f,0f],LeftArm:[30f,0f,0f]}},RequiredPlayerRange:0,MaxNearbyEntities:0}".replace("%d%", String.valueOf(data));
	}
	
}
