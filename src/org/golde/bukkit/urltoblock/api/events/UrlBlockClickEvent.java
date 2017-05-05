package org.golde.bukkit.urltoblock.api.events;

import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.EquipmentSlot;

public class UrlBlockClickEvent extends Event {

	private static final HandlerList handlers = new HandlerList();
	@Override
	public HandlerList getHandlers() {
	    return handlers;
	}
	
	private Player player;
	private int id;
	private Action action;
	private BlockFace blockFace;
	private EquipmentSlot hand;
	public UrlBlockClickEvent(Player player, int id, Action action, BlockFace blockFace, EquipmentSlot hand) {
		this.player = player;
		this.id = id;
		this.action = action;
		this.blockFace = blockFace;
		this.hand = hand;
	}
	
	public Player getPlayer() {
		return player;
	}
	
	public int getUrlBlockId() {
		return id;
	}
	
	public Action getAction() {
		return action;
	}
	
	public BlockFace getBlockFace() {
		return blockFace;
	}
	
	public EquipmentSlot getHand() {
		return hand;
	}
	
}
