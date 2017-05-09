package org.golde.bukkit.urltoblock.api.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class UrlBlockPopulateEvent extends Event{

	public UrlBlockPopulateEvent() {
		
	}
	private static final HandlerList handlers = new HandlerList();

	public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() { //I hate bukkit. All my other events dont need this lile exept for this one. WHY
        return handlers;
    }
	
}
