package me.pogostick29dev.bloodbath;

import com.sk89q.worldedit.bukkit.selections.CuboidSelection;
import org.bukkit.*;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;

public class Arena {
	
	public enum ArenaState {
		WAITING, COUNTDOWN, STARTED
	}

	private String id;
	private CuboidSelection bounds;
	private ArenaState state;
	
	private ArrayList<Spawn> spawns;
	private ArrayList<GameChest> chests;
	
	private ArrayList<Player> players;
	private ArrayList<BlockState> changedBlocks;
	
	protected Arena(String id) {
		this.id = id;
		
		this.bounds = new CuboidSelection(
				Bukkit.getServer().getWorld(SettingsManager.getArenas().<String>get(id + ".world")),
				Main.loadLocation(SettingsManager.getArenas().<ConfigurationSection>get(id + ".cornerA")),
				Main.loadLocation(SettingsManager.getArenas().<ConfigurationSection>get(id + ".cornerB"))
		);
		
		this.state = ArenaState.WAITING;
		
		this.spawns = new ArrayList<Spawn>();
		if (SettingsManager.getArenas().contains(id + ".spawns")) {
			for (String spawnID : SettingsManager.getArenas().<ConfigurationSection>get(id + ".spawns").getKeys(false)) {
				spawns.add(new Spawn(Main.loadLocation(SettingsManager.getArenas().<ConfigurationSection>get(id + ".spawns." + spawnID))));
			}			
		}
		
		this.chests = new ArrayList<GameChest>();
		if (SettingsManager.getArenas().contains(id + ".chests")) {
			for (String chestID : SettingsManager.getArenas().<ConfigurationSection>get(id + ".chests").getKeys(false)) {
				Location loc = Main.loadLocation(SettingsManager.getArenas().<ConfigurationSection>get(id + ".chests." + chestID + ".location"));
				
				if (loc.getBlock() == null || !(loc.getBlock().getState() instanceof Chest)) {
					SettingsManager.getArenas().set(id + ".chests." + chestID, null);
				}
				
				else {
					chests.add(new GameChest((Chest) loc.getBlock().getState(), SettingsManager.getArenas().<Integer>get(id + ".chests." + chestID + ".tier")));
				}
			}			
		}
		
		this.players = new ArrayList<Player>();
		this.changedBlocks = new ArrayList<BlockState>();
	}
	
	public String getID() {
		return id;
	}
	
	public CuboidSelection getBounds() {
		return bounds;
	}
	
	public int getMaxPlayers() {
		return spawns.size();
	}
	
	public ArenaState getState() {
		return state;
	}
	
	public Player[] getPlayers() {
		return players.toArray(new Player[players.size()]);
	}
	
	public boolean hasPlayer(Player p) {
		return players.contains(p);
	}
	
	public void addPlayer(Player p) {
		if (state == ArenaState.STARTED) {
			p.sendMessage(ChatColor.RED + "This arena has already started.");
			return;
		}
		
		if (players.size() + 1 > spawns.size()) {
			p.sendMessage(ChatColor.RED + "This arena is full.");
			return;
		}
		
		boolean success = false;
		
		for (Spawn spawn : spawns) {
			if (!spawn.hasPlayer()) {
				spawn.setPlayer(p);
				p.teleport(spawn.getLocation());
				success = true;
				break;
			}
		}
		
		if (!success) {
			p.sendMessage(ChatColor.RED + "Could not find spawn.");
			return;
		}
		
		players.add(p);
		
		p.getInventory().clear();
		
		ItemStack kitSelector = new ItemStack(Material.COMPASS, 1);
		ItemMeta meta = kitSelector.getItemMeta();
		meta.setDisplayName(ChatColor.GOLD + "Kit Selector");
		meta.setLore(Arrays.asList("Right click this", "to choose", "your kit."));
		kitSelector.setItemMeta(meta);
		p.getInventory().addItem(kitSelector);
		
		p.sendMessage(ChatColor.GREEN + "You have joined arena " + id + ".");
		
		if (players.size() >= spawns.size() && state == ArenaState.WAITING) {
			this.state = ArenaState.COUNTDOWN;
			new Countdown(this, 30, 30, 20, 10, 5, 4, 3, 2, 1).runTaskTimerAsynchronously(Main.getPlugin(), 0, 20);
		}
	}
	
	public void removePlayer(Player p) {
		players.remove(p);
		
		for (Spawn spawn : spawns) {
			if (spawn.hasPlayer() && spawn.getPlayer().equals(p)) {
				spawn.setPlayer(null);
			}
		}
		
		p.teleport(Bukkit.getServer().getWorlds().get(0).getSpawnLocation()); // TODO: Temporary.
		
		if (players.size() <= 1) {
			if (players.size() == 1) {
				Bukkit.getServer().broadcastMessage(players.get(0).getName() + " has won arena " + id + "!");
				players.remove(0);
				players.get(0).teleport(Bukkit.getServer().getWorlds().get(0).getSpawnLocation()); // TODO: Temporary.
			}
			
			else {
				Bukkit.getServer().broadcastMessage("Arena " + id + " has ended.");
			}
			
			players.clear();
			rollback();
			state = ArenaState.WAITING;
		}
	}
	
	public void addSpawn(Location loc) {
		spawns.add(new Spawn(loc));
	}
	
	public void addChest(Chest chest, int tier) {
		chests.add(new GameChest(chest, tier));
	}
	
	public void addBlockState(BlockState state) {
		changedBlocks.add(state);
	}
	
	private void rollback() {
		for (BlockState state : changedBlocks) {
			state.update(true, false);
		}
		
		changedBlocks.clear();
 	}
	
	public void start() {
		this.state = ArenaState.STARTED;
		
		for (Player p : players) {
			p.setHealth(20.0D);
			p.setGameMode(GameMode.SURVIVAL);
		}
		
		for (Spawn spawn : spawns) {
			spawn.setPlayer(null);
		}
		
		for (GameChest chest : chests) {
			chest.fillChest();
		}
	}
}