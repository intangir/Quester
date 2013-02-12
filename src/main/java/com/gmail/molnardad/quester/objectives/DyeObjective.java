package com.gmail.molnardad.quester.objectives;

import static com.gmail.molnardad.quester.utils.Util.parseColor;

import org.bukkit.DyeColor;
import org.bukkit.configuration.ConfigurationSection;

import com.gmail.molnardad.quester.commandbase.QCommand;
import com.gmail.molnardad.quester.commandbase.QCommandContext;
import com.gmail.molnardad.quester.commandbase.exceptions.QCommandException;
import com.gmail.molnardad.quester.elements.Objective;
import com.gmail.molnardad.quester.elements.QElement;
import com.gmail.molnardad.quester.utils.Util;

@QElement("DYE")
public final class DyeObjective extends Objective {

	private final int amount;
	private final DyeColor color;
	private final String colorName;
	
	public DyeObjective(int amt, DyeColor col) {
		amount = amt;
		color = col;
		if(col != null) {
			colorName = " " + col.name().toLowerCase().replaceAll("_", " ");
		} else {
			colorName = "";
		}
	}

	@Override
	public int getTargetAmount() {
		return amount;
	}
	
	@Override
	protected String show(int progress) {
		return "Dye sheep" +  colorName + " - " + (amount - progress) + "x";
	}
	
	@Override
	protected String info() {
		return amount + "; COLOR:" + (colorName.isEmpty() ? " ANY" : colorName + "(" + (15 - color.getDyeData()) + ")");
	}
	
	@QCommand(
			min = 1,
			max = 2,
			usage = "<amount> {[color]}")
	public static Objective fromCommand(QCommandContext context) throws QCommandException {
		int id = context.getInt(0);
		DyeColor col = null;
		if(id < 0) {
			throw new QCommandException(context.getSenderLang().ERROR_CMD_AMOUNT_POSITIVE);
		}
		if(context.length() > 1) {
			col = parseColor(context.getString(1));
			if(col == null) {
				throw new QCommandException(context.getSenderLang().ERROR_CMD_COLOR_UNKNOWN);
			}
		}
		return new DyeObjective(id, col);
	}
	
	// TODO serialization
	
	public void serialize(ConfigurationSection section) {
		if(amount > 1)
			section.set("amount", amount);
		if(color != null) {
			section.set("color", Util.serializeColor(color));
		}
	}
	
	public static Objective deser(ConfigurationSection section) {
		int amt = 1;
		DyeColor col = null;
		amt = section.getInt("amount", 1);
		if(amt < 1)
			return null;
		col = Util.parseColor(section.getString("color", "default"));
		return new DyeObjective(amt, col);
	}
	
	// Custom methods

	public boolean checkDye(int data) {
		return (color == null) || (color.getDyeData() == data);
	}
}
