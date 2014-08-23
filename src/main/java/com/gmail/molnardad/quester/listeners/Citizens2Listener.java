package com.gmail.molnardad.quester.listeners;

import java.util.List;

import net.citizensnpcs.api.event.NPCDeathEvent;
import net.citizensnpcs.api.event.NPCLeftClickEvent;
import net.citizensnpcs.api.event.NPCRightClickEvent;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.gmail.molnardad.quester.Quest;
import com.gmail.molnardad.quester.QuestData;
import com.gmail.molnardad.quester.QuestHolder;
import com.gmail.molnardad.quester.QuestManager;
import com.gmail.molnardad.quester.Quester;
import com.gmail.molnardad.quester.QuesterTrait;
import com.gmail.molnardad.quester.exceptions.QuesterException;
import com.gmail.molnardad.quester.objectives.NpcKillObjective;
import com.gmail.molnardad.quester.objectives.NpcObjective;
import com.gmail.molnardad.quester.objectives.Objective;
import com.gmail.molnardad.quester.utils.Util;

public class Citizens2Listener implements Listener {
	
	QuestManager qm = Quester.qMan;
	
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onLeftClick(NPCLeftClickEvent event) {
		if(event.getNPC().hasTrait(QuesterTrait.class)) {
			event.setCancelled(true);
			QuestHolder qh = qm.getHolder(event.getNPC().getTrait(QuesterTrait.class).getHolderID());
			Player player = event.getClicker();
			if(!Util.permCheck(player, QuestData.PERM_USE_NPC, true)) {
				return;
			}
			// If player has perms and holds blaze rod
			boolean isOp = Util.permCheck(player, QuestData.MODIFY_PERM, false);
			if(isOp) {
				if(player.getItemInHand().getTypeId() == 369) {
					event.getNPC().getTrait(QuesterTrait.class).setHolderID(-1);
					player.sendMessage(ChatColor.GREEN + Quester.strings.HOL_UNASSIGNED);
				    return;
				}
			}
			if(qh == null) {
				player.sendMessage(ChatColor.RED + Quester.strings.ERROR_HOL_NOT_ASSIGNED);
				return;
			}
			try {
				qh.selectNext();
			} catch (QuesterException e) {
				player.sendMessage(e.message());
				if(!isOp) {
					return;
				}
				
			}
			
			player.sendMessage(Util.line(ChatColor.BLUE, event.getNPC().getName() + "'s quests", ChatColor.GOLD));
			if(isOp) {
				qh.showQuestsModify(player);
			} else {
				qh.showQuestsUse(player);
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onRightClick(NPCRightClickEvent event) {
		if(event.getNPC().hasTrait(QuesterTrait.class)) {
			event.setCancelled(true);
			QuestManager qm = Quester.qMan;
			QuestHolder qh = qm.getHolder(event.getNPC().getTrait(QuesterTrait.class).getHolderID());
			Player player = event.getClicker();
			if(!Util.permCheck(player, QuestData.PERM_USE_NPC, true)) {
				return;
			}
			boolean isOP = Util.permCheck(player, QuestData.MODIFY_PERM, false);
			// If player has perms and holds blaze rod
			if(isOP) {
				if(player.getItemInHand().getTypeId() == 369) {
					int sel = qm.getSelectedHolderID(player.getName());
					if(sel < 0){
						player.sendMessage(ChatColor.RED + Quester.strings.ERROR_HOL_NOT_SELECTED);
					} else {
						event.getNPC().getTrait(QuesterTrait.class).setHolderID(sel);
						player.sendMessage(ChatColor.GREEN + Quester.strings.HOL_ASSIGNED);
					}
				    return;
				}
			}
			if(qh == null) {
				player.sendMessage(ChatColor.RED + Quester.strings.ERROR_HOL_NOT_ASSIGNED);
				return;
			}
			int selected = qh.getSelected();
			Quest progressed = null;
			List<Integer> qsts = qh.getQuests();
			
			List<Quest> currentQuests = qm.getPlayerQuests(player.getName());
			if(!player.isSneaking()) {

				// cycle through all quests they have, determine the first that can be completed, or at least progressed
				for(Quest quest : currentQuests) {
					int questID = quest.getID();

					// quest giver accepts this quest
					if(questID >= 0 && qsts.contains(questID)) {
						try {
							// switch and see if its completed
							qm.switchQuest(player, quest);
							qm.complete(player, false);
							return;
						} catch (QuesterException e) {
							// must not be complete, save it for if we resort to displaying progress instead, only show progress if we had it selected 
							if(progressed == null && questID == selected) {
								progressed = quest;
							}
						}
					}
				}
				
				// no quests completed, did any progress at least?
				if(progressed != null) {
					try {
						qm.switchQuest(player, progressed);
						qm.showProgress(player);
						return;
					} catch (QuesterException f) {
						player.sendMessage(ChatColor.DARK_PURPLE + Quester.strings.ERROR_INTERESTING);
					}
				} else if(currentQuests.size() > 0){
					// reselect first
					qm.switchQuest(player, currentQuests.get(0));
				}
				// nothing useful done so far.. check if they have more quests
			}
			// take a quest from them
			if(qm.isQuestActive(selected)) {
				try {
					qm.startQuest(player, qm.getQuestNameByID(selected), false);
					qh.selectNext();
				} catch (QuesterException e) {
					player.sendMessage(e.message());
				}
			} else {
				player.sendMessage(ChatColor.RED + Quester.strings.ERROR_Q_NOT_SELECTED);
			}
		}
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void onAnyClick(NPCRightClickEvent event) {
		Player player = event.getClicker();
		// try the NPC against ALL of your quests..
		List<Quest> currentQuests = qm.getPlayerQuests(player.getName());
		for(Quest quest : currentQuests) {
			if(quest != null) {
		    	if(!quest.allowedWorld(player.getWorld().getName().toLowerCase()))
		    		continue;
		    	List<Objective> objs = quest.getObjectives();
		    	for(int i = 0; i < objs.size(); i++) {
		    		if(objs.get(i).getType().equalsIgnoreCase("NPC")) {
		    			qm.switchQuest(player, quest);
		    			if(!qm.isObjectiveActive(player, i)){
		    				continue;
		    			}
		    			NpcObjective obj = (NpcObjective)objs.get(i);
		    			if(obj.checkNpc(event.getNPC().getId())) {
		    				qm.incProgress(player, i);
		    				if(obj.getCancel()) {
		    					event.setCancelled(true);
		    				}
		    				return;
		    			}
		    		}
		    	}
		    }
	    }
		if(currentQuests.size() > 0){
			// reselect first
			qm.switchQuest(player, currentQuests.get(0));
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onNpcDeath(NPCDeathEvent event) {
		Player player = event.getNPC().getBukkitEntity().getKiller();
		if(player == null) {
			return;
		}
		// try the NPC against ALL of your quests..
		List<Quest> currentQuests = qm.getPlayerQuests(player.getName());
		for(Quest quest : currentQuests) {
	    	if(!quest.allowedWorld(player.getWorld().getName().toLowerCase()))
	    		return;
	    	List<Objective> objs = quest.getObjectives();
	    	for(int i = 0; i < objs.size(); i++) {
	    		if(objs.get(i).getType().equalsIgnoreCase("NPCKILL")) {
	    			qm.switchQuest(player, quest);
	    			if(!qm.isObjectiveActive(player, i)){
	    				continue;
	    			}
	    			NpcKillObjective obj = (NpcKillObjective)objs.get(i);
	    			if(obj.checkNpc(event.getNPC().getName())) {
	    				qm.incProgress(player, i);
	    				return;
	    			}
	    		}
	    	}
	    }
		if(currentQuests.size() > 0){
			// reselect first
			qm.switchQuest(player, currentQuests.get(0));
		}
	}
}
