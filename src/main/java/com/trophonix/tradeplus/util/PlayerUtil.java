package com.trophonix.tradeplus.util;

import org.bukkit.entity.Player;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerUtil {

  private static final Map<UUID, String> ipAddresses = new HashMap<>();

  public static void registerIP(Player player) {
    InetSocketAddress address = player.getAddress();
    if (address != null) {
      String ip = player.getAddress().getHostString();
      if (ip != null) ipAddresses.put(player.getUniqueId(), ip);
    }
  }

  public static void removeIP(Player player) {
    ipAddresses.remove(player.getUniqueId());
  }

  public static boolean sameIP(Player player1, Player player2) {
    String ip1 = ipAddresses.get(player1.getUniqueId());
    String ip2 = ipAddresses.get(player2.getUniqueId());
    if (ip1 == null || ip2 == null) return false;
    return ip1.equals(ip2);
  }
}
