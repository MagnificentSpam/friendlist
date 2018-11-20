package de.rasmusantons.bungee.friendlist;

import net.md_5.bungee.api.ProxyServer;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.UUID;
import java.util.logging.Level;

public class MojangApi {
	private static String PROFILES_URL = "https://api.mojang.com/users/profiles/minecraft/%s";
	private static String NAMES_URL = "https://api.mojang.com/user/profiles/%s/names";

	private static UUID uuidFromString(String id) {
		return UUID.fromString(id.substring(0, 8) + "-" + id.substring(8, 12) + "-"
				+ id.substring(12, 16) + "-" + id.substring(16, 20) + "-" +id.substring(20, 32));
	}

	private static String getHTTP(URL url) throws IOException {
		StringBuilder result = new StringBuilder();
		HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
		connection.setRequestMethod("GET");
		connection.setRequestProperty("Content-Type", "application/json");
		if (connection.getResponseCode() != 200)
			return null;
		BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		String line;
		while ((line = reader.readLine()) != null) {
			result.append(line);
		}
		reader.close();
		return result.toString();
	}

	public static OfflinePlayer findPlayerByName(String username) {
		try {
			URL url = new URL(String.format(PROFILES_URL, username));
			String queryResponse = getHTTP(url);
			if (queryResponse == null)
				return null;
			JSONObject jsonResponse = new JSONObject(queryResponse);
			OfflinePlayer player = new OfflinePlayer();
			player.id = uuidFromString(jsonResponse.getString("id"));
			player.name = jsonResponse.getString("name");
			return player;
		} catch (IOException e) {
			ProxyServer.getInstance().getLogger().log(Level.SEVERE, e.getMessage());
			return null;
		}
	}

	public static OfflinePlayer findPlayerById(UUID id) {
		try {
			URL url = new URL(String.format(NAMES_URL, id.toString().replace("-", "")));
			String queryResponse = getHTTP(url);
			if (queryResponse == null)
				return null;
			OfflinePlayer player = new OfflinePlayer();
			player.id = id;
			JSONArray jsonResponse = new JSONArray(queryResponse);
			for (Object nameState : jsonResponse)
				player.name = ((JSONObject) nameState).getString("name");
			return player;
		} catch (IOException e) {
			ProxyServer.getInstance().getLogger().log(Level.SEVERE, e.getMessage());
			return null;
		}
	}

	public static class OfflinePlayer {
		public UUID id;
		public String name;
	}
}
