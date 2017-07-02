package org.golde.bukkit.urltoblock;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
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
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_11_R1.block.CraftCreatureSpawner;
import org.bukkit.craftbukkit.v1_11_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_11_R1.inventory.CraftItemStack;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.golde.bukkit.urltoblock.ChestGUI.OptionClickEvent;
import org.golde.bukkit.urltoblock.ChestGUI.OptionClickEventHandler;
import org.golde.bukkit.urltoblock.Updater.UpdateResult;
import org.golde.bukkit.urltoblock.Updater.UpdateResults;
import org.golde.bukkit.urltoblock.api.events.UrlBlockBreakEvent;
import org.golde.bukkit.urltoblock.api.events.UrlBlockClickEvent;
import org.golde.bukkit.urltoblock.api.events.UrlBlockPlaceEvent;
import org.golde.bukkit.urltoblock.api.events.UrlBlockPopulateEvent;
import org.golde.bukkit.urltoblock.dump.DumpException;
import org.golde.bukkit.urltoblock.dump.ReportError;

import net.minecraft.server.v1_11_R1.EntityPlayer;
import net.minecraft.server.v1_11_R1.NBTTagCompound;
import net.minecraft.server.v1_11_R1.NBTTagInt;
import net.minecraft.server.v1_11_R1.NBTTagList;
import net.minecraft.server.v1_11_R1.NBTTagString;
import net.minecraft.server.v1_11_R1.PacketPlayOutAnimation;

public class Main extends JavaPlugin implements Listener{

	public static final boolean DEV = false; //TODO: CHANGE
	public String key = "NO_KEY";
	Random rand = new Random();
	public List<UrlBlock> blocks = new ArrayList<UrlBlock>();
	public static Main plugin;
	public ServerType SERVER_TYPE;
	public void onEnable() {
		plugin = this;
		getCommand("urltoblock").setExecutor(this);
		Bukkit.getPluginManager().registerEvents(this, this);
		getLogger().info("UrlToBlock plugin is starting.");
		getKey();
		populateBlockListFromServer();
		readConfig();
		SERVER_TYPE = ServerType.whatAmI(this);
		for(World w:Bukkit.getWorlds()) {
			w.setGameRuleValue("sendCommandFeedback", "false"); //So no spam in chat when placing blocks
		}
		checkForUpdates();
	}

	final String getWebsite() {
		if(DEV) {return "http://egoldblockcreatordev.azurewebsites.net/Api/";}
		return "http://egoldblockcreator.azurewebsites.net/Api/";
	}

	void checkForUpdates(){
		Updater updater = new Updater(this, "40330"); 
		UpdateResults result = updater.checkForUpdates();


		if(result.getResult() == UpdateResult.FAIL)
		{
			getLogger().severe("Update checker failed to check for updates!");
			getLogger().severe("Stacktrace: " + result.getVersion());
		}
		else if(result.getResult() == UpdateResult.NO_UPDATE)
		{
			getLogger().info("No update available");
		}
		else if(result.getResult() == UpdateResult.UPDATE_AVAILABLE)
		{
			consoleMsg("§aAn update for UrlToBlock has been found!");
			consoleMsg("§bCurrent version: §e" + getDescription().getVersion() + "§b, new version: §e" + result.getVersion());

			UpdateResults dlResult = updater.downloadUpdate();
			if(dlResult.getResult() == UpdateResult.FAIL) {
				getLogger().severe("Update downloader failed to download updates!");
				getLogger().severe("Stacktrace: " + result.getVersion());
			}
			else if(dlResult.getResult() == UpdateResult.UPDATE_SUCCESS) {
				consoleMsg("§aThe update has been downloaded! Please restart your server.");
				Bukkit.getPluginManager().disablePlugin(this);
			}
			else if(dlResult.getResult() == UpdateResult.DISABLED && result.getResult() == UpdateResult.UPDATE_AVAILABLE) {
				getLogger().warning("There is an update available but you have auto-update-downloads disabled.");
			}

		}
		else if (result.getResult() == Updater.UpdateResult.DEV){
			Bukkit.getConsoleSender().sendMessage("§eYou seem to have a version of the plugin that is not on spigot...");
			consoleMsg("§cExpect bugs!");
		}


	}

	void consoleMsg(String msg) {
		Bukkit.getConsoleSender().sendMessage("[UrlToBlock] " + msg);
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
			//e.printStackTrace();
		}
	}

	//In game tab complete stuff
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, final String[] args){
		List<String> l = new ArrayList<String>();

		if(args.length == 1) {
			l.add("add");
			l.add("addtile");
			l.add("remove");
			l.add("reload");
			l.add("dump");

			if(sender instanceof Player) {
				l.add("gui");
				l.add("stack");
			}else {
				l.add("purge");
				l.add("saveResourcePackToComputer");
			}
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
		else if(args[0].equalsIgnoreCase("addtile")) {
			if(args.length != 4) {
				displayHelp(sender, isPlayer);
				return true;
			}
			addTileBlock(sender, args[1], Integer.valueOf(args[2]), Integer.valueOf(args[3]));
			return true;
		}

		else if(args[0].equalsIgnoreCase("add")) {
			if(args.length != 2) {
				displayHelp(sender, isPlayer);
				return true;
			}
			addBlock(sender, args[1]);
			return true;
		}

		else if(args[0].equalsIgnoreCase("rename")) {
			if(args.length != 3) {
				displayHelp(sender, isPlayer);
				return true;
			}
			renameBlock(sender, getBlockByDamageValue(Short.valueOf(args[1])), args[2]);
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
		else if(args[0].equalsIgnoreCase("saveResourcePackToComputer")) {
			if(isPlayer) {
				sender.sendMessage("This command is for console only for safety reasons.");
				return true;
			}
			saveResourcepackToComputer(sender);
		}
		else if(args[0].equalsIgnoreCase("dump")) {
			new ReportError(new DumpException(), sender);
		}else if(args[0].equalsIgnoreCase("stack")) {
			if(!isPlayer) {
				sender.sendMessage("Consoles do not have an inventory!");
				return true;
			}
			stackAllUrlBlocks((Player)sender);
		}
		else {
			displayHelp(sender, isPlayer);
		}

		return true;
	}
	
	//User help if command is typed wrong
		void displayHelp(CommandSender sender, boolean isPlayer) {
			sender.sendMessage("--- UrlToBlock Help ---");
			sender.sendMessage("/ub add <image url> - Create a block");
			sender.sendMessage("/ub addtile <image url> <width> <height> - Create tiled blocks");
			sender.sendMessage("/ub remove <id> - Remove a block");
			sender.sendMessage("/ub dump - Dump debug info");
			sender.sendMessage("/ub rename <id> <name> - Dump debug info");
			if(isPlayer) {
				sender.sendMessage("/ub gui - Open gui of all created blocks");
				sender.sendMessage("/ub stack - Stack unstacked UrlBlocks in your inventory");
			}
			if(!isPlayer) {
				sender.sendMessage("/ub purge - Remove all blocks");
				sender.sendMessage("/ub saveResourcePackToComputer - Saves the resourcepack to the computers file system in the plugins directory");
			}
			sender.sendMessage("/ub reload - Reloads config");
			sender.sendMessage("-----------------------");
		}

	private void saveResourcepackToComputer(final CommandSender sender) {
		String url = getResourcePackQuery();

		sendGetAsync(url, new GetFinished() {
			public void response(String responce) {
				if(responce.startsWith("@")) {
					getLogger().warning("Uh Oh! Something bad happened! Error Code: " + responce);
					return;
				}else {
					try {
						URL url = new URL(responce);
						URLConnection conn = url.openConnection();
						InputStream in = conn.getInputStream();
						FileOutputStream out = new FileOutputStream(getDataFolder().getAbsolutePath() + "\\UrlToBlock ResourcePack.zip");
						byte[] b = new byte[1024];
						int count;
						while ((count = in.read(b)) >= 0) {
							out.write(b, 0, count);
						}
						out.flush(); out.close(); in.close();
						sender.sendMessage("Saved resourcepack to: " + getDataFolder().getAbsolutePath() + "\\UrlToBlock ResourcePack.zip");

					} catch (IOException e) {
						sender.sendMessage("Failed to download or save the resourcepack to the computers file system.");
						new ReportError(e);
					}
				}		
			}
		});
	}

	//Purge all made blocks from server (Console only)c
	private void purgeAllBlocks(final CommandSender sender) {
		sendGetAsync(getWebsite() + "DeleteAll?uuid=" + key, new GetFinished() {
			public void response(String response) {
				blocks = new ArrayList<UrlBlock>();
				sender.sendMessage("Purged all blocks.");
				getResourcePack(Bukkit.getOnlinePlayers().toArray(new Player[0]));				
			}
		});
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

	

	String urlEncode(String urlParam)
	{
		try {
			return URLEncoder.encode(urlParam, "UTF-8");
		}
		catch (Exception e) {
			return "";
		}
	}

	//Adds a block to the game
	void addBlock(final CommandSender sender, final String blockurl) {
		sender.sendMessage("Your request is being prossessed... Please wait.");
		String url = getWebsite() + "AddTexture?uuid=" + key + "&texture=" + urlEncode(blockurl);
		sendGetAsync(url, new GetFinished() {
			@Override
			public void response(String responce) {
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
				populateBlockListFromServer();
				getResourcePack(Bukkit.getOnlinePlayers().toArray(new Player[0]));
			}
		});
	}

	//Adds a block to the game
	void addTileBlock(final CommandSender sender, String blockurl, int width, int height) {
		sender.sendMessage("Your request is being prossessed... Please wait.");
		String url = getWebsite() + "AddTiled?uuid=" + key + "&texture=" + urlEncode(blockurl) + "&width=" + width + "&height=" + height;
		sendGetAsync(url, new GetFinished() {
			public void response(String responce) {
				if(responce.startsWith("@")) {
					String error = "Uh Oh! Something bad happened! Error Code: " + responce;
					sender.sendMessage(ChatColor.RED + error);
					getLogger().warning(error);
					return;
				}else {
					String[] arrayResponce = responce.split(",");
					for(String num:arrayResponce) {
						blocks.add(new UrlBlock(Integer.parseInt(num)));
						if(sender instanceof Player) {
							addBlock((Player)sender, (short)Integer.parseInt(num));
						}
					}
					sender.sendMessage("Success!");

				}
				populateBlockListFromServer();
				getResourcePack(Bukkit.getOnlinePlayers().toArray(new Player[0]));
			}
		});
	}

	@EventHandler //gives the new joined player the resource pack
	public void onPlayerJoin(final PlayerJoinEvent e) {
		new BukkitRunnable() {
			@Override
			public void run() {
				getResourcePack(e.getPlayer());
				fixAttackSpeedForInventory(e.getPlayer());
			}
		}.runTaskLater(this, 2);

	}

	boolean hasExternalResourcePack(){
		String con = getConfig().getString("existing-resource-pack");
		if(con == null || con.equalsIgnoreCase("") || con.equalsIgnoreCase(" ") || con.equalsIgnoreCase("NONE")) {
			return false;
		}
		return true;
	}

	private String getResourcePackQuery()
	{
		String url = getWebsite() + "GetUrl?uuid=" + key + "&spawner=" + noParticles + "&transparent=" + getConfig().getBoolean("noSpawnerTexture", false);
		if(hasExternalResourcePack()) {
			try {
				url+= "&merge=" + URLEncoder.encode(getConfig().getString("existing-resource-pack"), "UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		return url;
	}

	//Send the resourcepack to the player
	public void getResourcePack(final Player... player) {
		String url = getResourcePackQuery();

		sendGetAsync(url, new GetFinished() {
			public void response(String responce) {
				if(responce.startsWith("@")) {
					getLogger().warning("Uh Oh! Something bad happened! Error Code: " + responce);
					return;
				}else {
					responce = responce + "?a=" + rand.nextInt(); //get around minecraft's cashing of resource packs
					for(Player p:player) {
						p.setResourcePack(responce);
					}
				}		
			}
		});
	}

	// Send a get request to the backend, asyncronously. When the response is received,
	// calls the "response" function on the object "whenFinished", passing the response
	// from the GET.
	void sendGetAsync(final String url, final GetFinished whenFinished) {
		new BukkitRunnable() {
			public void run() {
				String getResponse;
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
					getResponse = response.toString();
				} catch(Exception e) {
					e.printStackTrace();
					getResponse = "@Texture Server seems to be offline. Please try again later.";
				}
				final String responseToSend = getResponse;

				new BukkitRunnable() {
					public void run() {
						whenFinished.response(responseToSend);
					}
				}.runTask(Main.plugin);
			}
		}.runTaskAsynchronously(Main.plugin);
	}

	//Removed a block from the arraylist and from the backend server
	void removeBlock(CommandSender sender, int damage) {
		for(UrlBlock b:blocks) {
			if(b.getDamage() == damage) {
				removeSingleBlock(sender, b);
				return;
			}
		}
		sender.sendMessage("Failed to remove block with id: " + damage);
	}

	void removeSingleBlock(final CommandSender sender, UrlBlock b)
	{
		blocks.remove(b);
		String url = getWebsite() + "DeleteTexture?uuid=" + key + "&damage=" + b.getDamage();
		sendGetAsync(url, new GetFinished() {
			public void response(String responce) {
				if(responce.startsWith("@")) {
					String error = "Uh Oh! Something bad happened! Error Code: " + responce;
					sender.sendMessage(ChatColor.RED + error);
					getLogger().warning(error);
					return;
				}

				sender.sendMessage("Successfully removed block!");				
			}
		});
	}

	void renameBlock(final CommandSender sender, final UrlBlock block, final String newName) {
		String url = getWebsite() + "Rename?uuid=" + key + "&damage=" + block.getDamage() + "&name=" + urlEncode(newName);
		sendGetAsync(url, new GetFinished() {
			public void response(String responce) {
				if(responce.startsWith("@")) {
					String error = "Uh Oh! Something bad happened! Error Code: " + responce;
					sender.sendMessage(ChatColor.RED + error);
					getLogger().warning(error);
					return;
				}
				block.setName(newName);
				refreshEveryonesUrlBlock();
				sender.sendMessage("Successfully renamed block!");				
			}
		});
	}

	//Get a list of damage ids from the backend server so we can populate the gui of past blocks we made
	void populateBlockListFromServer() {
		String url = getWebsite() + "GetTexturesAndNames?uuid=" + key;
		sendGetAsync(url, new GetFinished() {
			public void response(String responce) {
				if(responce.equals("")) {
					return;
				}
				if(responce.startsWith("@")) {
					getLogger().warning("Uh Oh! Something bad happened! Error Code: " + responce);
					return;
				}else {
					blocks = new ArrayList<UrlBlock>();
					try {
						for (String line: responce.split("\\|")) {
							String[] fields = line.split("&", -1);
							if (fields.length == 3) {
								int num = Integer.parseInt(fields[0]);
								String name = URLDecoder.decode(fields[1], "UTF-8");
								String lore = URLDecoder.decode(fields[2], "UTF-8");

								if (name.equals("")) {
									blocks.add(new UrlBlock(num));
								}
								else {
									blocks.add(new UrlBlock(num, name, lore.split("\r\n")));
								}
							}
						}
						refreshEveryonesUrlBlock();
						callEvent(new UrlBlockPopulateEvent());
					}
					catch (Exception e) {
						new ReportError(e);
					}
				}			
			}
		});
	}

	//Show block GUI to a player (Starts at page 0)
	void openGui(final Player p, final int pageNumber) {
		int numberOfPages = 1 + (blocks.size() - 1) / 45;


		ChestGUI gui = new ChestGUI("UrlToBlock - " + (pageNumber + 1), 54, new OptionClickEventHandler(){

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
					if(e.getClickEvent().getClick().isShiftClick()) {
						addBlock(p, e.getItem().getDurability(), 64);
					}else {
						addBlock(p, e.getItem().getDurability());
					}
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
	public UrlBlock getBlockByDamageValue(short damage) {
		for(UrlBlock b:blocks) {
			if(b.getDamage() == damage) {
				return b;
			}
		}
		return null;
	}

	//Is a url block item
	public boolean isUrlBlockItem(ItemStack i) {
		if(i == null) {return false;}
		for(UrlBlock b:blocks) {
			if(b.getDamage() == i.getDurability() && i.getItemMeta().isUnbreakable() && (i.getType() == Material.DIAMOND_AXE || i.getType() == Material.DIAMOND_HOE)) {
				return true;
			}
		}
		return false;
	}

	void addBlock(Player p, short data) {
		addBlock(p, data, 1);
	}
	void addBlock(Player p, short data, int amount) {
		ItemStack i = getBlockByDamageValue(data).getHandItem();
		i.setAmount(amount);
		i = fixAttackSpeed(p, i);
		p.getInventory().addItem(i);
	}

	void refreshEveryonesUrlBlock() {
		for(Player p:Bukkit.getOnlinePlayers()) {
			PlayerInventory pinv = p.getInventory();
			for (int i = 0; i < pinv.getSize(); i++) {
				ItemStack item = pinv.getItem(i);
				if(isUrlBlockItem(item)) {
					UrlBlock urlBlock = getBlockByDamageValue(item.getDurability());
					pinv.setItem(i, urlBlock.getHandItem());
				}
			}

			p.updateInventory();
		}
	}

	@EventHandler
	public void onBreak(BlockBreakEvent e) {
		Block blockClicked = e.getBlock();

		if (blockClicked == null) {
			return;
		}

		if(isUrlBlock(blockClicked.getLocation())) {
			int id = getIdFromSpawner(blockClicked.getLocation(), e.getPlayer());
			if(id >=0) {
				e.setExpToDrop(0);
				e.setCancelled(callEvent(new UrlBlockBreakEvent(blockClicked.getLocation(), id, e.getPlayer())));
				if(e.getPlayer().getGameMode() != GameMode.CREATIVE) {
					e.getBlock().getLocation().getWorld().dropItem(e.getBlock().getLocation(), getBlockByDamageValue((short)id).getHandItem(1));
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
			int id = getIdFromSpawner(blockClicked.getLocation(), e.getPlayer());
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
			world.playSound(e.getClickedBlock().getLocation(), Sound.BLOCK_METAL_PLACE, 1, 1);
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


	public void fixAttackSpeedForInventory(Player p) {
		PlayerInventory pinv = p.getInventory();
		for (int i = 0; i < pinv.getSize(); i++) {
			ItemStack item = pinv.getItem(i);
			if(isUrlBlockItem(item)) {
				fixAttackSpeed(p, item);
			}
		}
		p.updateInventory();
	}

	public ItemStack fixAttackSpeed(Player p, ItemStack item) {
		try {
			net.minecraft.server.v1_11_R1.ItemStack nmsStack = CraftItemStack.asNMSCopy(item);
			NBTTagCompound compound = (nmsStack.hasTag()) ? nmsStack.getTag() : new NBTTagCompound();
			NBTTagList modifiers = new NBTTagList();
			NBTTagCompound damage = new NBTTagCompound();
			damage.set("AttributeName", new NBTTagString("generic.attackSpeed"));
			damage.set("Name", new NBTTagString("generic.attackSpeed"));
			damage.set("Amount", new NBTTagInt(24));
			damage.set("Operation", new NBTTagInt(0));
			damage.set("UUIDLeast", new NBTTagInt(894654));
			damage.set("UUIDMost", new NBTTagInt(2872));
			modifiers.add(damage);
			compound.set("AttributeModifiers", modifiers);
			nmsStack.setTag(compound);
			item = CraftItemStack.asBukkitCopy(nmsStack);
		}
		catch(Exception e) {
			new ReportError(e, p);
		}
		return item;
	}

	void armSwingAnimation(Player p) {
		try {
			EntityPlayer b = ((CraftPlayer) p).getHandle();
			PacketPlayOutAnimation packet = new PacketPlayOutAnimation(b, 0);
			b.playerConnection.sendPacket(packet);
		}catch(Exception e) {
			new ReportError(e, p);
		}
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

	public int getIdFromSpawner(Location l, CommandSender p) {
		if(!isUrlBlock(l)) {return -1;}
		BlockState blockState = l.getBlock().getState();
		CraftCreatureSpawner spawner = ((CraftCreatureSpawner)blockState);
		try {
			net.minecraft.server.v1_11_R1.TileEntityMobSpawner tile = spawner.getTileEntity();
			NBTTagList handItems = (NBTTagList)((NBTTagCompound)tile.d().get("SpawnData")).get("HandItems");
			return handItems.get(0).getInt("Damage");
		}catch(Exception e) {
			new ReportError(e, p);
			//e.printStackTrace();
			return -2;
		}
	}

	@EventHandler
	public void pickupItem(PlayerPickupItemEvent e) {
		ItemStack i = e.getItem().getItemStack();
		if(isUrlBlockItem(i)) {
			List<ItemStack> returned = stackItems(i, e.getPlayer().getInventory());
			for(ItemStack toDrop:returned) {
				e.getPlayer().getWorld().dropItem(e.getPlayer().getLocation(), toDrop);
			}

			e.getPlayer().getLocation().getWorld().playSound(e.getPlayer().getLocation(), Sound.ENTITY_ITEM_PICKUP, 1, 2);
			e.getItem().remove();
			e.getPlayer().updateInventory();
			e.setCancelled(true);
		}

	}

	@EventHandler
	public void hopperTransfer(final InventoryMoveItemEvent e) {
		if(!isUrlBlockItem(e.getItem())) {return;}

		final List<ItemStack> returned = stackItems(e.getItem(), e.getDestination());

		// Remove the item from the event so it doesn't get put into the destination
		final ItemStack itemToRemove = e.getItem();
		new BukkitRunnable() {
			public void run() {
				e.getSource().removeItem(itemToRemove);
				// add items that don't fit back to the source.
				for(ItemStack item: returned) {
					e.getSource().addItem(item);
				}
			}
		}.runTaskLater(this, 1);

		e.setCancelled(true);
	}

	@EventHandler
	public void villagerTrade(final InventoryClickEvent e) {
		if(e.getInventory().getType() == InventoryType.MERCHANT && e.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
			final Player p = (Player)e.getWhoClicked();
			if(!isUrlBlockItem(e.getCurrentItem())) {return;}
			
			new BukkitRunnable() {
				@Override
				public void run() {
					stackAllUrlBlocks(p);
				}
			}.runTaskLater(this, 1);
		}
	}

	// Take all UrlBlocks in the players inventory and stack them up 
	// as much as possible.
	void stackAllUrlBlocks(Player p)
	{
		PlayerInventory inv = p.getInventory();

		for (int slot = 0; slot < inv.getSize(); ++slot)
		{
			ItemStack current = inv.getItem(slot);
			if (current != null && isUrlBlockItem(current)) {
				inv.clear(slot);
				List<ItemStack> returned = stackItems(current, inv);
				if (returned.size() > 0) {
					inv.addItem(returned.get(0));
				}
			}
		}
		
		p.updateInventory();
	}
	
	List<ItemStack> stackItems(ItemStack is, Inventory inv) {
		if (is == null || is.getItemMeta() == null || is.getItemMeta().getDisplayName() == null)
			return new ArrayList<ItemStack>();

		int countPickedUp = is.getAmount();

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
			List<ItemStack> toReturn = new ArrayList<ItemStack>();
			for (ItemStack extra: inv.addItem(is).values()) {
				toReturn.add(extra);
			}
			return toReturn;
		}
		return new ArrayList<ItemStack>();

		//event.getPlayer().updateInventory();
		//event.setCancelled(true);
	}

	public String commaSep(ArrayList<String> list)
	{
		String result = "";
		for (int i = 0; i < list.size(); ++i) {
			if (i != 0){
				result += ", ";
			}
			result += list.get(i);
		}
		return result;
	}

}

abstract class GetFinished
{
	public abstract void response(String response);
}


