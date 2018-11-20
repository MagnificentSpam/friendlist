package de.rasmusantons.bungee.friendlist;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

import java.util.Set;
import java.util.UUID;

public class FriendList extends Plugin implements Listener {

	@Override
	public void onEnable() {
		getProxy().getPluginManager().registerListener(this, this);
		getProxy().getPluginManager().registerCommand(this, new FriendCommand(this));
	}

	@EventHandler
	public void onLogin(PostLoginEvent event) {
		ProxiedPlayer player = event.getPlayer();
		Set<UUID> openRequests = Storage.getInstance().getRequestsForPlayer(player.getUniqueId());
		if (openRequests != null) {
			ProxyServer.getInstance().getScheduler().runAsync(this, () -> {
				ComponentBuilder messageBuilder = new ComponentBuilder(String.format("You have %d open friend requests:", openRequests.size())).color(ChatColor.GOLD);
				for (UUID friendId : openRequests) {
					MojangApi.OfflinePlayer friend = MojangApi.findPlayerById(friendId);
					if (friend == null) {
						messageBuilder.append("ERROR").reset().color(ChatColor.RED);
						continue;
					}
					messageBuilder.append(String.format("\n%s    ", friend.name)).reset().color(ChatColor.GOLD);
					messageBuilder.append("ACCEPT").color(ChatColor.GREEN).event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, String.format("/f accept %s 1", friend.id)));
					messageBuilder.append("    ").reset();
					messageBuilder.append("DENY").color(ChatColor.RED).event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, String.format("/f accept %s 0", friend.id)));
				}
				player.sendMessage(messageBuilder.create());
			});
		}
	}
}
