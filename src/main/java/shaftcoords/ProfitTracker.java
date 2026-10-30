package shaftcoords;

import at.hannibal2.skyhanni.api.event.HandleEvent;
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule;
import at.hannibal2.skyhanni.data.IslandType;
import at.hannibal2.skyhanni.data.ItemAddManager;
import at.hannibal2.skyhanni.events.IslandChangeEvent;
import at.hannibal2.skyhanni.events.ItemAddEvent;
import at.hannibal2.skyhanni.events.PurseChangeEvent;
import at.hannibal2.skyhanni.events.mining.OreMinedEvent;
import at.hannibal2.skyhanni.utils.ItemPriceSource;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntConsumer;

/**
 * The mining Profit Tracker, SkySoft's: in the mining islands (Gold Mine, Deep Caverns, Dwarven Mines and the Glacite
 * Tunnels, Crystal Hollows, mineshafts) the MINING items you gain, into your inventory or sacks (SkyHanni's ItemAddEvent),
 * are counted and valued by Config.Profit's price source; with the coins you pick up (no menu open), the blocks you mine
 * and the time you're at it (paused after Config.Profit.pauseAfter seconds with none of that). Three periods: this
 * session, today and in total (the last two kept in FILE). The HUD shows one, its most valuable items and the summary lines.
 */
@SkyHanniModule  // SkyHanni's event bus takes handlers only from these (and public)
public final class ProfitTracker {
    // SkySoft's mining preset
    static final List<String> MINING = List.of("COBBLESTONE", "ENCHANTED_COBBLESTONE", "COAL", "ENCHANTED_COAL", "ENCHANTED_COAL_BLOCK",
            "IRON_INGOT", "ENCHANTED_IRON", "ENCHANTED_IRON_BLOCK", "GOLD_INGOT", "ENCHANTED_GOLD", "ENCHANTED_GOLD_BLOCK", "INK_SACK-4",
            "ENCHANTED_LAPIS_LAZULI", "ENCHANTED_LAPIS_LAZULI_BLOCK", "REDSTONE", "ENCHANTED_REDSTONE", "ENCHANTED_REDSTONE_BLOCK", "EMERALD",
            "ENCHANTED_EMERALD", "ENCHANTED_EMERALD_BLOCK", "DIAMOND", "ENCHANTED_DIAMOND", "ENCHANTED_DIAMOND_BLOCK", "OBSIDIAN",
            "ENCHANTED_OBSIDIAN", "ICE", "ENCHANTED_ICE", "ENCHANTED_PACKED_ICE", "HARD_STONE", "ENCHANTED_HARD_STONE", "MITHRIL_ORE",
            "ENCHANTED_MITHRIL", "TITANIUM_ORE", "ENCHANTED_TITANIUM", "GLACITE", "ENCHANTED_GLACITE", "UMBER", "ENCHANTED_UMBER", "TUNGSTEN",
            "ENCHANTED_TUNGSTEN", "ROUGH_RUBY_GEM", "FLAWED_RUBY_GEM", "FINE_RUBY_GEM", "FLAWLESS_RUBY_GEM", "ROUGH_AMETHYST_GEM",
            "FLAWED_AMETHYST_GEM", "FINE_AMETHYST_GEM", "FLAWLESS_AMETHYST_GEM", "ROUGH_JADE_GEM", "FLAWED_JADE_GEM", "FINE_JADE_GEM",
            "FLAWLESS_JADE_GEM", "ROUGH_SAPPHIRE_GEM", "FLAWED_SAPPHIRE_GEM", "FINE_SAPPHIRE_GEM", "FLAWLESS_SAPPHIRE_GEM", "ROUGH_AMBER_GEM",
            "FLAWED_AMBER_GEM", "FINE_AMBER_GEM", "FLAWLESS_AMBER_GEM", "ROUGH_TOPAZ_GEM", "FLAWED_TOPAZ_GEM", "FINE_TOPAZ_GEM",
            "FLAWLESS_TOPAZ_GEM", "ROUGH_JASPER_GEM", "FLAWED_JASPER_GEM", "FINE_JASPER_GEM", "FLAWLESS_JASPER_GEM", "ROUGH_OPAL_GEM",
            "FLAWED_OPAL_GEM", "FINE_OPAL_GEM", "FLAWLESS_OPAL_GEM", "ROUGH_ONYX_GEM", "FLAWED_ONYX_GEM", "FINE_ONYX_GEM", "FLAWLESS_ONYX_GEM",
            "ROUGH_AQUAMARINE_GEM", "FLAWED_AQUAMARINE_GEM", "FINE_AQUAMARINE_GEM", "FLAWLESS_AQUAMARINE_GEM", "ROUGH_CITRINE_GEM",
            "FLAWED_CITRINE_GEM", "FINE_CITRINE_GEM", "FLAWLESS_CITRINE_GEM", "ROUGH_PERIDOT_GEM", "FLAWED_PERIDOT_GEM", "FINE_PERIDOT_GEM",
            "FLAWLESS_PERIDOT_GEM", "REFINED_MINERAL", "GLOSSY_GEMSTONE", "STARFALL", "MITHRIL_GOURMAND", "RARE_DIAMOND", "DYE_EMERALD",
            "DYE_FOSSIL", "DYE_FROSTBITTEN", "DYE_JADE", "DYE_NYANZA", "PET_ITEM_PURE_MITHRIL_GEM", "ATTRIBUTE_SHARD_ATOMIZED_CRYSTALS;1",
            "SLUDGE_JUICE", "WORM_MEMBRANE", "DWARVEN_OS_BLOCK_BRAN", "SCATHA;2", "SCATHA;3", "SCATHA;4", "PREHISTORIC_EGG", "PICKONIMBUS",
            "ASCENSION_ROPE", "WISHING_COMPASS", "OIL_BARREL", "JUNGLE_HEART", "GOBLIN_EGG", "GOBLIN_EGG_GREEN", "GOBLIN_EGG_YELLOW",
            "GOBLIN_EGG_RED", "GOBLIN_EGG_BLUE", "TREASURITE", "ELECTRON_TRANSMITTER", "FTX_3070", "ROBOTRON_REFLECTOR", "CONTROL_SWITCH",
            "SYNTHETIC_HEART", "SUPERLITE_MOTOR", "YOGGIE");
    static final List<String> PERIODS = List.of("SESSION", "TODAY", "TOTAL"), SOURCES = List.of("INSTANT_SELL", "SELL_ORDER", "NPC_SELL"),
            ITEM_SOURCES = List.of("DEFAULT", "INSTANT_SELL", "SELL_ORDER", "NPC_SELL");  // an item's: DEFAULT, the tracker's
    static final long SAVE_MS = 60_000, HIGHLIGHT_MS = 2000;
    private static final List<IslandType> ISLANDS = List.of(IslandType.GOLD_MINES, IslandType.DEEP_CAVERNS, IslandType.DWARVEN_MINES,
            IslandType.CRYSTAL_HOLLOWS, IslandType.MINESHAFT);
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("sammui-shaftin-supplementaries-profit.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    static final ProfitTracker INSTANCE = new ProfitTracker();  // after FILE and GSON, which it loads with

    /** What's kept in FILE. */
    private static final class Saved {
        String date = LocalDate.now().toString();
        ProfitStats today = new ProfitStats(), total = new ProfitStats();
    }

    private ProfitStats session = new ProfitStats();
    private final Saved saved = load();
    private final Map<String, Long> changedAt = new HashMap<>();  // when each item's count last went up
    private long lastActivity, lastTick, savedAt;
    private boolean paused, dirty, inMenu, resetArmed;  // inMenu: last drawn over a menu, clickable
    private List<Line> shown = List.of();               // the lines last drawn, at shownScale
    private float shownScale = 1;
    int hudX, hudY, hudW, hudH;  // where the HUD was last drawn, screen pixels (HudMoveScreen)

    void init() {
        ClientTickEvents.END_CLIENT_TICK.register(mc -> tick());
        ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> save());
        HudElementRegistry.attachElementAfter(VanillaHudElements.BOSS_BAR, Identifier.fromNamespaceAndPath("sammui-shaftin-supplementaries", "profit"), (g, delta) -> {
            if (!(Minecraft.getInstance().gui.screen() instanceof AbstractContainerScreen<?>)) render(g, false, false, -1, -1);  // in one: over it
        });
        ScreenEvents.AFTER_INIT.register((mc, screen, w, h) -> {
            if (!(screen instanceof AbstractContainerScreen<?>)) return;
            ScreenEvents.afterExtract(screen).register((s, g, mouseX, mouseY, delta) -> render(g, false, true, mouseX, mouseY));
            ScreenMouseEvents.allowMouseClick(screen).register((s, event) -> !click(event));
        });
    }

    static String periodName(String period) {
        return switch (period) {
            case "TODAY" -> "Today";
            case "TOTAL" -> "Total";
            default -> "Session";
        };
    }

    static String sourceName(String source) {
        return switch (source) {
            case "INSTANT_SELL" -> "Instant Sell";
            case "NPC_SELL" -> "NPC Sell";
            default -> "Sell Order";
        };
    }

    static boolean active() {
        return Config.INSTANCE.profit.enabled && ISLANDS.stream().anyMatch(IslandType::isInIsland);
    }

    static boolean tracked(String item) {
        Config.Profit cfg = Config.INSTANCE.profit;
        return (MINING.contains(item) || cfg.custom.contains(item)) && !cfg.excluded.contains(item);
    }

    ProfitStats stats(String period) {
        return switch (period) {
            case "TODAY" -> saved.today;
            case "TOTAL" -> saved.total;
            default -> session;
        };
    }

    void reset(String period) {
        switch (period) {
            case "TODAY" -> saved.today = new ProfitStats();
            case "TOTAL" -> saved.total = new ProfitStats();
            default -> session = new ProfitStats();
        }
        save();
    }

    /**
     * The item's price by its own source or the tracker's, or null if it has none. With gemsByFlawless, a rough, flawed
     * or fine gemstone's is its flawless one's (same source) over how many of it make one.
     */
    static Double price(String item) {
        Config.Profit cfg = Config.INSTANCE.profit;
        ItemPriceSource source = switch (cfg.itemSources.getOrDefault(item, cfg.priceSource)) {
            case "INSTANT_SELL" -> ItemPriceSource.BAZAAR_INSTANT_SELL;
            case "NPC_SELL" -> ItemPriceSource.NPC_SELL;
            default -> ItemPriceSource.BAZAAR_INSTANT_BUY;  // what a sell order fills at
        };
        long per = cfg.gemsByFlawless ? ProfitStats.perFlawless(item) : 0;
        if (per == 0) return Sh.price(item, source);
        Double flawless = Sh.price(ProfitStats.flawless(item), source);
        return flawless == null ? null : flawless / per;
    }

    private void each(java.util.function.Consumer<ProfitStats> f) {
        f.accept(session);
        f.accept(saved.today);
        f.accept(saved.total);
        dirty = true;
    }

    @HandleEvent
    public void onItemAdd(ItemAddEvent event) {
        ItemAddManager.Source source = event.getSource();
        if (!active() || source != ItemAddManager.Source.ITEM_ADD && source != ItemAddManager.Source.SACKS || event.getAmount() <= 0) return;
        String item = Sh.internalName(event);
        if (item == null || !tracked(item)) return;
        each(s -> s.add(item, event.getAmount()));
        lastActivity = System.currentTimeMillis();
        changedAt.put(item, lastActivity);
    }

    @HandleEvent
    public void onOreMined(OreMinedEvent event) {
        if (!active()) return;
        int blocks = 1 + event.getExtraBlocks().values().stream().mapToInt(Integer::intValue).sum();
        each(s -> s.blocks += blocks);
        lastActivity = System.currentTimeMillis();
    }

    @HandleEvent
    public void onPurse(PurseChangeEvent event) {
        if (!active() || event.getCoins() <= 0 || Minecraft.getInstance().gui.screen() != null) return;  // a menu open: selling, say
        each(s -> s.coins += event.getCoins());
    }

    @HandleEvent
    public void onIslandChange(IslandChangeEvent event) {
        if (dirty) save();
    }

    private void tick() {
        long now = System.currentTimeMillis(), dt = Math.min(now - lastTick, 1000);  // a stall isn't time mined
        lastTick = now;
        String today = LocalDate.now().toString();
        if (!today.equals(saved.date)) {
            saved.date = today;
            saved.today = new ProfitStats();
            dirty = true;
        }
        int pauseAfter = Config.INSTANCE.profit.pauseAfter;
        boolean counting = active() && lastActivity != 0 && (pauseAfter <= 0 || now - lastActivity < pauseAfter * 1000L);
        paused = !counting;
        if (counting && dt > 0) each(s -> s.uptimeMs += dt);
        if (dirty && now - savedAt > SAVE_MS) save();
    }

    /**
     * The HUD; `preview` draws it anywhere (HudMoveScreen), else only while tracking. In a `menu` (a container screen:
     * your inventory, a chest) it's drawn over the menu, as SkySoft's is, with lines you click to change it: the period,
     * price source and gemstone pricing (left-click next, right-click back), a reset (confirmed), the items (left-click
     * cycles one's price source, right-click stops tracking it) and Manage items; the line under the mouse lit, its hint
     * at the bottom. The mouse is at `mouseX`, `mouseY`.
     */
    void render(GuiGraphicsExtractor g, boolean preview, boolean menu, int mouseX, int mouseY) {
        Config.Profit cfg = Config.INSTANCE.profit;
        Minecraft mc = Minecraft.getInstance();
        if (menu) inMenu = false;
        if (!preview && !active()) return;  // F1 hides the whole HUD, this too
        Font font = mc.font;
        int hovered = menu ? lineAt(mouseX, mouseY) : -1;  // by the last frame's lines
        ProfitStats s = stats(cfg.period);
        record Row(String item, long count, Double price) {
            double value() {
                return price == null ? 0 : count * price;
            }
        }
        List<Row> rows = new ArrayList<>();
        s.items.forEach((item, count) -> {
            if (!cfg.excluded.contains(item)) rows.add(new Row(item, count, price(item)));
        });
        rows.sort(Comparator.comparingDouble(Row::value).reversed().thenComparing(Comparator.comparingLong(Row::count).reversed()));
        double total = rows.stream().mapToDouble(Row::value).sum() + (cfg.lines.contains("COINS") ? s.coins : 0);

        List<Line> lines = new ArrayList<>();
        lines.add(new Line(false, null, "§e§lMining Profit §7(" + periodName(cfg.period) + ")" + (paused && !preview ? " §c(paused)" : ""), "", null, null));
        long now = System.currentTimeMillis();
        for (Row r : rows.subList(0, Math.min(rows.size(), Math.max(0, cfg.maxItems)))) {
            String item = r.item(), q = (cfg.highlight && now - changedAt.getOrDefault(item, 0L) < HIGHLIGHT_MS ? "§a" : "§7") + ProfitStats.count(r.count()) + "x";
            String name = Sh.name(item), own = cfg.itemSources.get(item);
            lines.add(new Line(true, cfg.icons ? Sh.stack(item) : null, cfg.quantityLeft ? q + " §r" + name : name + " " + q,
                    r.price() == null ? "§8?" : "§6" + ProfitStats.coins(r.value()),
                    b -> {
                        if (b == 1) cfg.excluded.add(item);
                        else {
                            String next = cycle(ITEM_SOURCES, own == null ? "DEFAULT" : own, 0);
                            if (next.equals("DEFAULT")) cfg.itemSources.remove(item);
                            else cfg.itemSources.put(item, next);
                        }
                    },
                    "§8Left-click: price §7(" + (own == null ? "Default" : sourceName(own)) + "§7)§8, right-click: stop tracking"));
        }
        if (rows.size() > cfg.maxItems) lines.add(new Line(false, null, "§8+" + (rows.size() - cfg.maxItems) + " more", "", null, null));
        if (rows.isEmpty()) lines.add(new Line(false, null, "§8Nothing mined yet", "", null, null));
        for (String l : Config.Profit.LINES) {
            if (!cfg.lines.contains(l)) continue;
            lines.add(switch (l) {
                case "COINS" -> new Line(false, null, "§7Coins", "§6" + ProfitStats.coins(s.coins), null, null);
                case "TOTAL_PROFIT" -> new Line(false, null, "§7Total Profit", "§6" + ProfitStats.coins(total), null, null);
                case "PROFIT_PER_HOUR" -> new Line(false, null, "§7Profit/h", "§6" + ProfitStats.coins(ProfitStats.perHour(total, s.uptimeMs)), null, null);
                case "BLOCKS" -> new Line(false, null, "§7Blocks Mined", "§f" + ProfitStats.count(s.blocks), null, null);
                default -> new Line(false, null, "§7Uptime", "§f" + ProfitStats.time(s.uptimeMs), null, null);
            });
        }
        if (menu) {
            String period = periodName(cfg.period);
            lines.add(new Line(false, null, "§7Display Mode §a§l[" + period + "]", "", b -> cfg.period = cycle(PERIODS, cfg.period, b), "§8Left-click: next, right-click: back"));
            lines.add(new Line(false, null, "§7Price Source §e§l[" + sourceName(cfg.priceSource) + "]", "",
                    b -> cfg.priceSource = cycle(SOURCES, cfg.priceSource, b), "§8Left-click: next, right-click: back"));
            lines.add(new Line(false, null, "§7Gemstones §b§l[" + (cfg.gemsByFlawless ? "Flawless" : "Own price") + "]", "",
                    b -> cfg.gemsByFlawless = !cfg.gemsByFlawless, "§8Flawless: rough, flawed, fine priced from the flawless one"));
            if (resetArmed) {
                lines.add(new Line(false, null, "§a[Confirm]", "", b -> {
                    reset(cfg.period);
                    resetArmed = false;
                }, "§7Reset " + period + " data."));
                lines.add(new Line(false, null, "§c[Cancel]", "", b -> resetArmed = false, null));
            } else {
                lines.add(new Line(false, null, "§c[Reset " + period + "]", "", b -> resetArmed = true, "§7Reset " + period + " data."));
            }
            lines.add(new Line(false, null, "§7[Manage items]", "", b -> manage(), "§7Manage tracked items."));
            String hint = hovered >= 0 && hovered < shown.size() ? shown.get(hovered).hint() : null;
            lines.add(new Line(false, null, hint == null ? "§8Click a line to change it" : hint, "", null, null));
        }

        int iconW = cfg.icons ? 18 : 0, w = 0, h = 0;
        for (Line l : lines) {
            w = Math.max(w, (l.item() ? iconW : 0) + font.width(l.left()) + (l.right().isEmpty() ? 0 : 10 + font.width(l.right())));
            h += height(l);
        }
        float scale = cfg.scale;
        int x = cfg.x < 0 ? Math.round(g.guiWidth() + cfg.x - w * scale) : cfg.x, y = cfg.y;
        hudX = x;
        hudY = y;
        hudW = Math.round(w * scale);
        hudH = Math.round(h * scale);
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(scale, scale);
        if (cfg.background || preview || menu) g.fill(-3, -3, w + 3, h + 1, preview ? 0x60000000 : menu ? 0xD0101010 : 0x80000000);
        int ly = 0;
        for (int i = 0; i < lines.size(); i++) {
            Line l = lines.get(i);
            int lh = height(l), tx = l.item() ? iconW : 0;
            if (i == hovered && l.click() != null) g.fill(-2, ly, w + 2, ly + lh, 0x30FFFFFF);
            if (l.icon() != null) g.item(l.icon(), 0, ly);
            int ty = ly + (lh - 8) / 2;
            g.text(font, l.left(), tx, ty, 0xFFFFFFFF, true);
            if (!l.right().isEmpty()) g.text(font, l.right(), w - font.width(l.right()), ty, 0xFFFFFFFF, true);
            ly += lh;
        }
        g.pose().popMatrix();
        shown = lines;
        shownScale = scale;
        inMenu = menu;
    }

    /** A HUD line: `item`, an item's row (its icon's room, with icons on); `click`, what a click on it does in a menu (by mouse button, 1 right). */
    private record Line(boolean item, ItemStack icon, String left, String right, IntConsumer click, String hint) {}

    private int height(Line l) {
        return l.item() && Config.INSTANCE.profit.icons ? 18 : 11;
    }

    /** The shown line at screen `x`, `y`, or -1. */
    private int lineAt(double x, double y) {
        if (x < hudX || x > hudX + hudW || y < hudY || y >= hudY + hudH) return -1;
        double ly = (y - hudY) / shownScale;
        int top = 0;
        for (int i = 0; i < shown.size(); i++) {
            int lh = height(shown.get(i));
            if (ly >= top && ly < top + lh) return i;
            top += lh;
        }
        return -1;
    }

    /** A click in a menu: true if it was on one of the HUD's lines, done (the menu doesn't get it). */
    private boolean click(MouseButtonEvent event) {
        if (!inMenu) return false;
        int i = lineAt(event.x(), event.y());
        IntConsumer click = i < 0 ? null : shown.get(i).click();
        if (click == null) return false;
        click.accept(event.button());
        Config.INSTANCE.save();
        return true;
    }

    /** The next of `values` after `current` (button 1, right-click: the one before). */
    private static String cycle(List<String> values, String current, int button) {
        int i = Math.max(0, values.indexOf(current)), n = values.size();
        return values.get(((button == 1 ? i - 1 : i + 1) % n + n) % n);
    }

    /** Manage items: leaves the menu (closing it, as Esc does) for ProfitItemsScreen. */
    private void manage() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui.screen() != null) mc.gui.screen().onClose();
        mc.gui.setScreen(new ProfitItemsScreen(null));
    }

    private Saved load() {
        try {
            if (Files.exists(FILE)) {
                Saved s = GSON.fromJson(Files.readString(FILE), Saved.class);
                if (s != null && s.today != null && s.total != null) return s;
            }
        } catch (IOException | RuntimeException e) {
            ShaftCoords.LOG.warn("Can't read {}, starting the profit tracker afresh", FILE, e);
        }
        return new Saved();
    }

    void save() {
        savedAt = System.currentTimeMillis();
        dirty = false;
        try {
            Files.writeString(FILE, GSON.toJson(saved));
        } catch (IOException e) {
            ShaftCoords.LOG.warn("Can't save {}", FILE, e);
        }
    }
}
