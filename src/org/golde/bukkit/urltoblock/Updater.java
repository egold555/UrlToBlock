package org.golde.bukkit.urltoblock;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Scanner;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class Updater
{

	public enum UpdateResult
	{
		NO_UPDATE,
		DISABLED,
		FAIL,
		UPDATE_AVAILABLE,
		UPDATE_SUCCESS,
		DEV;
	}

	public class UpdateResults
	{
		private UpdateResult result;
		private String version;

		public UpdateResults(UpdateResult result, String version)
		{
			this.result = result;
			this.version = version;
		}

		public UpdateResult getResult()
		{
			return result;
		}

		public String getVersion()
		{
			return version;
		}
	}

	private String APIKey = "98BE0FE67F88AB82B4C197FAF1DC3B69206EFDCC4D3B80FC83A00037510B99B4";
	private String currentVersion;

	private String resourceId;
	private Plugin plugin;

	public Updater(Plugin plugin, String resourceUrl)
	{
		this.resourceId = resourceUrl;
		this.plugin = plugin;

		currentVersion = Main.plugin.getDescription().getVersion().split(" ")[0];
	}

	public UpdateResults checkForUpdates()
	{
		if(plugin.getConfig().getBoolean("enable-update-checker", false))
		{
			try
			{
				HttpURLConnection con = (HttpURLConnection) new URL("http://www.spigotmc.org/api/general.php").openConnection();

				con.setDoOutput(true);
				con.setRequestMethod("POST");
				con.getOutputStream()
				.write(("key=" + APIKey + "&resource=" + resourceId).getBytes("UTF-8"));

				if (con.getResponseCode() == 500)
					return new UpdateResults(UpdateResult.FAIL, "Server responded with code 500: Internal server error");

				String version = new BufferedReader(new InputStreamReader(con.getInputStream())).readLine();

				con.disconnect();

				switch(compareVersion(version)){
				case 0: return new UpdateResults(UpdateResult.NO_UPDATE, null);
				case -1: return new UpdateResults(UpdateResult.UPDATE_AVAILABLE, version);
				case 1: return new UpdateResults(UpdateResult.DEV, version);
				default: return new UpdateResults(UpdateResult.FAIL, "This was not suppost to happen!");
				}

			}
			catch (Exception ex)
			{
				return new UpdateResults(UpdateResult.FAIL, ex.toString());
			}

		}
		else
			return new UpdateResults(UpdateResult.DISABLED, null);

	}

	public UpdateResults downloadUpdate() {
		if (plugin.getConfig().getBoolean("auto-download-updates", false)) {
			File jar = new File(URLDecoder.decode(((JavaPlugin)plugin).getClass().getProtectionDomain().getCodeSource().getLocation().getPath()));
			try {
				URL url = new URL("https://api.spiget.org/v2/resources/"+resourceId+"/download");

				ReadableByteChannel rbc = Channels.newChannel(url.openStream());
				FileOutputStream fos = new FileOutputStream(jar, false);
				fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
				fos.close();
				return new UpdateResults(UpdateResult.UPDATE_SUCCESS, null);
			} catch(IOException e) {
				e.printStackTrace();
				return new UpdateResults(UpdateResult.FAIL, e.toString());
			}
		}
		else
			return new UpdateResults(UpdateResult.DISABLED, null);
	}

	private int compareVersion(String newVersion)
	{
		Scanner currentV = new Scanner(currentVersion);
		Scanner newV = new Scanner(newVersion);

		currentV.useDelimiter("\\.");
		newV.useDelimiter("\\.");

		while(currentV.hasNextInt() && newV.hasNextInt())
		{
			int cV = currentV.nextInt();
			int nV = newV.nextInt();

			if(cV < nV)
			{
				currentV.close();
				newV.close();

				return -1;
			}
			else if(cV > nV)
			{
				currentV.close();
				newV.close();

				return 1;
			}
		}

		if(currentV.hasNextInt())
		{
			currentV.close();
			newV.close();

			return 1;
		}

		if(newV.hasNextInt())
		{
			currentV.close();
			newV.close();

			return -1;
		}

		currentV.close();
		newV.close();
		return 0;
	}
}
