package shaftcoords;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** One period's mining profit (ProfitTracker): items gained by internal name, purse coins, blocks mined, time active. */
final class ProfitStats {
    private static final Pattern GEM = Pattern.compile("(ROUGH|FLAWED|FINE)_\\w+_GEM");
    Map<String, Long> items = new LinkedHashMap<>();
    double coins;
    long blocks, uptimeMs;

    /** How many of a rough, flawed or fine gemstone make a flawless one (80 a step up), or 0 for anything else. */
    static long perFlawless(String item) {
        Matcher m = GEM.matcher(item);
        if (!m.matches()) return 0;
        return switch (m.group(1)) {
            case "ROUGH" -> 80L * 80 * 80;
            case "FLAWED" -> 80L * 80;
            default -> 80;
        };
    }

    /** A rough, flawed or fine gemstone's flawless one: FLAWED_AMETHYST_GEM is FLAWLESS_AMETHYST_GEM. */
    static String flawless(String item) {
        return item.replaceFirst("^(ROUGH|FLAWED|FINE)_", "FLAWLESS_");
    }

    void add(String item, long amount) {
        items.merge(item, amount, Long::sum);
    }

    /** 1.23M, 456.7k, 789. */
    static String coins(double v) {
        double a = Math.abs(v);
        if (a >= 1e9) return String.format(Locale.ROOT, "%.2fB", v / 1e9);
        if (a >= 1e6) return String.format(Locale.ROOT, "%.2fM", v / 1e6);
        if (a >= 1e3) return String.format(Locale.ROOT, "%.1fk", v / 1e3);
        return String.format(Locale.ROOT, "%.0f", v);
    }

    /** 12,345. */
    static String count(long n) {
        return String.format(Locale.ROOT, "%,d", n);
    }

    /** 1h 02m, 5m 03s, 12s. */
    static String time(long ms) {
        long s = ms / 1000;
        if (s >= 3600) return String.format(Locale.ROOT, "%dh %02dm", s / 3600, s / 60 % 60);
        if (s >= 60) return String.format(Locale.ROOT, "%dm %02ds", s / 60, s % 60);
        return s + "s";
    }

    /** Coins an hour at `total` over `uptimeMs`, 0 under a second. */
    static double perHour(double total, long uptimeMs) {
        return uptimeMs < 1000 ? 0 : total * 3_600_000 / uptimeMs;
    }
}
