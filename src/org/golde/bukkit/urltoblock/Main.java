package org.golde.bukkit.urltoblock;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.function.Predicate;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_11_R1.block.CraftCreatureSpawner;
import org.bukkit.craftbukkit.v1_11_R1.entity.CraftPlayer;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.golde.bukkit.urltoblock.ChestGUI.OptionClickEvent;
import org.golde.bukkit.urltoblock.ChestGUI.OptionClickEventHandler;
import org.golde.bukkit.urltoblock.api.events.UrlBlockBreakEvent;
import org.golde.bukkit.urltoblock.api.events.UrlBlockClickEvent;
import org.golde.bukkit.urltoblock.api.events.UrlBlockPlaceEvent;

import net.minecraft.server.v1_11_R1.EntityPlayer;
import net.minecraft.server.v1_11_R1.NBTTagCompound;
import net.minecraft.server.v1_11_R1.NBTTagList;
import net.minecraft.server.v1_11_R1.PacketPlayOutAnimation;

public class Main extends JavaPlugin implements Listener{

	String key = "NO_KEY";
	Random rand = new Random();
	List<UrlBlock> blocks = new ArrayList<UrlBlock>();
	final String website = "http://egoldblockcreator.azurewebsites.net/Api/";
	public static Main plugin;

	public void onEnable() {
		plugin = this;
		getCommand("urltoblock").setExecutor(this);
		Bukkit.getPluginManager().registerEvents(this, this);
		getLogger().info("UrlToBlock plugin is starting.");
		getKey();
		populateBlockListFromServer();
		readConfig();

		for(World w:Bukkit.getWorlds()) {
			w.setGameRuleValue("sendCommandFeedback", "false"); //So no spam in chat when placing blocks
		}
		checkForUpdates();
	}

	void checkForUpdates(){
		Updater updater = new Updater("40330"); 
		Updater.UpdateResults result = updater.checkForUpdates();
		if(result.getResult() == Updater.UpdateResult.FAIL)
		{
			getLogger().severe("Update checker failed to check for updates!");
			getLogger().severe("Stacktrace: " + result.getVersion());
		}
		else if(result.getResult() == Updater.UpdateResult.NO_UPDATE)
		{
			getLogger().info("No update available");
		}
		else if(result.getResult() == Updater.UpdateResult.UPDATE_AVAILABLE)
		{
			Bukkit.getConsoleSender().sendMessage("[UrlToBlock] §aAn update for UrlToBlock has been found!");
			Bukkit.getConsoleSender().sendMessage("[UrlToBlock] §bCurrent version: §e" + getDescription().getVersion() + "§b, new version: §e" + result.getVersion());
		}
		else if (result.getResult() == Updater.UpdateResult.DEV){
			Bukkit.getConsoleSender().sendMessage("[UrlToBlock] §eYou seem to have a version of the plugin that is not on spigot...");
			Bukkit.getConsoleSender().sendMessage("[UrlToBlock] §cExpect bugs!");
		}
	}

	boolean noParticles = true;
	void readConfig() {
		saveDefaultConfig();
		noParticles = getConfig().getBoolean("noParticles");
	}

	//Reads the UUID.key uuid so we know where on the server is their resource pack
	void getKey() {
		getDataFolder().mkdir();
		File theKey = new File(getDataFolder(), "UUID.key");
		try {
			if(!theKey.exists()) {
				theKey.createNewFile();
				BufferedWriter writer = new BufferedWriter(new FileWriter(theKey));
				writer.write(UUID.randomUUID().toString() + "\n");
				writer.write("#DO NOT MODIFY (All custom blocks will be lost if you do)");
				writer.close();
			} 
			BufferedReader reader = new BufferedReader(new FileReader(theKey));
			key = reader.readLine(); 
			reader.close();
		}catch (IOException e) {
			e.printStackTrace();
		}
	}

	//In game tab complete stuff
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, final String[] args){
		List<String> l = new ArrayList<String>();

		if(args.length == 1) {
			l.add("add");
			l.add("remove");
			l.add("reload");
			l.add("silent");
			if(sender instanceof Player) {l.add("gui");}
			if(!(sender instanceof Player)) {l.add("purge");}

		}

		if(args.length == 2) {
			if(args[0].equalsIgnoreCase("remove")) {
				for(UrlBlock b:blocks) {
					l.add(String.valueOf(b.getDamage()));
				}
			}
		}

		l.removeIf(new Predicate<String>() {
			@Override
			public boolean test(String a) {
				return !(a.startsWith(args[args.length - 1]));
			}
		});

		return l;
	}

	//Plugin commands
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

		boolean isPlayer = sender instanceof Player;

		if(args.length == 0) {
			displayHelp(sender, isPlayer);
			return true;
		}

		if(args[0].equalsIgnoreCase("gui")) {
			if(isPlayer) {
				openGui((Player)sender, 0);
			}
			return true;
		}
		else if(args[0].equalsIgnoreCase("add") | args[0].equalsIgnoreCase("create")) {
			if(args.length != 2) {
				displayHelp(sender, isPlayer);
				return true;
			}
			addBlock(sender, args[1]);
			return true;
		}

		else if(args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("delete")) {
			if(args.length != 2 || !isInteger(args[1])) {
				displayHelp(sender, isPlayer);
				return true;
			}else {
				removeBlock(sender, Integer.parseInt(args[1]));
			}
		}
		else if(args[0].equalsIgnoreCase("reload")) {
			readConfig();
			sender.sendMessage("Config reloaded.");
		}
		else if(args[0].equalsIgnoreCase("purge")) {
			if(isPlayer) {
				sender.sendMessage("This command is for console only for safety reasons.");
				return true;
			}
			purgeAllBlocks(sender);
		}
		else {
			displayHelp(sender, isPlayer);
		}

		return true;
	}

	//Purge all made blocks from server (Console only)c
	private void purgeAllBlocks(CommandSender sender) {
		try {
			sendGet(website + "DeleteAll?uuid=" + key);
		} catch (Exception e) {
			e.printStackTrace();
		}
		blocks = new ArrayList<UrlBlock>();
		sender.sendMessage("Purged all blocks.");
		for(Player all:Bukkit.getOnlinePlayers()) {
			getResourcePack(all);
		}
	}

	boolean isInteger(String s) {
		try{
			@SuppressWarnings("unused")
			int num = Integer.parseInt(s);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	//User help if command is typed wrong
	void displayHelp(CommandSender sender, boolean isPlayer) {
		sender.sendMessage("--- UrlToBlock Help ---");
		sender.sendMessage("/ub add <image url> - Create a block");
		sender.sendMessage("/ub remove <id> - Remove a block");
		if(isPlayer) {
			sender.sendMessage("/ub gui - Open gui of all created blocks");
		}
		if(!isPlayer) {
			sender.sendMessage("/ub purge - Remove all blocks");
		}
		sender.sendMessage("/ub reload - Reloads config");
		sender.sendMessage("-----------------------");
	}

	//Adds a block to the game
	void addBlock(CommandSender sender, String blockurl) {
		try {
			String url = website + "AddTexture?uuid=" + key + "&texture=" + URLEncoder.encode(blockurl, "UTF-8");
			String responce = sendGet(url);
			if(responce.startsWith("@")) {
				String error = "Uh Oh! Something bad happened! Error Code: " + responce;
				sender.sendMessage(ChatColor.RED + error);
				getLogger().warning(error);
				return;
			}else {
				blocks.add(new UrlBlock(Integer.parseInt(responce)));
				sender.sendMessage("Success!");
				if(sender instanceof Player) {
					addBlock((Player)sender, (short)Integer.parseInt(responce));
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		for(Player all:Bukkit.getOnlinePlayers()) {
			getResourcePack(all);
		}
	}

	@EventHandler //gives the new joined player the resource pack
	public void onPlayerJoin(final PlayerJoinEvent e) {
		new BukkitRunnable() {
			@Override
			public void run() {
				getResourcePack(e.getPlayer());
			}
		}.runTaskLater(this, 2);

	}

	//Send the resourcepack to the player
	void getResourcePack(Player p) {
		try {
			String url = website + "GetUrl?uuid=" + key + "&spawner=" + noParticles;
			String responce = sendGet(url);
			if(responce.startsWith("@")) {
				getLogger().warning("Uh Oh! Something bad happened! Error Code: " + responce);
				return;
			}else {
				responce = responce + "?a=" + rand.nextInt(); //get around minecraft's cashing of resource packs
				p.setResourcePack(responce);

			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	//Send a get request to the backend
	String sendGet(String url) {
		try {
			URL obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();

			con.setRequestMethod("GET");

			BufferedReader in = new BufferedReader(
					new InputStreamReader(con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();
			return response.toString();
		}catch(Exception e) {
			return "@Texture Server seems to be offline. Please try again later.";
		}



	}

	//Removed a block from the arraylist and from the backend server
	void removeBlock(CommandSender sender, int damage) {
		for(UrlBlock b:blocks) {
			if(b.getDamage() == damage) {
				blocks.remove(b);
				try {
					String url = website + "DeleteTexture?uuid=" + key + "&damage=" + damage;
					String responce = sendGet(url);
					if(responce.startsWith("@")) {
						String error = "Uh Oh! Something bad happened! Error Code: " + responce;
						sender.sendMessage(ChatColor.RED + error);
						getLogger().warning(error);
						return;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				sender.sendMessage("Successfully removed block!");
				return;
			}
		}
		sender.sendMessage("Failed to remove block with id: " + damage);
	}

	//Get a list of damage ids from the backend server so we can populate the gui of past blocks we made
	void populateBlockListFromServer() {
		try {
			String url = website + "GetTextures?uuid=" + key;
			String responce = sendGet(url);
			if(responce.equals("")) {
				return;
			}
			if(responce.startsWith("@")) {
				getLogger().warning("Uh Oh! Something bad happened! Error Code: " + responce);
				return;
			}else {
				String[] arrayResponce = responce.split(",");
				for(String num:arrayResponce) {
					blocks.add(new UrlBlock(Integer.parseInt(num)));
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	//Show block GUI to a player (Starts at page 0)
	void openGui(final Player p, final int pageNumber) {
		int numberOfPages = 1 + (blocks.size() - 1) / 45;


		ChestGUI gui = new ChestGUI("UrlToBlock", 54, new OptionClickEventHandler(){

			@Override
			public void onOptionClick(OptionClickEvent e) {
				if(e.getItem().getType() == Material.APPLE) {
					new BukkitRunnable() {
						public void run() {
							openGui(p, pageNumber - 1);
						}
					}.runTaskLater(Main.this, 2);
				}
				else if(e.getItem().getType() == Material.EMERALD) {
					new BukkitRunnable() {
						public void run() {
							openGui(p, pageNumber + 1);
						}
					}.runTaskLater(Main.this, 2);
				}
				else {
					addBlock(p, e.getItem().getDurability());
				}
				e.setWillClose(true);
				e.setWillDestroy(true);
			}}, this);

		if (pageNumber > 0) {
			gui.setOption(45, new ItemStack(Material.APPLE), "&cPrevious Page", "");
		}
		if (pageNumber < numberOfPages - 1) {
			gui.setOption(53, new ItemStack(Material.EMERALD), "&aNext Page", "");
		}
		for(int i = pageNumber * 45; i < Math.min((pageNumber + 1) * 45, blocks.size()); i++) {
			gui.setOption(i - (pageNumber * 45), blocks.get(i).getGuiItem(), "&eClick to get Custom Block", "&bID: " + blocks.get(i).getDamage());
		}

		gui.open(p);
	}

	//Gets a block by its damage value
	UrlBlock getBlockByDamageValue(short damage) {
		for(UrlBlock b:blocks) {
			if(b.getDamage() == damage) {
				return b;
			}
		}
		return null;
	}

	//Is a url block item
	boolean isUrlBlockItem(ItemStack i) {
		for(UrlBlock b:blocks) {
			if(b.getDamage() == i.getDurability()) {
				return true;
			}
		}
		return false;
	}

	void addBlock(Player p, short data) {
		p.getInventory().addItem(getBlockByDamageValue(data).getHandItem());
		fixAttackSpeed(p);
	}

	@EventHandler
	public void onBreak(BlockBreakEvent e) {
		Block blockClicked = e.getBlock();

		if (blockClicked == null) {
			return;
		}

		if(isUrlBlock(blockClicked.getLocation())) {
			int id = getIdFromSpawner(blockClicked.getLocation());
			if(id >=0) {
				e.setExpToDrop(0);
				e.setCancelled(callEvent(new UrlBlockBreakEvent(blockClicked.getLocation(), id, e.getPlayer())));
				if(e.getPlayer().getGameMode() != GameMode.CREATIVE) {
					e.getBlock().getLocation().getWorld().dropItem(e.getBlock().getLocation(), getBlockByDamageValue((short)id).getHandItem());
				}
			}
		}
	}

	@EventHandler
	public void onUrlBlockItemPlace(PlayerInteractEvent e) {

		Block blockClicked = e.getClickedBlock();
		BlockFace clickedBlockFace = e.getBlockFace();


		if (blockClicked == null) {
			return;
		}

		if(isUrlBlock(blockClicked.getLocation())) {
			int id = getIdFromSpawner(blockClicked.getLocation());
			if(id >=0) {
				callEvent(new UrlBlockClickEvent(e.getPlayer(), id, e.getAction(), clickedBlockFace, e.getHand()));
			}
		}

		if(e.getItem() == null) {
			return;
		}

		if(e.getAction() != Action.RIGHT_CLICK_BLOCK) {
			return;
		}

		if(!isUrlBlockItem(e.getItem())) {
			return;
		}

		World world =  blockClicked.getLocation().getWorld();
		UrlBlock urlBlock = getBlockByDamageValue(e.getItem().getDurability());
		boolean cancled = callEvent(new UrlBlockPlaceEvent(blockClicked.getLocation(), urlBlock.getDamage(), e.getPlayer()));

		if(!cancled) {
			urlBlock.placeBlock(e.getPlayer(), blockClicked.getX() + clickedBlockFace.getModX(), blockClicked.getY() + clickedBlockFace.getModY(), blockClicked.getZ() + clickedBlockFace.getModZ());
			world.playSound(e.getClickedBlock().getLocation(), Sound.BLOCK_METAL_PLACE, 1, 0);
			armSwingAnimation(e.getPlayer());
			removeItemFromPlayer(e.getPlayer(), e.getItem());
		}
	}

	void removeItemFromPlayer(Player p, ItemStack i) {
		if(p.getGameMode() == GameMode.SURVIVAL) {
			i.setAmount(i.getAmount() - 1);
			p.updateInventory();
		}
	}

	boolean callEvent(Event event) {
		getServer().getPluginManager().callEvent(event);
		if(event instanceof Cancellable) {
			if(((Cancellable) event).isCancelled()){
				return true;
			}
		}
		return false;
	}

	void fixAttackSpeed(Player p) {
		AttributeInstance attribute = p.getAttribute(Attribute.GENERIC_ATTACK_SPEED);
		attribute.setBaseValue(24);
		p.saveData();
	}

	void armSwingAnimation(Player p) {
		EntityPlayer b = ((CraftPlayer) p).getHandle();
		PacketPlayOutAnimation packet = new PacketPlayOutAnimation(b, 0);
		b.playerConnection.sendPacket(packet);
	}

	public boolean isUrlBlock(Location l) {
		Block b = l.getBlock();
		if(b == null) {
			return false;
		}

		BlockState bs = b.getState();
		if(bs == null) {
			return false;
		}

		if(bs.getType() == Material.MOB_SPAWNER) {
			CreatureSpawner spawner = (CreatureSpawner)bs;
			if(spawner.getSpawnedType() == EntityType.ARMOR_STAND) {
				//Bad way to telling but it works for now
				return true;
			}
		}
		return false;
	}

	public int getIdFromSpawner(Location l) {
		if(!isUrlBlock(l)) {return -1;}
		BlockState blockState = l.getBlock().getState();
		CraftCreatureSpawner spawner = ((CraftCreatureSpawner)blockState);
		try {
			net.minecraft.server.v1_11_R1.TileEntityMobSpawner tile = spawner.getTileEntity();
			NBTTagList handItems = (NBTTagList)((NBTTagCompound)tile.d().get("SpawnData")).get("HandItems");
			return handItems.get(0).getInt("Damage");
		}catch(Exception e) {
			e.printStackTrace();
			return -2;
		}
	}

	@EventHandler
	public void pickupItem(PlayerPickupItemEvent e) {
		ItemStack i = e.getItem().getItemStack();
		if(isUrlBlockItem(i)) {
			stackItems(e);
			e.getItem().remove();
		}

	}

	void stackItems(PlayerPickupItemEvent event) {
		ItemStack is = event.getItem().getItemStack();
		if (is == null || is.getItemMeta() == null || is.getItemMeta().getDisplayName() == null)
			return;
		
		int countPickedUp = is.getAmount();
		PlayerInventory inv = event.getPlayer().getInventory();
		
		for (int slot = 0; slot < inv.getSize(); ++slot)
		{
			ItemStack current = inv.getItem(slot);
			if (current != null && current.getItemMeta() != null && current.getItemMeta().getDisplayName() != null && current.getItemMeta().getDisplayName().equals(is.getItemMeta().getDisplayName())) {
				int numberLeft = 64 - current.getAmount();
				if (numberLeft > 0) {
					int add = Math.min(numberLeft, countPickedUp);
					current.setAmount(current.getAmount() + add);
					inv.setItem(slot, current);
					countPickedUp -= add;
				}
			}
		
			if (countPickedUp == 0)
				break;
		}
		
		if (countPickedUp > 0) {
			is.setAmount(countPickedUp);
			for (ItemStack extra: inv.addItem(is).values()) {
				event.getPlayer().getLocation().getWorld().dropItem(event.getPlayer().getLocation(), extra);
			}
		}
		
		event.getPlayer().updateInventory();
		event.setCancelled(true);
	}

}