package org.golde.bukkit.urltoblock.api.events;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class UrlBlockBreakEvent extends Event implements Cancellable {

	private static final HandlerList handlers = new HandlerList();
	@Override
	public HandlerList getHandlers() {
	    return handlers;
	}

	private Location loc;
	private int id;
	private Player player;
	private boolean cancelled;
	public UrlBlockBreakEvent(Location loc, int id, Player player) {
		this.loc = loc;
		this.id = id;
		this.player = player;
	}
	
	public Location getLocation() {
		return loc;
	}
	
	public int getUrlBlockID() {
		return id;
	}
	
	public Player getPlayer() {
		return player;
	}

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}
	
}
