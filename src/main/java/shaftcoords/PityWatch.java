package shaftcoords;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bugged pity: Hypixel's Pity tab widget, "Glacite Mineshafts: 561/2,000", should move within WAIT_MS of mining a block
 * that counts towards it; if it doesn't, the pity is bugged. Once that's been said, it stays quiet till the count moves.
 */
final class PityWatch {
    static final long WAIT_MS = 3000;
    private static final Pattern LINE = Pattern.compile("Glacite Mineshafts: ([\\d,]+)/");
    private Integer pity;      // the count last seen; null till the widget's been seen
    private long minedAt;      // when a pity block was mined, waiting for the count to move (0: not waiting)
    private boolean alarmed;

    /** The count in a line of the widget, or null if it isn't that line. */
    static Integer parse(String line) {
        Matcher m = LINE.matcher(line);
        return m.find() ? Integer.valueOf(m.group(1).replace(",", "")) : null;
    }

    void pity(int value) {
        if (pity == null || value != pity) {
            minedAt = 0;
            alarmed = false;
        }
        pity = value;
    }

    void mined(long now) {
        if (pity != null && minedAt == 0 && !alarmed) minedAt = now;
    }

    /** True once, when the count hasn't moved WAIT_MS after a pity block was mined. */
    boolean due(long now) {
        if (minedAt == 0 || now - minedAt < WAIT_MS) return false;
        minedAt = 0;
        alarmed = true;
        return true;
    }

    void reset() {
        pity = null;
        minedAt = 0;
        alarmed = false;
    }
}
