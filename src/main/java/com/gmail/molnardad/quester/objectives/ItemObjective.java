package com.gmail.molnardad.quester.objectives;

import static com.gmail.molnardad.quester.utils.Util.parseEnchants;
import static com.gmail.molnardad.quester.utils.Util.parseItem;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.gmail.molnardad.quester.commandbase.QCommand;
import com.gmail.molnardad.quester.commandbase.QCommandContext;
import com.gmail.molnardad.quester.elements.Objective;
import com.gmail.molnardad.quester.elements.QElement;
import com.gmail.molnardad.quester.managers.QuestManager;
import com.gmail.molnardad.quester.utils.Util;

@QElement("ITEM")
public final class ItemObjective extends Objective {

	private final Material material;
	private final short data;
	private final int amount;
	private final Map<Integer, Integer> enchants;
	
	public ItemObjective(Material mat, int amt, int dat, Map<Integer, Integer> enchs) {
		material = mat;
		amount = amt;
		data = (short)dat;
		if(enchs != null)
			this.enchants = enchs;
		else
			this.enchants = new HashMap<Integer, Integer>();
	}
	
	@Override
	public int getTargetAmount() {
		return 1;
	}
	
	@Override
	protected String show(int progress) {
		String datStr = data < 0 ? " (any) " : " (data " + data + ") ";
		String pcs = amount == 1 ? " piece of " : " pieces of ";
		String enchs = "\n -- Enchants:";
		String mat = material.getId() == 351 ? "dye" : material.name().toLowerCase();
		if(enchants.isEmpty()) {
			enchs = "";
		}
		for(Integer i : enchants.keySet()) {
			enchs = enchs + " " + Util.enchantName(i, enchants.get(i)) + ";";
		}
		return "Have " + amount + pcs + mat + datStr + "on completion." + enchs;
	}
	
	@Override
	protected String info() {
		String dataStr = (data < 0 ? "" : ":" + data);
		String itm = material.name() + "["+material.getId() + dataStr + "]; AMT: " + amount;
		String enchs = enchants.isEmpty() ? "" : "\n -- ENCH:";
		for(Integer e : enchants.keySet()) {
			enchs = enchs + " " + Enchantment.getById(e).getName() + ":" + enchants.get(e);
		}
		return itm + enchs;
	}

	@Override
	public boolean tryToComplete(Player player) {
		Inventory newInv = QuestManager.createInventory(player);
		if(takeInventory(newInv)) {
			takeInventory(player.getInventory());
			return true;
		}
		return false;
	}
	
	@QCommand(
			min = 1,
			max = 3,
			usage = "{<item>} [amount] {[enchants]}")
	public static Objective fromCommand(QCommandContext context) {
		Material mat = null;
		int dat;
		int amt = 1;
		Map<Integer, Integer> enchs = null;
		int[] itm = parseItem(context.getString(0), context.getSenderLang());
		mat = Material.getMaterial(itm[0]);
		dat = itm[1];
		if(context.length() > 1) {
			amt = context.getInt(1);
			if(context.length() > 2) {
				enchs = parseEnchants(context.getString(2));
			}
		}
		if(amt < 1 || dat < -1) {
			throw new IllegalArgumentException(context.getSenderLang().ERROR_CMD_ITEM_NUMBERS);
		}
		if(context.length() > 2) {
			enchs = parseEnchants(context.getString(2));
		}
		
		return new ItemObjective(mat, dat, amt, enchs);
	}

	// TODO serialization
	
	public void serialize(ConfigurationSection section) {
		section.set("item", Util.serializeItem(material, data));
		if(!enchants.isEmpty())
			section.set("enchants", Util.serializeEnchants(enchants));
		if(amount != 1)
			section.set("amount", amount);
	}
	
	public static Objective deser(ConfigurationSection section) {
		Material mat = null;
		int dat = -1, amt = 1;
		Map<Integer, Integer> enchs = null;
		try {
			int[] itm = Util.parseItem(section.getString("item", ""));
			mat = Material.getMaterial(itm[0]);
			dat = itm[1];
			
			if(section.isInt("amount"))
				amt = section.getInt("amount");
			if(amt < 1)
				amt = 1;
			
			if(section.isString("enchants"))
				try {
					enchs = Util.parseEnchants(section.getString("enchants"));
				} catch (IllegalArgumentException e) {
					enchs = null;
				}
		} catch (Exception e) {
			return null;
		}
		
		return new ItemObjective(mat, amt, dat, enchs);
	}
	
	//Custom methods
	
	public boolean takeInventory(Inventory inv) {
		int remain = amount;
		ItemStack[] contents = inv.getContents();
		for (int i = 0; i <contents.length; i++) {
	        if (contents[i] != null) {
	        	boolean enchsOK = true;
	        	for(Integer e : enchants.keySet()) {
	        		if(enchants.get(e) != contents[i].getEnchantmentLevel(Enchantment.getById(e))) {
	        			enchsOK = false;
	        			break;
	        		}
	        	}
	        	if(enchsOK) {
		        	if (contents[i].getTypeId() == material.getId()) {
		        		if(data < 0 || contents[i].getDurability() == data) {
		        			if(remain >= contents[i].getAmount()) {
		        				remain -= contents[i].getAmount();
		        				contents[i] = null;
		        				inv.clear(i);
		        			} else {
		        				contents[i].setAmount(contents[i].getAmount() - remain);
		        				remain = 0;
		        				break;
		        			}
		        		}
		        	}
	        	}
	        }
	    }
		return remain == 0;
	}
}
