package com.gmail.molnardad.quester.objectives;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
public final class NpcKillObjective extends Objective {

	public static final String TYPE = "NPCKILL";
	private final String name;
	private final String strName;
	private final int amount;
	
	public NpcKillObjective(String name, int amt) {
		this.name = name;
		amount = amt;
		strName = name == null ? "any NPC" : "NPC named " + name;
	}
	
	@Override
	public String getType() {
		return TYPE;
	}
	
	@Override
	public int getTargetAmount() {
		return amount;
	}
	
	@Override
	public boolean isComplete(Player player, int progress) {
		return progress >= amount;
	}
	
	@Override
	public String progress(int progress) {
		if(!desc.isEmpty()) {
			return ChatColor.translateAlternateColorCodes('&', desc).replaceAll("%r", String.valueOf(amount - progress)).replaceAll("%t", String.valueOf(amount));
		}
		return "Kill " + strName + " - " + (amount-progress) + "x";
	}
	
	@Override
	public String toString() {
		return TYPE + ": " + (name == null ? "ANY" : name) + "; AMT: " + amount + coloredDesc();
	}
	
	public boolean checkNpc(String npcName) {
		if(name == null) {
			return true;
		}
		return name.equalsIgnoreCase(ChatColor.stripColor(npcName));
	}

	@Override
	public void serialize(ConfigurationSection section) {
		super.serialize(section, TYPE);
		section.set("name", name);
		if(amount > 1) {
			section.set("amount", amount);
		}
	}
	
	public static Objective deser(ConfigurationSection section) {
		int amt = 1;
		String nm = null;
		amt = section.getInt("amount", 1);
		if(amt < 1)
			return null;
		nm = section.getString("name", null);
		return new NpcKillObjective(nm, amt);
	}
}
