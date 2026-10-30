package shaftcoords;

/** Self-check for PityWatch and ProfitStats, run by build.ps1; exits non-zero on failure. */
public final class MiningCheck {
    public static void main(String[] args) {
        int fails = 0;
        PityWatch w = new PityWatch();
        w.mined(0);
        fails += check(!w.due(10_000), "no alarm before the widget's been seen");
        w.pity(PityWatch.parse(" Glacite Mineshafts: 1,234/2,000"));
        w.mined(1000);
        fails += check(!w.due(3500), "no alarm before 3 s");
        w.pity(1240);
        fails += check(!w.due(9000), "the count moved: no alarm");
        w.mined(10_000);
        w.mined(12_000);  // a later block doesn't push the wait back
        w.pity(1240);     // the same count again isn't a move
        fails += check(w.due(13_000), "the count didn't move in 3 s: alarm");
        w.mined(14_000);
        fails += check(!w.due(20_000), "alarmed once, quiet till the count moves");
        w.pity(0);        // a mineshaft spawned: back to 0 is a move too
        w.mined(21_000);
        fails += check(w.due(24_000), "after it moves, it can alarm again");
        fails += check(PityWatch.parse("Pity:") == null && PityWatch.parse("Glacite Mineshafts: 561/2,000") == 561, "widget lines");

        fails += check(ProfitStats.coins(1_234_567).equals("1.23M") && ProfitStats.coins(45_670).equals("45.7k")
                && ProfitStats.coins(789).equals("789") && ProfitStats.coins(2.5e9).equals("2.50B"), "coins");
        fails += check(ProfitStats.count(12345).equals("12,345"), "counts");
        fails += check(ProfitStats.time(3_725_000).equals("1h 02m") && ProfitStats.time(303_000).equals("5m 03s")
                && ProfitStats.time(12_400).equals("12s"), "times");
        fails += check(ProfitStats.perHour(1_000_000, 1_800_000) == 2_000_000 && ProfitStats.perHour(5, 0) == 0, "per hour");
        fails += check(ProfitStats.perFlawless("ROUGH_AMETHYST_GEM") == 512_000 && ProfitStats.perFlawless("FLAWED_AMETHYST_GEM") == 6400
                && ProfitStats.perFlawless("FINE_AMETHYST_GEM") == 80 && ProfitStats.perFlawless("FLAWLESS_AMETHYST_GEM") == 0
                && ProfitStats.perFlawless("GLACITE") == 0 && ProfitStats.perFlawless("ROUGH_AQUAMARINE_GEM") == 512_000, "gemstones per flawless");
        fails += check(ProfitStats.flawless("FLAWED_AMETHYST_GEM").equals("FLAWLESS_AMETHYST_GEM")
                && ProfitStats.flawless("ROUGH_RUBY_GEM").equals("FLAWLESS_RUBY_GEM"), "a gemstone's flawless one");
        ProfitStats s = new ProfitStats();
        s.add("GLACITE", 64);
        s.add("GLACITE", 10);
        fails += check(s.items.get("GLACITE") == 74, "items add up");
        ProfitStats shaft = new ProfitStats();
        shaft.add("GLACITE", 6);
        shaft.add("UMBER", 3);
        shaft.coins = 10;
        shaft.blocks = 4;
        shaft.uptimeMs = 1000;
        s.uptimeMs = 2000;
        ProfitStats all = ProfitStats.sum(s, shaft);
        fails += check(all.items.equals(java.util.Map.of("GLACITE", 80L, "UMBER", 3L)) && all.coins == 10 && all.blocks == 4 && all.uptimeMs == 3000
                && s.items.get("GLACITE") == 74, "in and out of mineshafts added up, neither changed");
        System.out.println(fails == 0 ? "ok    mining: bugged pity, profit numbers" : fails + " failed");
        System.exit(fails == 0 ? 0 : 1);
    }

    private static int check(boolean ok, String what) {
        if (!ok) System.out.println("FAIL  " + what);
        return ok ? 0 : 1;
    }
}
