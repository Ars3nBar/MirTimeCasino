package ru.mirtime.casino;

import net.milkbowl.vault.economy.Economy;
import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.text.NumberFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CasinoData {

    public Economy economy;
    public PlayerPointsAPI pointsAPI;
    public final Map<UUID, PlayerSession> sessions = new HashMap<>();
    private static final NumberFormat FORMAT = NumberFormat.getInstance(Locale.US);

    public enum Currency {
        TOKENS("&#00E5FFТокены", Material.EMERALD),
        VAULT("&#00FF66Монеты", Material.SUNFLOWER),
        DIAMOND("&#00a6ffАлмазы", Material.DIAMOND),
        NETHERITE("&#0055ffНезерит", Material.NETHERITE_INGOT);

        public final String displayName;
        public final Material icon;

        Currency(String displayName, Material icon) {
            this.displayName = displayName;
            this.icon = icon;
        }
    }

    public static class PlayerSession {
        public String selectedGame = "roulette";
        public Currency currency = Currency.TOKENS;
        public long bet = 10;
        public boolean inProgress = false;
    }

    public void init(MirTimeCasino plugin) {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") != null) {
            RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
            if (rsp != null) economy = rsp.getProvider();
        }
        if (Bukkit.getPluginManager().isPluginEnabled("PlayerPoints")) {
            pointsAPI = PlayerPoints.getInstance().getAPI();
        }
    }

    public PlayerSession getSession(Player p) {
        return sessions.computeIfAbsent(p.getUniqueId(), k -> new PlayerSession());
    }

    public long getBalance(Player p, Currency c) {
        return switch (c) {
            case TOKENS -> pointsAPI != null ? pointsAPI.look(p.getUniqueId()) : 0;
            case VAULT -> economy != null ? (long) economy.getBalance(p) : 0;
            case DIAMOND -> countItems(p, Material.DIAMOND);
            case NETHERITE -> countItems(p, Material.NETHERITE_INGOT);
        };
    }

    public boolean takeBalance(Player p, Currency c, long amount) {
        if (amount <= 0 || getBalance(p, c) < amount) return false;
        return switch (c) {
            case TOKENS -> pointsAPI != null && pointsAPI.take(p.getUniqueId(), (int) amount);
            case VAULT -> economy != null && economy.withdrawPlayer(p, amount).transactionSuccess();
            case DIAMOND -> { removeItems(p, Material.DIAMOND, (int) amount); yield true; }
            case NETHERITE -> { removeItems(p, Material.NETHERITE_INGOT, (int) amount); yield true; }
        };
    }

    public void giveBalance(Player p, Currency c, long amount) {
        if (amount <= 0) return;
        switch (c) {
            case TOKENS -> { if (pointsAPI != null) pointsAPI.give(p.getUniqueId(), (int) amount); }
            case VAULT -> { if (economy != null) economy.depositPlayer(p, amount); }
            case DIAMOND -> giveItems(p, Material.DIAMOND, (int) amount);
            case NETHERITE -> giveItems(p, Material.NETHERITE_INGOT, (int) amount);
        }
    }

    private int countItems(Player p, Material mat) {
        int count = 0;
        for (ItemStack item : p.getInventory().getContents()) {
            if (item != null && item.getType() == mat) count += item.getAmount();
        }
        return count;
    }

    private void removeItems(Player p, Material mat, int amount) {
        int rem = amount;
        for (ItemStack item : p.getInventory().getContents()) {
            if (item != null && item.getType() == mat) {
                int take = Math.min(rem, item.getAmount());
                item.setAmount(item.getAmount() - take);
                rem -= take;
                if (rem <= 0) break;
            }
        }
    }

    private void giveItems(Player p, Material mat, int amount) {
        HashMap<Integer, ItemStack> left = p.getInventory().addItem(new ItemStack(mat, amount));
        for (ItemStack drop : left.values()) p.getWorld().dropItemNaturally(p.getLocation(), drop);
    }

    public static String color(String text) {
        if (text == null) return "";
        Pattern pattern = Pattern.compile("&#([A-Fa-f0-9]{6})");
        Matcher matcher = pattern.matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder rep = new StringBuilder("§x");
            for (char c : hex.toCharArray()) rep.append('§').append(c);
            matcher.appendReplacement(buffer, rep.toString());
        }
        matcher.appendTail(buffer);
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

    public static String fmt(long num) {
        return FORMAT.format(num);
    }

    public static ItemStack item(Material mat, String name, List<String> lore) {
        ItemStack is = new ItemStack(mat);
        ItemMeta meta = is.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(name));
            if (lore != null && !lore.isEmpty()) {
                List<String> colored = new ArrayList<>();
                for (String l : lore) colored.add(color(l));
                meta.setLore(colored);
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            is.setItemMeta(meta);
        }
        return is;
    }

    public static void frame(Inventory inv) {
        ItemStack b = item(Material.BLUE_STAINED_GLASS_PANE, "&#0055ff▪", null);
        ItemStack c = item(Material.LIGHT_BLUE_STAINED_GLASS_PANE, "&#00E5FF▫", null);
        for (int s : new int[]{0, 1, 7, 8, 9, 17, 18, 26, 27, 35, 36, 37, 43, 44}) inv.setItem(s, b);
        for (int s : new int[]{2, 3, 4, 5, 6, 10, 11, 13, 15, 16, 19, 22, 25, 28, 29, 30, 32, 33, 34, 38, 39, 40, 41, 42}) inv.setItem(s, c);
    }
}
