package com.trophonix.tradeplus.commands;

import com.trophonix.tradeplus.TradePlus;
import com.trophonix.tradeplus.events.TradeAcceptEvent;
import com.trophonix.tradeplus.events.TradeRequestEvent;
import com.trophonix.tradeplus.trade.Trade;
import com.trophonix.tradeplus.trade.TradeRequest;
import com.trophonix.tradeplus.util.MsgUtils;
import com.trophonix.tradeplus.util.PDCUtils;
import com.trophonix.tradeplus.util.PlayerUtil;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.*;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

public class TradeCommand implements TabExecutor {

    private static final DecimalFormat format = new DecimalFormat("0.##");

    private final ConcurrentLinkedQueue<TradeRequest> requests = new ConcurrentLinkedQueue<>();

    private final TradePlus pl;

    private boolean pdc;

    public TradeCommand(TradePlus pl) {
//    super(
//        new ArrayList<String>() {
//          {
//            add("trade");
//            if (pl.getTradeConfig().getAliases() != null) {
//              addAll(pl.getTradeConfig().getAliases());
//            }
//          }
//        });
        this.pl = pl;
        try {
            Class.forName("org.bukkit.persistence.PersistentDataContainer");
            PDCUtils.initialize(pl);
            pdc = true;
        } catch (ClassNotFoundException ignored) {
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            MsgUtils.send(sender, "&cThis command can only be executed by players!");
            return true;
        }

        if (pdc && args.length == 1 && args[0].equalsIgnoreCase("toggle")) {
            boolean allowed = PDCUtils.toggleTrading(player);
            (allowed ? pl.getTradeConfig().getTradingEnabled() : pl.getTradeConfig().getTradingDisabled()).send(sender);
            return true;
        }

        boolean permissionRequired = pl.getConfig().getBoolean("permissions.required", false);

        if (args.length == 1) {
            final Player receiver = Bukkit.getPlayer(args[0]);

            String playerName = pl.getNametagsModule().getNametagsManager().isAnonymous(player) ? "Anonimo" : player.getName();

            if (receiver == null) {
                if (args[0].equalsIgnoreCase("rifiuta")) {
                    requests.forEach(
                            req -> {
                                if (req.receiver == player) {
                                    requests.remove(req);
                                    if (req.sender.isOnline()) {
                                        pl.getTradeConfig()
                                                .getTheyDenied()
                                                .send(req.sender, "%PLAYER%", playerName);
                                    }
                                }
                            });
                    pl.getTradeConfig().getYouDenied().send(player);
                    return true;
                }
                pl.getTradeConfig().getErrorsPlayerNotFound().send(player);
                return true;
            }

            String receiverName = pl.getNametagsModule().getNametagsManager().isAnonymous(receiver) ? "Anonimo" : receiver.getName();

            if (player == receiver) {
                pl.getTradeConfig().getErrorsSelfTrade().send(player);
                return true;
            }

            if (!pl.getTradeConfig().isAllowSameIpTrade()) {
                if (PlayerUtil.sameIP(player, receiver)) {
                    pl.getTradeConfig().getErrorsSameIp().send(player);
                    return true;
                }
            }

            if (!pl.getTradeConfig().isAllowTradeInCreative()) {
                if (player.getGameMode().equals(GameMode.CREATIVE) || player.getGameMode().equals(GameMode.SPECTATOR)) {
                    pl.getTradeConfig().getErrorsCreative().send(player);
                    return true;
                } else if (receiver.getGameMode().equals(GameMode.CREATIVE) || receiver.getGameMode().equals(GameMode.SPECTATOR)) {
                    pl.getTradeConfig().getErrorsCreativeThem().send(player, "%PLAYER%", receiverName);
                    return true;
                }
            }

            if (pl.getTradeConfig().getBlockedWorlds().contains(player.getWorld().getName())) {
                pl.getTradeConfig().getErrorsBlockedWorld().send(player, "%WORLD%", player.getWorld().getName());
                return true;
            }

            if (player.getWorld().equals(receiver.getWorld())) {
                double amount = pl.getTradeConfig().getSameWorldRange();
                if (amount != 0.0
                    && player.getLocation().distanceSquared(receiver.getLocation()) > Math.pow(amount, 2)) {
                    pl.getTradeConfig()
                            .getErrorsSameWorldRange()
                            .send(player, "%PLAYER%", receiverName, "%AMOUNT%", format.format(amount));
                    return true;
                }
            } else {
                if (pl.getTradeConfig().isAllowCrossWorld()) {
                    double amount = Math.pow(pl.getTradeConfig().getCrossWorldRange(), 2);
                    Location test = receiver.getLocation().clone();
                    test.setWorld(player.getWorld());
                    if (amount != 0.0 && player.getLocation().distanceSquared(test) > amount) {
                        pl.getTradeConfig()
                                .getErrorsCrossWorldRange()
                                .send(player, "%PLAYER%", receiverName, "%AMOUNT%", format.format(amount));
                        return true;
                    }
                } else {
                    pl.getTradeConfig().getErrorsNoCrossWorld().send(player, "%PLAYER%", receiverName);
                    return true;
                }
            }

            for (TradeRequest req : requests) {
                if (req.sender == player) {
                    pl.getTradeConfig().getErrorsWaitForExpire().send(player, "%PLAYER%", receiverName);
                    return true;
                }
            }

            boolean accept = false;
            for (TradeRequest req : requests) {
                if (req.contains(player) && req.contains(receiver)) accept = true;
            }
            if (accept) {
                TradeAcceptEvent tradeAcceptEvent = new TradeAcceptEvent(receiver, player);
                Bukkit.getPluginManager().callEvent(tradeAcceptEvent);
                if (tradeAcceptEvent.isCancelled()) return true;
                pl.getTradeConfig().getAcceptSender().send(receiver, "%PLAYER%", playerName);
                pl.getTradeConfig().getAcceptReceiver().send(player, "%PLAYER%", receiverName);
                new Trade(receiver, player);
                requests.removeIf(req -> req.contains(player) && req.contains(receiver));
            } else {
                if (pdc && !PDCUtils.allowTrading(receiver)) {
                    pl.getTradeConfig().getErrorsTradingDisabled().send(sender, "%PLAYER%", receiverName);
                    return true;
                }

                String sendPermission = pl.getTradeConfig().getSendPermission();
                if (permissionRequired) {
                    if (!sender.hasPermission(sendPermission)) {
                        pl.getTradeConfig().getErrorsNoPermsAccept().send(player);
                        return true;
                    }
                }

                String acceptPermission = pl.getTradeConfig().getAcceptPermission();
                if (permissionRequired && !receiver.hasPermission(acceptPermission)) {
                    pl.getTradeConfig()
                            .getErrorsNoPermsReceive()
                            .send(player, "%PLAYER%", receiverName);
                    return true;
                }

                TradeRequestEvent event = new TradeRequestEvent(player, receiver);
                Bukkit.getPluginManager().callEvent(event);
                if (event.isCancelled()) return true;
                final TradeRequest request = new TradeRequest(player, receiver);
                requests.add(request);
                pl.getTradeConfig().getRequestSent().send(player, "%PLAYER%", receiverName);
                pl.getTradeConfig()
                        .getRequestReceived()
                        .setOnClick("/trade " + player.getName())
                        .send(receiver, "%PLAYER%", playerName);
                Bukkit.getScheduler()
                        .runTaskLater(
                                pl,
                                () -> {
                                    boolean was = requests.remove(request);
                                    if (player.isOnline() && was) {
                                        pl.getTradeConfig().getExpired().send(player, "%PLAYER%", receiverName);
                                    }
                                },
                                20 * (long) pl.getTradeConfig().getRequestCooldownSeconds());
            }
            return true;
        }
        pl.getTradeConfig().getErrorsInvalidUsage().send(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> args0 = new ArrayList<>();
        args0.add("rifiuta");
        args0.addAll(
                Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .toList());
        if (args.length == 0) {
            return args0;
        } else if (args.length == 1) {
            return args0.stream()
                    .filter(
                            name ->
                                    !name.equalsIgnoreCase(args[0])
                                    && name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
