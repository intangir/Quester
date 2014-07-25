package com.gmail.molnardad.quester;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.gmail.molnardad.quester.exceptions.ExceptionType;
import com.gmail.molnardad.quester.exceptions.QuesterException;
import com.gmail.molnardad.quester.utils.Util;

import static com.gmail.molnardad.quester.Quester.qMan;

public class QuestHolder {

	private List<Integer> heldQuests = new ArrayList<Integer>();
	private int selected = -1;
	private String name;
	
	public QuestHolder(String name) {
		this.name = name;
	}
	
	public void setname(String newName) {
		name = newName;
	}
	
	public String getName() {
		return name;
	}
	
	public List<Integer> getQuests() {
		return heldQuests;
	}
	
	public int getSelected() {
		if(heldQuests.size() > selected && selected >= 0) {
			return heldQuests.get(selected);
		} else if(heldQuests.size() == 1 && selected == -1) {
			// if holder has only 1 quest, default to it without needing to select it
			return selected = 0;
		} else
			return selected = -1;
	}
	
	public int getSelectedIndex() {
		return selected;
	}
	
	public void selectNext() throws QuesterException {
		if(heldQuests.isEmpty())
			throw new QuesterException(ExceptionType.Q_NONE);
		if(getSelected() == -1) {
			selected = 0;
			if(qMan.isQuestActive(heldQuests.get(0)))
				return;
		}
		int i = selected;
		boolean notChosen = true;
		while(notChosen) {
			if(i < heldQuests.size()-1)
				i++;
			else
				i = 0;
			if(qMan.isQuestActive(heldQuests.get(i))) {
				selected = i;
				notChosen = false;
			} else if(i == selected) {
				throw new QuesterException(ExceptionType.Q_NONE_ACTIVE);
			}
		}
	}
	
	private void checkQuests() {
		for(int i=heldQuests.size()-1; i>=0; i--) {
			if(!qMan.isQuest(heldQuests.get(i))) {
				heldQuests.remove(i);
			}
		}
	}
	
	public void addQuest(int questID) {
		if(!heldQuests.contains(questID)) {
			heldQuests.add(questID);
			checkQuests();
		}
	}
	
	public void removeQuest(int questID) {
		for(int i=0; i<heldQuests.size(); i++) {
			if(questID == heldQuests.get(i)) {
				heldQuests.remove(i);
				break;
			}
		}
		checkQuests();
	}
	
	public void moveQuest(int from, int to) throws QuesterException {
		try{
			heldQuests.get(from);
			heldQuests.get(to);
			Util.moveListUnit(heldQuests, from, to);
		} catch (IndexOutOfBoundsException e) {
			throw new QuesterException(Quester.strings.ERROR_CMD_ID_OUT_OF_BOUNDS);
		}
	}
	
	public void showQuestsUse(Player player) {
		for(int i=0; i<heldQuests.size(); i++) {
			if(qMan.isQuestActive(heldQuests.get(i))) {
				player.sendMessage((i == selected ? ChatColor.GREEN : ChatColor.BLUE) + " - " + qMan.getQuestNameByID(heldQuests.get(i)));
			}
		}
	}
	
	public void showQuestsModify(CommandSender sender){
		sender.sendMessage(ChatColor.GOLD + "Holder name: " + ChatColor.RESET + name);
		for(int i=0; i<heldQuests.size(); i++) {
			ChatColor col = qMan.isQuestActive(heldQuests.get(i)) ? ChatColor.BLUE : ChatColor.RED;
			
			sender.sendMessage(i + ". " + (i == selected ? ChatColor.GREEN : ChatColor.BLUE) + "[" + heldQuests.get(i) + "] " + col + qMan.getQuestNameByID(heldQuests.get(i)));
		}
	}
	
	public Map<String, Object> serialize() {
		Map<String, Object> map = new HashMap<String, Object>();
		String str = "";
		boolean first = true;
		for(int i : heldQuests) {
			if(first) {
				str += String.valueOf(i);
				first = false;
			} else 
				str += "," + i;
		}
		map.put("name", name);
		map.put("quests", str);
		
		return map;
	}
	
	public static QuestHolder deserialize(ConfigurationSection section) {
		QuestHolder qHolder = null;
		try{
			if(section == null)
				return null;
			String name = section.getString("name", "QuestHolder");
			String str = section.getString("quests", "");
			
			qHolder = new QuestHolder(name);
			String[] split = str.split(",");
			
			int i;
			for(String s : split) {
				try {
					i = Integer.parseInt(s);
					qHolder.addQuest(i);
				} catch (NumberFormatException f) {
				}
			}
			
		} catch (Exception e) {
		}
		
		if(qHolder != null)
			qHolder.checkQuests();
		return qHolder;
	}
}
