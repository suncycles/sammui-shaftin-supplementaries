package shaftcoords;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
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

    int color(Corpse c) {
        return Corpse.rgb(colors.get(c), c.color);
    }

    private static Config load() {
        try {
            if (Files.exists(FILE)) {
                Config c = GSON.fromJson(Files.readString(FILE), Config.class);
                if (c != null) {  // a hand-edited null keeps its default
                    if (c.routesOff == null) c.routesOff = new TreeSet<>();
                    if (c.colors == null) c.colors = new EnumMap<>(Corpse.class);
                    c.espLife = Math.max(0, c.espLife);
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
