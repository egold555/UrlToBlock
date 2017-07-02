package org.golde.bukkit.urltoblock.dump;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.golde.bukkit.urltoblock.Main;

public class DumpTemplate {
	
	private final String NEW_LINE = "\n";
	private final String exception;
	private final boolean isFromDumpCommand;
	
	public DumpTemplate(Exception ex){
		if(ex instanceof DumpException){
			exception = "(Dump Command)";
			isFromDumpCommand = true;
			return;
		}
		StringWriter w = new StringWriter();
		ex.printStackTrace(new PrintWriter(w));
		exception = w.toString();
		isFromDumpCommand = false;
	}
	
	private String getDate(){
		DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
		Date date = new Date();
		return dateFormat.format(date);
	}
	
	private String getPlugins(){
		String toReturn = "";
		for(Plugin pl:Bukkit.getPluginManager().getPlugins()){
			toReturn = toReturn + pl.getName()  + " - " + pl.getDescription().getVersion() + NEW_LINE;
		}
		return toReturn;
	}
	
	private String getConfig() {

		FileConfiguration config = Main.plugin.getConfig();
		String cfg = "";
		cfg = cfg + "noParticles: " + config.getBoolean("noParticles") + NEW_LINE;
		cfg = cfg + "existing-resource-pack: " + config.getString("existing-resource-pack") + NEW_LINE;
		
		return cfg;
	}
	
	public String output(){
		
		
		return "Date: " + getDate() + 
				NEW_LINE +
				"UrlToBlock Version: " + Main.plugin.getDescription().getVersion().split(" ")[0] + 
				NEW_LINE + 
				"UUID Key: " + Main.plugin.key +
				NEW_LINE +
				NEW_LINE + 
				"Server Version: " + Bukkit.getVersion() + 
				NEW_LINE +
				"Server Type: " + Main.plugin.SERVER_TYPE.name() +
				NEW_LINE + 
				NEW_LINE + 
				"Exception: " + 
				exception +
				NEW_LINE +
				NEW_LINE +
				"Plugins: " +
				NEW_LINE + 
				getPlugins()+
				NEW_LINE +
				
				"Config: " +
				NEW_LINE +
				getConfig().replaceAll("&", "*");
				
				

	}

	public boolean isFromDumpCommand() {
		return isFromDumpCommand;
	}
}
