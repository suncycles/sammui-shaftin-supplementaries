package shaftcoords;

import java.util.Map;

/** Self-check for Corpse, run by build.ps1: which corpse an armor stand is, the title, the colours; exits non-zero on failure. */
public final class CorpseCheck {
    public static void main(String[] args) {
        int fails = check(Corpse.of(true, false, "") == Corpse.LAPIS, "a sea lantern head is Lapis")
                + check(Corpse.of(false, true, "Mineral Boots") == Corpse.TUNGSTEN, "Mineral Boots are Tungsten")
                + check(Corpse.of(false, true, "§6Yog Boots") == Corpse.UMBER, "Yog Boots are Umber")
                + check(Corpse.of(false, true, "Vanguard Boots") == Corpse.VANGUARD, "other leather boots are Vanguard")
                + check(Corpse.of(false, false, "Mineral Boots") == null, "no sea lantern, no leather boots: not a corpse")
                + check(Corpse.title(null, Map.of(Corpse.LAPIS, 2, Corpse.UMBER, 1)).equals("§92L §61U"), "title 2L 1U")
                + check(Corpse.title("§2PERI 1", Map.of(Corpse.LAPIS, 2, Corpse.UMBER, 1)).equals("§2PERI 1 §92L §61U"), "title PERI 1 2L 1U")
                + check(Corpse.title(null, Map.of(Corpse.TUNGSTEN, 3, Corpse.LAPIS, 1, Corpse.VANGUARD, 0)).equals("§91L §73T"), "title in L U T V order, none left out")
                + check(Corpse.title("OPAL C", Map.of()).isEmpty(), "no corpses, no title")
                + check(Corpse.rgb("#ff8800", 0) == 0xFF8800 && Corpse.rgb("FFAA00", 0) == 0xFFAA00, "colours read")
                + check(Corpse.rgb("12345", 7) == 7 && Corpse.rgb("GGGGGG", 7) == 7 && Corpse.rgb(null, 7) == 7, "bad colours fall back");
        System.out.println(fails == 0 ? "ok    corpses: gear to corpse, the title, colours" : fails + " failed");
        System.exit(fails == 0 ? 0 : 1);
    }

    private static int check(boolean ok, String what) {
        if (!ok) System.out.println("FAIL  " + what);
        return ok ? 0 : 1;
    }
}
