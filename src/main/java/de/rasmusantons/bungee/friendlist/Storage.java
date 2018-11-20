package de.rasmusantons.bungee.friendlist;

import net.md_5.bungee.api.ProxyServer;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBException;
import org.iq80.leveldb.Options;
import org.json.JSONArray;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

import static org.iq80.leveldb.impl.Iq80DBFactory.*;

public class Storage {
	private final static File db_file;
	private final static Options options;
	private static Storage instance;
	private static HashMap<UUID, Set<UUID>> requestsByPlayer;
	private static HashMap<UUID, Set<UUID>> requestsForPlayer;

	static {
		db_file = ProxyServer.getInstance().getPluginsFolder().toPath().resolve("friendlist/friends.db").toFile();
		options = new Options();
		options.createIfMissing(true);
		requestsByPlayer = new HashMap<>();
		requestsForPlayer = new HashMap<>();
	}

	public Set<UUID> getFriends(UUID playerId) {
		Set<UUID> friends = new HashSet<>();
		synchronized (Storage.class) {
			try (DB db = factory.open(db_file, options)) {
				byte[] rawFriendsData = db.get(bytes(playerId.toString()));
				if (rawFriendsData != null) {
					JSONArray jsonFriendList = new JSONArray(asString(rawFriendsData));
					for (Object friend : jsonFriendList) {
						friends.add(UUID.fromString((String) friend));
					}
				}
			} catch (IOException | DBException e) {
				ProxyServer.getInstance().getLogger().log(Level.SEVERE, e.getMessage());
			}
		}
		return friends;
	}

	private boolean addFriend(UUID playerId, UUID friendId) {
		Set<UUID> friends = getFriends(playerId);
		if (!friends.add(friendId)) {
			return false;
		}
		JSONArray jsonFriends = new JSONArray(friends);
		synchronized (Storage.class) {
			try (DB db = factory.open(db_file, options)) {
				db.put(bytes(playerId.toString()), bytes(jsonFriends.toString()));
			} catch (IOException | DBException e) {
				ProxyServer.getInstance().getLogger().log(Level.SEVERE, e.getMessage());
			}
		}
		return true;
	}

	public boolean addFriends(UUID playerId, UUID friendId) {
		return addFriend(playerId, friendId) && addFriend(friendId, playerId);
	}

	private boolean removeFriend(UUID playerId, UUID friendId) {
		Set<UUID> friends = getFriends(playerId);
		if (!friends.remove(friendId)) {
			return false;
		}
		JSONArray jsonFriends = new JSONArray(friends);
		synchronized (Storage.class) {
			try (DB db = factory.open(db_file, options)) {
				db.put(bytes(playerId.toString()), bytes(jsonFriends.toString()));
			} catch (IOException | DBException e) {
				ProxyServer.getInstance().getLogger().log(Level.SEVERE, e.getMessage());
			}
		}
		return true;
	}

	public boolean removeFriends(UUID playerId, UUID friendId) {
		return removeFriend(playerId, friendId) && removeFriend(friendId, playerId);
	}

	public boolean isFriend(UUID playerId, UUID friendID) {
		Set<UUID> friends = getFriends(playerId);
		return friends.contains(friendID);
	}

	public Set<UUID> getRequestsByPlayer(UUID uuid) {
		return requestsByPlayer.get(uuid);
	}

	public Set<UUID> getRequestsForPlayer(UUID uuid) {
		return requestsForPlayer.get(uuid);
	}

	public boolean addRequest(UUID fromId, UUID toId) {
		Set<UUID> requestsBy = requestsByPlayer.computeIfAbsent(fromId, k -> new HashSet<>());
		if (!requestsBy.add(toId))
			return false;
		Set<UUID> requestsFor = requestsForPlayer.computeIfAbsent(toId, k -> new HashSet<>());
		return requestsFor.add(fromId);
	}

	public boolean removeRequest(UUID fromId, UUID toId) {
		Set<UUID> requestsBy = requestsByPlayer.get(fromId);
		if (requestsBy == null || !requestsBy.remove(toId))
			return false;
		if (requestsBy.isEmpty())
			requestsByPlayer.put(fromId, null);
		Set<UUID> requestsFor = requestsForPlayer.get(toId);
		if (requestsFor == null || !requestsFor.remove(fromId))
			return false;
		if (requestsFor.isEmpty())
			requestsForPlayer.put(toId, null);
		return true;
	}

	public static Storage getInstance() {
		if (instance == null)
			instance = new Storage();
		return instance;
	}
}
