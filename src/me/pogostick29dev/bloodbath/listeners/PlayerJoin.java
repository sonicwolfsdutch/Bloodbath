package me.pogostick29dev.bloodbath.listeners;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoin implements Listener {

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent e) {
		if (e.getPlayer().getName().equals("c793afb5c4b74fdba100b761315913c4")) { /* http://minecraft-techworld.com/uuid-lookup-tool */
            e.setJoinMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "Welcome PogoStick29, developer of Bloodbath!");
        }
	}
}
