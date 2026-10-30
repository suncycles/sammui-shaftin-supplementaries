package shaftcoords;

import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/** Which route a Glacite Mineshaft gets, by SkyHanni's name for its type (MineshaftDetection.MineshaftType). */
final class Routes {
    // A type's first four letters (its gemstone or ore) to its route. Any Crystal mineshaft (a *_C type) gets crystal.
    static final Map<String, String> BY_KIND = Map.of("AMBE", "amber", "AMET", "amethyst", "JASP", "jasper",
            "OPAL", "opal", "SAPP", "sapphire", "TUNG", "tungsten", "UMBE", "umber", "TOPA", "topaz", "JADE", "jade");
    // Types with a route of their own, ahead of their kind's
    static final Map<String, String> BY_TYPE = Map.of("RUBY_1", "ruby_1.txt", "RUBY_2", "ruby_2.txt",
            "ONYX_1", "onyx_1.txt", "ONYX_2", "onyx_2.txt", "AQUA_1", "aquamarine_1.txt", "AQUA_2", "aquamarine_2.txt",
            "CITR_1", "citrine_1.txt", "CITR_2", "citrine_2.txt", "PERI_1", "peridot_1.txt", "PERI_2", "peridot_2.txt");

    /** The route file for a mineshaft type (AMBE_1, RUBY_C, ...), or null if there's no route for it. */
    static String file(String type) {
        if (type.endsWith("_C")) return "crystal_shaft.txt";
        String own = BY_TYPE.get(type);
        if (own != null) return own;
        String kind = BY_KIND.get(type.substring(0, Math.min(4, type.length())));
        return kind == null ? null : kind + "_shaft.txt";
    }

    /** Every route file, by name. */
    static List<String> files() {
        TreeSet<String> files = new TreeSet<>(BY_TYPE.values());
        for (String kind : BY_KIND.values()) files.add(kind + "_shaft.txt");
        files.add("crystal_shaft.txt");
        return List.copyOf(files);
    }

    /** A route file as you'd say it: "amber", "ruby 1". */
    static String name(String file) {
        return file.replace("_shaft.txt", "").replace(".txt", "").replace('_', ' ');
    }
}
