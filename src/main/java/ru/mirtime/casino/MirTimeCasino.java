package ru.mirtime.casino;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

import static ru.mirtime.casino.CasinoData.*;

public class MirTimeCasino extends JavaPlugin implements CommandExecutor, Listener {

    private final CasinoData data = new CasinoData();
    private final Map<UUID, Map<Integer, Double>> treasureFields = new HashMap<>();
    private final Map<UUID, Double> treasureWin = new HashMap<>();
    private final Random random = new Random();

    @Override
    public void onEnable() {
        data.init(this);
        if (getCommand("casino") != null) getCommand("casino").setExecutor(this);
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("MirTimeCasino запущен!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player p) openMain(p);
        return true;
    }

    public void openMain(Player p) {
        Inventory inv = Bukkit.createInventory(null, 45, color("&#0055ff&l⚡ &#00E5FF&lMIR-TIME &8| &fКазино &8[&#00FF66$&8]"));
        frame(inv);
        inv.setItem(12, item(Material.TNT, "&#0055ff&l[$] &#00E5FF&lКладоискатель", List.of(" &#00FF66➥ Нажмите&f, чтобы играть", "", " &7Копай песок и собирай иксы,", " &7но остерегайся динамита!")));
        inv.setItem(14, item(Material.LEATHER_HORSE_ARMOR, "&#0055ff&l[$] &#00E5FF&lКонные скачки", List.of(" &#00FF66➥ Нажмите&f, чтобы играть", "", " &7Выбери своего скакуна и забери", " &7приз за победу &#00FF66&lx2.0&7!")));
        inv.setItem(20, item(Material.DIAMOND, "&#0055ff&l[$] &#00E5FF&lАлмазная рулетка", List.of(" &#00FF66➥ Нажмите&f, чтобы играть", "", " &7Классическая лента с анимацией", " &7прокрутки до &#00FF66&lx5.0&7!")));
        inv.setItem(21, item(Material.PAPER, "&#0055ff&l[$] &#00E5FF&lПокер &8(&75 карт&8)", List.of(" &#00FF66➥ Нажмите&f, чтобы играть", "", " &7Раздача на 5 карт с комбинациями", " &7от Пары до Роял-Флеша &#00FF66&lx50.0&7!")));
        inv.setItem(23, item(Material.QUARTZ_BLOCK, "&#0055ff&l[$] &#00E5FF&lКости &8(&72 кубика&8)", List.of(" &#00FF66➥ Нажмите&f, чтобы играть", "", " &7Угадай сумму двух кубиков:", " &7меньше 7, больше 7 или ровно 7 &#00FF66&lx4.5&7!")));
        inv.setItem(24, item(Material.SUNFLOWER, "&#0055ff&l[$] &#00E5FF&lОрёл и Решка", List.of(" &#00FF66➥ Нажмите&f, чтобы играть", "", " &7Классический бросок монеты 50/50", " &7с множителем &#00FF66&lx1.95&7!")));

        inv.setItem(31, item(Material.ENDER_CHEST, "&#0055ff[＄] &#00E5FF&lКошелёк и Додеп", List.of(
                "", " &#0055ff● &fТокены: &#00E5FF" + fmt(data.getBalance(p, Currency.TOKENS)),
                " &#0055ff● &fМонеты: &#00FF66" + fmt(data.getBalance(p, Currency.VAULT)), "",
                " &cЗалудил всё? Не беда!", " &fТокены для &#00FF66&lдодепа &fна сайте:",
                " &#00E5FF&nhttps://mir-time.ru", "", " &#00FF66➥ Нажмите&f, чтобы получить ссылку в чат"
        )));
        p.openInventory(inv);
    }

    public void openBet(Player p) {
        PlayerSession s = data.getSession(p);
        Inventory inv = Bukkit.createInventory(null, 45, color("&#0055ff&l＄ Казино: &#00E5FF&lНастройка ставки"));
        frame(inv);
        long bal = data.getBalance(p, s.currency);
        long step = (s.currency == Currency.VAULT) ? 100000L : 10L;
        long shift = (s.currency == Currency.VAULT) ? 1000000L : 100L;

        inv.setItem(13, item(Material.ENDER_CHEST, "&#0055ff[$] &#00E5FFСтавка [&f" + fmt(s.bet) + " &7/ &#00FF66" + fmt(bal) + "&#00E5FF]", List.of(
                " &#00FF66[ЛКМ] &f+&#00FF66" + fmt(step) + " &8| &c[ПКМ] &f-&#00E5FF" + fmt(step),
                " &#00FF66[ШИФТ+ЛКМ] &f+&#00FF66" + fmt(shift) + " &8| &c[ШИФТ+ПКМ] &f-&#00E5FF" + fmt(shift),
                "", " &7Додеп токенов: &#00E5FF&ndon.mir-time.ru"
        )));
        inv.setItem(21, item(Material.BOOK, "&#0055ff[i] &#00E5FF&lКак играть?", List.of(" &71. Выберите валюту справа.", " &72. Настройте ставку по Сундуку.", " &73. Нажмите Звезду Незера снизу!", " &7Додеп: &#00E5FF&ndon.mir-time.ru")));
        inv.setItem(23, item(s.currency.icon, "&#0055ff[$] &#00E5FFВалюта &f[" + s.currency.displayName + "&f]", List.of(" &#00FF66➥ Нажмите&f, чтобы сменить валюту")));
        inv.setItem(31, item(Material.NETHER_STAR, "&#00FF66&l[Начать игру]", List.of(s.bet > 0 && bal >= s.bet ? " &#00FF66➥ Нажмите&f, чтобы начать!" : " &c➥ Недостаточно средств! Додеп: &#00E5FF&ndon.mir-time.ru")));
        inv.setItem(40, item(Material.ARROW, "&c[←] &fВ главное меню", null));
        p.openInventory(inv);
    }

    public void openCurrency(Player p) {
        Inventory inv = Bukkit.createInventory(null, 45, color("&#0055ff&l＄ Казино: &#00E5FF&lВыбор валюты"));
        frame(inv);
        inv.setItem(20, item(Material.EMERALD, "&#0055ff[$] &#00E5FF&lТокены", List.of(" &#00FF66➥ Нажмите&f, чтобы выбрать", " &7Додеп: &#00E5FF&ndon.mir-time.ru")));
        inv.setItem(21, item(Material.SUNFLOWER, "&#0055ff[$] &#00FF66&lМонеты", List.of(" &#00FF66➥ Нажмите&f, чтобы выбрать")));
        inv.setItem(23, item(Material.DIAMOND, "&#0055ff[$] &#00a6ff&lАлмазы", List.of(" &#00FF66➥ Нажмите&f, чтобы выбрать")));
        inv.setItem(24, item(Material.NETHERITE_INGOT, "&#0055ff[$] &#0055ff&lНезерит", List.of(" &#00FF66➥ Нажмите&f, чтобы выбрать")));
        inv.setItem(40, item(Material.ARROW, "&c[←] &fНазад", null));
        p.openInventory(inv);
    }
    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        String title = e.getView().getTitle();
        if (!title.contains("MIR-TIME") && !title.contains("Казино:") && !title.contains("Игра:")) return;
        e.setCancelled(true);
        if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR) return;

        PlayerSession s = data.getSession(p);
        int slot = e.getRawSlot();

        if (title.contains("MIR-TIME")) {
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            switch (slot) {
                case 12 -> { s.selectedGame = "treasure"; openBet(p); }
                case 14 -> { s.selectedGame = "horses"; openBet(p); }
                case 20 -> { s.selectedGame = "roulette"; openBet(p); }
                case 21 -> { s.selectedGame = "poker"; openBet(p); }
                case 23 -> { s.selectedGame = "dice"; openBet(p); }
                case 24 -> { s.selectedGame = "coinflip"; openBet(p); }
                case 31 -> {
                    p.closeInventory();
                    p.sendMessage(color("&#0055ff&l⚡ &#00E5FF&lMIR-TIME &8» &fКупить токены для додепа: &#00FF66&nhttps://mir-time.ru"));
                }
            }
            return;
        }

        if (title.contains("Выбор валюты")) {
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            switch (slot) {
                case 20 -> { s.currency = Currency.TOKENS; s.bet = 10; openBet(p); }
                case 21 -> { s.currency = Currency.VAULT; s.bet = 100000; openBet(p); }
                case 23 -> { s.currency = Currency.DIAMOND; s.bet = 1; openBet(p); }
                case 24 -> { s.currency = Currency.NETHERITE; s.bet = 1; openBet(p); }
                case 40 -> openBet(p);
            }
            return;
        }

        if (title.contains("Настройка ставки")) {
            long step = (s.currency == Currency.VAULT) ? 100000L : 10L;
            long shift = (s.currency == Currency.VAULT) ? 1000000L : 100L;
            if (slot == 13) {
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.2f);
                ClickType ct = e.getClick();
                if (ct == ClickType.LEFT) s.bet += step;
                else if (ct == ClickType.RIGHT) s.bet = Math.max(1, s.bet - step);
                else if (ct == ClickType.SHIFT_LEFT) s.bet += shift;
                else if (ct == ClickType.SHIFT_RIGHT) s.bet = Math.max(1, s.bet - shift);
                openBet(p);
            } else if (slot == 23) openCurrency(p);
            else if (slot == 40) openMain(p);
            else if (slot == 31) {
                if (!data.takeBalance(p, s.currency, s.bet)) {
                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    p.sendMessage(color("&#0055ff&lMIR-TIME &8» &cНедостаточно средств! &fДодеп токенов: &#00E5FF&ndon.mir-time.ru"));
                    return;
                }
                startGame(p, s);
            }
            return;
        }

        if (title.contains("Игра:")) {
            if (slot == 40 && !s.inProgress) { openBet(p); return; }
            handleGameClick(p, s, slot, e.getInventory());
        }
    }

    private void startGame(Player p, PlayerSession s) {
        s.inProgress = true;
        switch (s.selectedGame) {
            case "roulette" -> runRoulette(p, s);
            case "poker" -> runPoker(p, s);
            case "coinflip" -> runCoinflip(p, s);
            case "dice" -> openDice(p);
            case "horses" -> openHorses(p);
            case "treasure" -> openTreasure(p);
        }
    }

    private void runRoulette(Player p, PlayerSession s) {
        Inventory inv = Bukkit.createInventory(null, 45, color("&#0055ff&l＄ Игра: &#00E5FF&lРулетка"));
        frame(inv);
        inv.setItem(13, item(Material.HOPPER, "&#00FF66&l▼ ВЫИГРЫШ ▼", null));
        inv.setItem(31, item(Material.HOPPER, "&#00FF66&l▲ ВЫИГРЫШ ▲", null));
        p.openInventory(inv);

        double[] mults = {0.0, 0.5, 1.2, 1.5, 2.0, 0.0, 1.0, 5.0};
        Material[] mats = {Material.COAL, Material.BRICK, Material.IRON_INGOT, Material.GOLD_INGOT, Material.EMERALD, Material.COAL, Material.COPPER_INGOT, Material.DIAMOND};
        new BukkitRunnable() {
            int ticks = 0, off = random.nextInt(mults.length);
            @Override
            public void run() {
                if (!p.isOnline()) { cancel(); return; }
                for (int i = 0; i < 7; i++) {
                    int idx = (off + i) % mults.length;
                    inv.setItem(19 + i, item(mats[idx], "&#0055ff[$] &#00FF66x" + mults[idx], null));
                }
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 1.2f);
                off++;
                if (++ticks >= 28) {
                    cancel();
                    finish(p, s, mults[(off + 2) % mults.length]);
                    inv.setItem(40, item(Material.ARROW, "&a[Назад к ставке]", null));
                }
            }
        }.runTaskTimer(this, 0L, 2L);
    }

    private void runPoker(Player p, PlayerSession s) {
        Inventory inv = Bukkit.createInventory(null, 45, color("&#0055ff&l＄ Игра: &#00E5FF&lПокер"));
        frame(inv);
        p.openInventory(inv);
        String[] ranks = {"6", "7", "8", "9", "10", "J", "Q", "K", "A"};
        String[] suits = {"♠", "♥", "♦", "♣"};

        new BukkitRunnable() {
            int idx = 0;
            final int[] r = new int[5], st = new int[5];
            @Override
            public void run() {
                if (!p.isOnline()) { cancel(); return; }
                if (idx < 5) {
                    r[idx] = random.nextInt(ranks.length);
                    st[idx] = random.nextInt(suits.length);
                    inv.setItem(20 + idx, item(Material.PAPER, "&#0055ff[★] &fКарта: &#00FF66&l" + ranks[r[idx]] + suits[st[idx]], null));
                    p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1.1f);
                    idx++;
                } else {
                    cancel();
                    finish(p, s, evalPoker(r, st, p));
                    inv.setItem(40, item(Material.ARROW, "&a[Назад к ставке]", null));
                }
            }
        }.runTaskTimer(this, 5L, 8L);
    }
    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        String title = e.getView().getTitle();
        if (!title.contains("MIR-TIME") && !title.contains("Казино:") && !title.contains("Игра:")) return;
        e.setCancelled(true);
        if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR) return;

        PlayerSession s = data.getSession(p);
        int slot = e.getRawSlot();

        if (title.contains("MIR-TIME")) {
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            switch (slot) {
                case 12 -> { s.selectedGame = "treasure"; openBet(p); }
                case 14 -> { s.selectedGame = "horses"; openBet(p); }
                case 20 -> { s.selectedGame = "roulette"; openBet(p); }
                case 21 -> { s.selectedGame = "poker"; openBet(p); }
                case 23 -> { s.selectedGame = "dice"; openBet(p); }
                case 24 -> { s.selectedGame = "coinflip"; openBet(p); }
                case 31 -> {
                    p.closeInventory();
                    p.sendMessage(color("&#0055ff&l⚡ &#00E5FF&lMIR-TIME &8» &fКупить токены для додепа: &#00FF66&nhttps://mir-time.ru"));
                }
            }
            return;
        }

        if (title.contains("Выбор валюты")) {
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            switch (slot) {
                case 20 -> { s.currency = Currency.TOKENS; s.bet = 10; openBet(p); }
                case 21 -> { s.currency = Currency.VAULT; s.bet = 100000; openBet(p); }
                case 23 -> { s.currency = Currency.DIAMOND; s.bet = 1; openBet(p); }
                case 24 -> { s.currency = Currency.NETHERITE; s.bet = 1; openBet(p); }
                case 40 -> openBet(p);
            }
            return;
        }

        if (title.contains("Настройка ставки")) {
            long step = (s.currency == Currency.VAULT) ? 100000L : 10L;
            long shift = (s.currency == Currency.VAULT) ? 1000000L : 100L;
            if (slot == 13) {
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.2f);
                ClickType ct = e.getClick();
                if (ct == ClickType.LEFT) s.bet += step;
                else if (ct == ClickType.RIGHT) s.bet = Math.max(1, s.bet - step);
                else if (ct == ClickType.SHIFT_LEFT) s.bet += shift;
                else if (ct == ClickType.SHIFT_RIGHT) s.bet = Math.max(1, s.bet - shift);
                openBet(p);
            } else if (slot == 23) openCurrency(p);
            else if (slot == 40) openMain(p);
            else if (slot == 31) {
                if (!data.takeBalance(p, s.currency, s.bet)) {
                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    p.sendMessage(color("&#0055ff&lMIR-TIME &8» &cНедостаточно средств! &fДодеп токенов: &#00E5FF&ndon.mir-time.ru"));
                    return;
                }
                startGame(p, s);
            }
            return;
        }

        if (title.contains("Игра:")) {
            if (slot == 40 && !s.inProgress) { openBet(p); return; }
            handleGameClick(p, s, slot, e.getInventory());
        }
    }

    private void startGame(Player p, PlayerSession s) {
        s.inProgress = true;
        switch (s.selectedGame) {
            case "roulette" -> runRoulette(p, s);
            case "poker" -> runPoker(p, s);
            case "coinflip" -> runCoinflip(p, s);
            case "dice" -> openDice(p);
            case "horses" -> openHorses(p);
            case "treasure" -> openTreasure(p);
        }
    }

    private void runRoulette(Player p, PlayerSession s) {
        Inventory inv = Bukkit.createInventory(null, 45, color("&#0055ff&l＄ Игра: &#00E5FF&lРулетка"));
        frame(inv);
        inv.setItem(13, item(Material.HOPPER, "&#00FF66&l▼ ВЫИГРЫШ ▼", null));
        inv.setItem(31, item(Material.HOPPER, "&#00FF66&l▲ ВЫИГРЫШ ▲", null));
        p.openInventory(inv);

        double[] mults = {0.0, 0.5, 1.2, 1.5, 2.0, 0.0, 1.0, 5.0};
        Material[] mats = {Material.COAL, Material.BRICK, Material.IRON_INGOT, Material.GOLD_INGOT, Material.EMERALD, Material.COAL, Material.COPPER_INGOT, Material.DIAMOND};
        new BukkitRunnable() {
            int ticks = 0, off = random.nextInt(mults.length);
            @Override
            public void run() {
                if (!p.isOnline()) { cancel(); return; }
                for (int i = 0; i < 7; i++) {
                    int idx = (off + i) % mults.length;
                    inv.setItem(19 + i, item(mats[idx], "&#0055ff[$] &#00FF66x" + mults[idx], null));
                }
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 1.2f);
                off++;
                if (++ticks >= 28) {
                    cancel();
                    finish(p, s, mults[(off + 2) % mults.length]);
                    inv.setItem(40, item(Material.ARROW, "&a[Назад к ставке]", null));
                }
            }
        }.runTaskTimer(this, 0L, 2L);
    }

    private void runPoker(Player p, PlayerSession s) {
        Inventory inv = Bukkit.createInventory(null, 45, color("&#0055ff&l＄ Игра: &#00E5FF&lПокер"));
        frame(inv);
        p.openInventory(inv);
        String[] ranks = {"6", "7", "8", "9", "10", "J", "Q", "K", "A"};
        String[] suits = {"♠", "♥", "♦", "♣"};

        new BukkitRunnable() {
            int idx = 0;
            final int[] r = new int[5], st = new int[5];
            @Override
            public void run() {
                if (!p.isOnline()) { cancel(); return; }
                if (idx < 5) {
                    r[idx] = random.nextInt(ranks.length);
                    st[idx] = random.nextInt(suits.length);
                    inv.setItem(20 + idx, item(Material.PAPER, "&#0055ff[★] &fКарта: &#00FF66&l" + ranks[r[idx]] + suits[st[idx]], null));
                    p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1.1f);
                    idx++;
                } else {
                    cancel();
                    finish(p, s, evalPoker(r, st, p));
                    inv.setItem(40, item(Material.ARROW, "&a[Назад к ставке]", null));
                }
            }
        }.runTaskTimer(this, 5L, 8L);
    }
