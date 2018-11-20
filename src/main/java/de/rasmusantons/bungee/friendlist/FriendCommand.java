package de.rasmusantons.bungee.friendlist;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.util.Set;
import java.util.UUID;

public class FriendCommand extends Command {

	private static BaseComponent[] USAGE;

	private FriendList main;

	static {
		USAGE = new ComponentBuilder("/f list\n/f add <name>\n/f remove <name>\n/f msg <name> <message...>").color(ChatColor.RED).create();
	}

	public FriendCommand(FriendList main) {
		super("friend", "permission.friends", "f");
		this.main = main;
	}

	@Override
	public void execute(CommandSender sender, String[] args) {
		if (!(sender instanceof ProxiedPlayer)) {
			sender.sendMessage(new ComponentBuilder("only players can use this command").create());
			return;
		}
		ProxiedPlayer player = (ProxiedPlayer) sender;
		if (args.length == 0) {
			player.sendMessage(USAGE);
			return;
		}
		switch (args[0]) {
			case "list": {
				ProxyServer.getInstance().getScheduler().runAsync(main, () -> {
					Set<UUID> friends = Storage.getInstance().getFriends(((ProxiedPlayer) sender).getUniqueId());
					ComponentBuilder friendListBuilder = new ComponentBuilder("");
					int onlineFriends = 0;
					for (UUID uuid : friends) {
						ProxiedPlayer friend = main.getProxy().getPlayer(uuid);
						if (friend != null && friend.isConnected()) {
							friendListBuilder = friendListBuilder.append(friend.getName() + " ").color(ChatColor.GREEN)
									.event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, String.format("/f msg %s ", friend.getName())));
							++onlineFriends;
						} else {
							MojangApi.OfflinePlayer offlineFriend = MojangApi.findPlayerById(uuid);
							if (offlineFriend == null) {
								friendListBuilder = friendListBuilder.append("ERROR" + " ").reset().color(ChatColor.RED);
								continue;
							}
							friendListBuilder = friendListBuilder.append(offlineFriend.name + " ").reset().color(ChatColor.RED);
						}
					}
					if (friends.size() == 0) {
						friendListBuilder = friendListBuilder.append("Your friend list is empty. Add friends by typing /f add <name>.").color(ChatColor.RED);
					}
					ComponentBuilder headerBuilder = new ComponentBuilder(String.format("Friends (%d/%d online):", onlineFriends, friends.size())).color(ChatColor.GOLD);
					player.sendMessage(headerBuilder.create());
					player.sendMessage(friendListBuilder.create());
				});
				break;
			}
			case "add": {
				ProxyServer.getInstance().getScheduler().runAsync(main, () -> {
					if (args.length != 2) {
						player.sendMessage(USAGE);
						return;
					}
					MojangApi.OfflinePlayer friend = MojangApi.findPlayerByName(args[1]);
					if (friend == null) {
						player.sendMessage(new ComponentBuilder(String.format("Player not found: %s", args[1])).color(ChatColor.RED).create());
						return;
					}
					if (friend.id.equals(player.getUniqueId())) {
						player.sendMessage(new ComponentBuilder("You cannot add yourself.").color(ChatColor.RED).create());
						return;
					}
					if (Storage.getInstance().isFriend(player.getUniqueId(), friend.id)) {
						player.sendMessage(new ComponentBuilder(String.format("%s is already on your friend list.", friend.name)).color(ChatColor.RED).create());
						return;
					}
					boolean success = Storage.getInstance().addRequest(player.getUniqueId(), friend.id);
					if (!success) {
						player.sendMessage(new ComponentBuilder(String.format("You already sent a request to %s.", friend.name)).color(ChatColor.RED).create());
						return;
					}
					player.sendMessage(new ComponentBuilder(String.format("Sent a friend request to %s.", friend.name)).color(ChatColor.GREEN).create());
					ProxiedPlayer onlineFriend = main.getProxy().getPlayer(friend.id);
					if (onlineFriend != null) {
						ComponentBuilder messageBuilder = new ComponentBuilder(String.format("%s sent you a friend request.\n", player.getName())).color(ChatColor.GOLD);
						messageBuilder.append("ACCEPT").color(ChatColor.GREEN).event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, String.format("/f accept %s 1", player.getUniqueId())));
						messageBuilder.append("    ").reset();
						messageBuilder.append("DENY").color(ChatColor.RED).event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, String.format("/f accept %s 0", player.getUniqueId())));
						onlineFriend.sendMessage(messageBuilder.create());
					}
				});
				break;
			}
			case "remove": {
				ProxyServer.getInstance().getScheduler().runAsync(main, () -> {
					if (args.length != 2) {
						player.sendMessage(USAGE);
						return;
					}
					MojangApi.OfflinePlayer friend = MojangApi.findPlayerByName(args[1]);
					if (friend == null) {
						player.sendMessage(new ComponentBuilder(String.format("Player not found: %s", args[1])).color(ChatColor.RED).create());
						return;
					}
					boolean success = (Storage.getInstance().removeFriends(player.getUniqueId(), friend.id));
					if (!success) {
						player.sendMessage(new ComponentBuilder(String.format("%s is not on your friend list", friend.name)).color(ChatColor.RED).create());
						return;
					}
					Storage.getInstance().removeFriends(friend.id, player.getUniqueId());
					player.sendMessage(new ComponentBuilder(String.format("Removed %s from your friend list", friend.name)).color(ChatColor.GREEN).create());
				});
				break;
			}
			case "accept": {
				if (args.length != 3) {
					player.sendMessage(USAGE);
					return;
				}
				UUID friendID;
				try {
					friendID = UUID.fromString(args[1]);
				} catch (IllegalArgumentException e) {
					player.sendMessage(USAGE);
					return;
				}
				ProxyServer.getInstance().getScheduler().runAsync(main, () -> {
					boolean accepted = args[2].equals("1");
					boolean success = Storage.getInstance().removeRequest(friendID, player.getUniqueId());
					Storage.getInstance().removeRequest(player.getUniqueId(), player.getUniqueId());
					if (!success) {
						player.sendMessage(new ComponentBuilder(String.format("friend request expired")).color(ChatColor.RED).create());
						return;
					}
					MojangApi.OfflinePlayer friend = MojangApi.findPlayerById(friendID);
					if (friend == null) {
						friend = new MojangApi.OfflinePlayer();
						friend.id = friendID;
						friend.name = "player";
					}
					if (accepted) {
						Storage.getInstance().addFriends(player.getUniqueId(), friendID);
						player.sendMessage(new ComponentBuilder(String.format("Accepted friend request from %s", friend.name)).color(ChatColor.GREEN).create());
					} else {
						player.sendMessage(new ComponentBuilder(String.format("Denied friend request from %s", friend.name)).color(ChatColor.RED).create());
					}
					ProxiedPlayer onlineFriend = main.getProxy().getPlayer(friendID);
					if (onlineFriend != null) {
						if (accepted)
							onlineFriend.sendMessage(new ComponentBuilder(String.format("%s accepted your friend request.", player.getName())).color(ChatColor.GREEN).create());
						else
							onlineFriend.sendMessage(new ComponentBuilder(String.format("%s denied your friend request.", player.getName())).color(ChatColor.RED).create());
					}
				});
				break;
			}
			case "msg": {
				if (args.length < 3) {
					player.sendMessage(USAGE);
					return;
				}
				MojangApi.OfflinePlayer friend = MojangApi.findPlayerByName(args[1]);
				if (friend == null) {
					player.sendMessage(new ComponentBuilder(String.format("Player not found: %s", args[1])).color(ChatColor.RED).create());
					return;
				}
				if (!Storage.getInstance().isFriend(player.getUniqueId(), friend.id)) {
					player.sendMessage(new ComponentBuilder(String.format("%s is not on your friend list.", friend.name)).color(ChatColor.RED).create());
					return;
				}
				ProxiedPlayer onlineFriend = main.getProxy().getPlayer(friend.id);
				if (onlineFriend == null) {
					player.sendMessage(new ComponentBuilder(String.format("%s is offline.", friend.name)).color(ChatColor.RED).create());
					return;
				}
				ComponentBuilder messagePrefixSelfBuilder = new ComponentBuilder(String.format("Me -> %s: ", friend.name)).color(ChatColor.GOLD);
				ComponentBuilder messagePrefixFriendBuilder = new ComponentBuilder(String.format("%s -> Me: ", player.getName())).color(ChatColor.GOLD);
				StringBuilder messageBuilder = new StringBuilder();
				for (int i = 2; i < args.length; ++i) {
					messageBuilder.append(args[i]);
					messageBuilder.append(" ");
				}
				player.sendMessage(messagePrefixSelfBuilder.append(messageBuilder.toString()).reset().create());
				onlineFriend.sendMessage(messagePrefixFriendBuilder.append(messageBuilder.toString()).reset().create());
				break;
			}
			default: {
				player.sendMessage(USAGE);
				break;
			}
		}
	}
}
