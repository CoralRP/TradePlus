package com.trophonix.tradeplus.events;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/***
 * Called when a trade ends.
 */
@Getter
public class TradeEndEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final Player sender;
    private final Player receiver;

    public TradeEndEvent(Player sender, Player receiver) { // idw why but they are swapped and its working
        this.sender = sender;
        this.receiver = receiver;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

}
