package shaftcoords;

import at.hannibal2.skyhanni.events.ItemAddEvent;
import at.hannibal2.skyhanni.utils.ItemPriceSource;
import at.hannibal2.skyhanni.utils.ItemPriceUtils;
import at.hannibal2.skyhanni.utils.ItemUtils;
import at.hannibal2.skyhanni.utils.NeuItems;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SkyHanni's item helpers. They take its NeuInternalName, a Kotlin inline class, so their names are mangled
 * ("getPriceOrNull-0mM9I0c") and Java can't name them: they're found by reflection, by the name before the '-'.
 * Prices are kept PRICE_MS. Anything that fails gives null (logged once).
 */
final class Sh {
    static final long PRICE_MS = 10_000;
    private static final Method INTERNAL_NAME = find(ItemAddEvent.class, "getInternalName");
    private static final Method NAME = find(ItemUtils.class, "getRepoItemName", String.class);
    private static final Method STACK = find(NeuItems.class, "getItemStackOrNull", String.class);
    private static final Method PRICE = find(ItemPriceUtils.class, "getPriceOrNull", String.class, ItemPriceSource.class, List.class);
    private record Price(Double value, long at) {}
    private static final Map<String, Price> prices = new HashMap<>();
    private static boolean warned;

    private static Method find(Class<?> owner, String name, Class<?>... params) {
        for (Method m : owner.getMethods()) {
            if ((m.getName().equals(name) || m.getName().startsWith(name + "-")) && java.util.Arrays.equals(m.getParameterTypes(), params)) return m;
        }
        ShaftCoords.LOG.warn("SkyHanni has no {}.{}: the profit tracker won't work", owner.getSimpleName(), name);
        return null;
    }

    private static Object call(Method m, Object on, Object... args) {
        try {
            return m == null ? null : m.invoke(on, args);
        } catch (ReflectiveOperationException | RuntimeException e) {
            if (!warned) ShaftCoords.LOG.warn("SkyHanni's {} failed", m.getName(), e);
            warned = true;
            return null;
        }
    }

    /** The item an ItemAddEvent is for, "GLACITE". */
    static String internalName(ItemAddEvent e) {
        return (String) call(INTERNAL_NAME, e);
    }

    /** The item's name with its rarity colour, "§aGlacite", or the internal name if SkyHanni doesn't know it. */
    static String name(String item) {
        Object n = call(NAME, ItemUtils.INSTANCE, item);
        return n == null ? item : (String) n;
    }

    static ItemStack stack(String item) {
        return (ItemStack) call(STACK, NeuItems.INSTANCE, item);
    }

    /** The item's price by `source` (Bazaar; else its lowest BIN, as SkyHanni does), or null if there's none. */
    static Double price(String item, ItemPriceSource source) {
        String key = item + "|" + source;
        long now = System.currentTimeMillis();
        Price p = prices.get(key);
        if (p == null || now - p.at() > PRICE_MS) {
            p = new Price((Double) call(PRICE, ItemPriceUtils.INSTANCE, item, source, List.of()), now);
            prices.put(key, p);
        }
        return p.value();
    }
}
