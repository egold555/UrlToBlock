package org.golde.bukkit.urltoblock;

import java.lang.reflect.Field;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.craftbukkit.v1_11_R1.block.CraftCreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.MetadataValue;

import net.minecraft.server.v1_11_R1.BlockMobSpawner;
import net.minecraft.server.v1_11_R1.MobSpawnerData;

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

	public void placeBlock(Player p, int x, int y, int z) {
		boolean isOrigOp = p.isOp();
		p.setOp(true);
		Bukkit.dispatchCommand(p, spawnerCommand(data, x, y, z));
		p.setOp(isOrigOp);
	}

	private String spawnerCommand(int data, int x, int y, int z) {
		return "setblock " + x + " " + y + " " + z + " minecraft:mob_spawner 0 replace {SpawnData:{id:minecraft:armor_stand,ArmorItems:[{},{},{},{}],HandItems:[{id:minecraft:diamond_hoe,Count:1,Damage:%d%,tag:{Unbreakable:1}},{}],Pose:{RightArm:[30f,0f,0f],LeftArm:[30f,0f,0f]}},RequiredPlayerRange:0.000001,MaxNearbyEntities:0}".replace("%d%", String.valueOf(data));
	}
}
