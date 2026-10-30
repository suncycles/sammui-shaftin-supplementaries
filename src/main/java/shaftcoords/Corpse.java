package shaftcoords;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A Glacite Mineshaft's Frozen Corpse, by what its armor stand wears (as Blade Extras' Corpse ESP tells them): a sea
 * lantern on its head is Lapis; leather boots are Tungsten if they're Mineral Boots, Umber if Yog Boots, else Vanguard.
 */
enum Corpse {
    LAPIS('L', '9', 0x5555FF), UMBER('U', '6', 0xFFAA00), TUNGSTEN('T', '7', 0xAAAAAA), VANGUARD('V', '3', 0x00AAAA);

    final char letter, format;  // the title's letter and its colour code: blue, gold (orange), grey, dark aqua
    final int color;            // the ESP's default colour, RGB

    Corpse(char letter, char format, int color) {
        this.letter = letter;
        this.format = format;
        this.color = color;
    }

    /** The corpse an armor stand is, from its gear, or null if it isn't one. */
    static Corpse of(boolean seaLanternHead, boolean leatherBoots, String bootsName) {
        if (seaLanternHead) return LAPIS;
        if (!leatherBoots) return null;
        return bootsName.contains("Mineral Boots") ? TUNGSTEN : bootsName.contains("Yog Boots") ? UMBER : VANGUARD;
    }

    /**
     * The title for the corpses found in mineshaft `shaft` ("§2PERI 1", in SkyHanni's colour; null till SkyHanni knows):
     * "§2PERI 1 §92L §61U" (Lapis, Umber, Tungsten, Vanguard, those with none left out); with no corpses, the mineshaft.
     */
    static String title(String shaft, Map<Corpse, Integer> found) {
        StringBuilder s = new StringBuilder();
        for (Corpse c : values()) {
            int n = found.getOrDefault(c, 0);
            if (n > 0) s.append(s.isEmpty() ? "" : " ").append('§').append(c.format).append(n).append(c.letter);
        }
        return shaft == null ? s.toString() : s.isEmpty() ? shaft : shaft + " " + s;
    }

    /**
     * The corpses a mineshaft has, by the tab's Frozen Corpses widget: a line each, "Lapis: NOT LOOTED" (SkyHanni's
     * pattern; formatting codes ignored). Kinds this doesn't know are left out.
     */
    static Map<Corpse, Integer> fromTab(List<String> lines) {
        Map<Corpse, Integer> count = new EnumMap<>(Corpse.class);
        for (String line : lines) {
            Matcher m = TAB_LINE.matcher(line.replaceAll("§.", ""));
            if (!m.matches()) continue;
            for (Corpse c : values()) if (c.name().equalsIgnoreCase(m.group(1))) count.merge(c, 1, Integer::sum);
        }
        return count;
    }

    private static final Pattern TAB_LINE = Pattern.compile("\\s*(\\w+): (?:NOT )?LOOTED\\s*");

    /** "FFAA00" (or "#FFAA00") as RGB, or `fallback` if it isn't 6 hex digits. */
    static int rgb(String hex, int fallback) {
        String h = hex == null ? "" : hex.startsWith("#") ? hex.substring(1) : hex;
        return h.matches("[0-9a-fA-F]{6}") ? Integer.parseInt(h, 16) : fallback;
    }
}
