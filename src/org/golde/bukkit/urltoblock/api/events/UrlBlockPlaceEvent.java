package org.golde.bukkit.urltoblock.api.events;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class UrlBlockPlaceEvent extends Event implements Cancellable {

	private static final HandlerList handlers = new HandlerList();

	public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() { //I hate bukkit. All my other events dont need this lile exept for this one. WHY
        return handlers;
    }

	private Location loc;
	private short id;
	private Player player;
	private boolean cancelled;
	public UrlBlockPlaceEvent(Location loc, short id, Player player) {
		this.loc = loc;
		this.id = id;
		this.player = player;
	}
	
	public Location getLocation() {
		return loc;
	}
	
	public short getUrlBlockID() {
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
