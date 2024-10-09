package com.trophonix.tradeplus.listener;

import com.trophonix.tradeplus.TradePlus;
import com.trophonix.tradeplus.events.TradeEndEvent;
import com.trophonix.tradeplus.events.TradeRequestEvent;
import it.coralrp.laroc.chat.api.managers.ChatManager;
import it.coralrp.laroc.nametags.api.NametagsModule;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class ActionListener implements Listener {

    private final ChatManager chatManager;
    private final NametagsModule nametagsModule;

    public ActionListener(TradePlus plugin) {
        this.chatManager = plugin.getChatModule().getChatManager();
        this.nametagsModule = plugin.getNametagsModule();
    }

    @EventHandler
    public void onTradeAcceptEvent(TradeRequestEvent event) {
        if (event.isCancelled()) return;

        Player receiver = event.getReceiver();
        String receiverName = nametagsModule.getNametagsManager().isAnonymous(receiver) ? "Anonimo" : receiver.getName();

        chatManager.sendChatAction(event.getSender(), Component.text("Ha inviato una richiesta di scambio a " + receiverName));
    }

    @EventHandler
    public void onTradeEndEvent(TradeEndEvent event) {
        Player receiver = event.getReceiver();
        String receiverName = nametagsModule.getNametagsManager().isAnonymous(receiver) ? "Anonimo" : receiver.getName();

        chatManager.sendChatAction(event.getSender(), Component.text("Ha terminato lo scambio con " + receiverName));
    }

}
