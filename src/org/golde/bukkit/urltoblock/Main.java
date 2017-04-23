package org.golde.bukkit.urltoblock;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.function.Predicate;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.golde.bukkit.urltoblock.ChestGUI.OptionClickEvent;
import org.golde.bukkit.urltoblock.ChestGUI.OptionClickEventHandler;


public class Main extends JavaPlugin implements Listener{

	String key = "NO_KEY";
	Random rand = new Random();
	List<Block> blocks = new ArrayList<Block>();
	final String website = "http://egoldblockcreator.azurewebsites.net/Api/";

	public void onEnable() {
		getCommand("urltoblock").setExecutor(this);
		Bukkit.getPluginManager().registerEvents(this, this);
		getLogger().info("UrlToBlock plugin is starting.");
		getKey();
		populateBlockListFromServer();
		readConfig();
	}

	boolean noParticles = true;
	void readConfig() {
		saveDefaultConfig();
		noParticles = getConfig().getBoolean("noParticles");
	}

	//Stop Spigot from spamming everything that command blocks do.
	boolean silenceSpigotConsole() {
		String rootDir = new File(".").getAbsolutePath();
		File spigotYml = new File(rootDir, "spigot.yml");
		if(spigotYml.exists()) {
			FileConfiguration spigotConfig = YamlConfiguration.loadConfiguration(spigotYml);
			spigotConfig.getConfigurationSection("commands").set("silent-commandblock-console", true);
			try {
				spigotConfig.save(spigotYml);
				return true;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return false;
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
			if(sender instanceof Player) {l.add("gui");}
			if(!(sender instanceof Player)) {l.add("purge");}
			l.add("silent");

		}

		if(args.length == 2) {
			if(args[0].equalsIgnoreCase("remove")) {
				for(Block b:blocks) {
					l.add(String.valueOf(b.damage));
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
		else if(args[0].equalsIgnoreCase("silent")){
			for(World w:Bukkit.getWorlds()) {
				w.setGameRuleValue("commandBlockOutput", "false"); //So no spam in chat when placing blocks
			}
			sender.sendMessage("Successfully changed values.");
			if(silenceSpigotConsole()) {
				sender.sendMessage("Server restart required to apply changes.");
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
		blocks = new ArrayList<Block>();
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
		sender.sendMessage("/ub silent - Silents command-block-output everywhere");
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
				blocks.add(new Block(Integer.parseInt(responce)));
				sender.sendMessage("Success!");
				if(sender instanceof Player) {
					((Player)sender).getInventory().addItem(getBlockByDamageValue((short)Integer.parseInt(responce)).block);
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
	public void onPlayerJoin(PlayerJoinEvent e) {
		getResourcePack(e.getPlayer());
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
	String sendGet(String url) throws Exception {

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

	}

	//Removed a block from the arraylist and from the backend server
	void removeBlock(CommandSender sender, int damage) {
		for(Block b:blocks) {
			if(b.damage == damage) {
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
					blocks.add(new Block(Integer.parseInt(num)));
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
					p.getInventory().addItem(getBlockByDamageValue(e.getItem().getDurability()).block);
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
			gui.setOption(i - (pageNumber * 45), blocks.get(i).hoeItem, "&eClick to get Custom Block", "&bID: " + blocks.get(i).damage);
		}

		gui.open(p);
	}

	//Gets a block by its damage value
	Block getBlockByDamageValue(short damage) {
		for(Block b:blocks) {
			if(b.damage == damage) {
				return b;
			}
		}
		return null;
	}

}