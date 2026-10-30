package shaftcoords;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/** SAMMUI SHAFTIN SUPPLEMENTARIES' settings, in config/sammui-shaftin-supplementaries.json; /sss opens ConfigScreen to change them. */
final class Config {
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("sammui-shaftin-supplementaries.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    static final Config INSTANCE = load();

    boolean autoStart = true;                                  // load a mineshaft's route when SkyHanni detects it
    Set<String> routesOff = new TreeSet<>();                   // route files that don't (by hand they still do)
    boolean corpseEsp = true;                                  // a box round each corpse, through walls
    boolean corpseTitle = true;                                // "PERI 1 2L 1U" on screen when corpses are found
    boolean muteCorpses = true;                                // the Organ Donor's corpse sound (a harp note), in mineshafts
    int espLife = 120;                                         // seconds a corpse's box shows once found; 0: till you leave
    Map<Corpse, String> colors = new EnumMap<>(Corpse.class);  // ESP colour per corpse, "FFAA00"; none: Corpse.color
    boolean buggedPity = true;                                 // a title and jingle when mining doesn't move the tab's pity
    Profit profit = new Profit();                              // the mining Profit Tracker

    int color(Corpse c) {
        return Corpse.rgb(colors.get(c), c.color);
    }

    /** The mining Profit Tracker's settings (ProfitTracker), SkySoft's; the defaults are what you had there. */
    static final class Profit {
        static final List<String> LINES = List.of("COINS", "TOTAL_PROFIT", "PROFIT_PER_HOUR", "BLOCKS", "UPTIME");
        boolean enabled = true;
        String period = "SESSION";                             // shown: SESSION, TODAY or TOTAL
        String priceSource = "SELL_ORDER";                     // INSTANT_SELL, SELL_ORDER or NPC_SELL; per item, itemSources
        boolean gemsByFlawless = false;                        // rough, flawed and fine gemstones at their flawless one's price over 80^n
        int pauseAfter = 60;                                   // seconds without tracked activity before uptime pauses; 0: never
        int maxItems = 5;
        boolean icons = true, quantityLeft = false, highlight = true, background = false;
        Set<String> lines = new LinkedHashSet<>(List.of("TOTAL_PROFIT", "PROFIT_PER_HOUR", "UPTIME"));  // of LINES
        int x = -14, y = 54;                                   // the HUD's corner, GUI pixels; x < 0: from the right edge
        float scale = 0.8f;
        Set<String> excluded = new TreeSet<>(), custom = new TreeSet<>();  // items not tracked; items tracked besides the preset's
        Map<String, String> itemSources = new TreeMap<>();     // an item's own price source
    }

    private static Config load() {
        try {
            if (Files.exists(FILE)) {
                Config c = GSON.fromJson(Files.readString(FILE), Config.class);
                if (c != null) {  // a hand-edited null keeps its default
                    if (c.routesOff == null) c.routesOff = new TreeSet<>();
                    if (c.colors == null) c.colors = new EnumMap<>(Corpse.class);
                    c.espLife = Math.max(0, c.espLife);
                    if (c.profit == null) c.profit = new Profit();
                    Profit p = c.profit, d = new Profit();
                    if (p.lines == null) p.lines = d.lines;
                    if (p.excluded == null) p.excluded = d.excluded;
                    if (p.custom == null) p.custom = d.custom;
                    if (p.itemSources == null) p.itemSources = d.itemSources;
                    if (p.period == null) p.period = d.period;
                    if (p.priceSource == null) p.priceSource = d.priceSource;
                    return c;
                }
            }
        } catch (IOException | RuntimeException e) {
            ShaftCoords.LOG.warn("Can't read {}, using the defaults", FILE, e);
        }
        return new Config();
    }

    void save() {
        try {
            Files.writeString(FILE, GSON.toJson(this));
        } catch (IOException e) {
            ShaftCoords.LOG.warn("Can't save {}", FILE, e);
        }
    }
}
