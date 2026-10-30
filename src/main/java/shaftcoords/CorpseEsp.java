package shaftcoords;

import at.hannibal2.skyhanni.data.IslandType;
import at.hannibal2.skyhanni.data.model.TabWidget;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Corpse ESP, after Blade Extras': in a Glacite Mineshaft each Frozen Corpse (see Corpse) gets a box in its colour
 * (Config.color), seen through walls, for Config.espLife seconds from when it's found with the seconds left over it (0:
 * till you leave), till you click it (to loot it). The title gives the mineshaft and its corpses, "PERI 1 2L 1U" (Lapis
 * blue, Umber orange, Tungsten grey, Vanguard dark aqua), counted from the tab's Frozen Corpses widget, which lists every
 * corpse in the shaft (the boxes come only as the server sends their armor stands, near you): once SkyHanni knows the
 * type and that list has stayed the same TAB_SETTLE ticks. Without the widget, TITLE_FALLBACK ticks after the type,
 * counting the corpses found by then.
 */
final class CorpseEsp {
    static final int TAB_SETTLE = 20, TITLE_FALLBACK = 200;  // ticks: 1 s, 10 s
    private record Found(ArmorStand stand, Corpse type, long at, boolean clicked) {}
    private static final Map<UUID, Found> found = new LinkedHashMap<>();  // by UUID: a corpse sent again isn't new
    private static ClientLevel level;  // all this is the level's: a new one (another shaft, or out of one) starts afresh
    private static String shaft;       // the mineshaft's type in SkyHanni's colour, "§2PERI 1", once SkyHanni detects it
    private static Map<Corpse, Integer> tab = Map.of();  // the tab's Frozen Corpses, since tabAt
    private static int tick, shaftAt, tabAt;
    private static boolean titled;

    static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(CorpseEsp::tick);
        LevelRenderEvents.BEFORE_GIZMOS.register(context -> render());  // just before the frame's gizmos are taken
    }

    /** SkyHanni detected this mineshaft's type (AMBE_1, ...), its colour "§6": on the client thread. */
    static void shaft(String type, String color) {
        shaft = color + type.replace('_', ' ');
        shaftAt = tick;
    }

    /** You clicked an entity: if it's a corpse, its box goes. */
    static void clicked(Entity e) {
        found.computeIfPresent(e.getUUID(), (id, f) -> new Found(f.stand(), f.type(), f.at(), true));
    }

    private static void tick(Minecraft mc) {
        tick++;
        Config cfg = Config.INSTANCE;
        if (mc.level != level) {  // not on leaving the island: SkyHanni may say it's a mineshaft after its type's come
            found.clear();
            level = mc.level;
            shaft = null;
            tab = Map.of();
            titled = false;
        }
        if (mc.level == null || !IslandType.MINESHAFT.isInIsland() || !(cfg.corpseEsp || cfg.corpseTitle)) return;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (!(e instanceof ArmorStand stand)) continue;
            UUID at = stand.getUUID();
            Found f = found.get(at);
            if (f != null) {
                if (f.stand() != stand) found.put(at, new Found(stand, f.type(), f.at(), f.clicked()));
                continue;
            }
            ItemStack boots = stand.getItemBySlot(EquipmentSlot.FEET);
            Corpse type = Corpse.of(stand.getItemBySlot(EquipmentSlot.HEAD).is(Items.SEA_LANTERN), boots.is(Items.LEATHER_BOOTS),
                    boots.getHoverName().getString());
            if (type != null) found.put(at, new Found(stand, type, System.currentTimeMillis(), false));
        }
        if (titled || shaft == null || !cfg.corpseTitle) return;
        Map<Corpse, Integer> now = TabWidget.FROZEN_CORPSES.isActive()
                ? Corpse.fromTab(TabWidget.FROZEN_CORPSES.getLines().stream().map(Component::getString).toList()) : Map.of();
        if (!now.equals(tab)) {
            tab = now;
            tabAt = tick;
        }
        boolean listed = !tab.isEmpty() && tick - tabAt >= TAB_SETTLE;
        if (listed || tick - shaftAt >= TITLE_FALLBACK) {
            titled = true;
            Map<Corpse, Integer> count = new EnumMap<>(Corpse.class);
            if (listed) count.putAll(tab);
            else for (Found f : found.values()) count.merge(f.type(), 1, Integer::sum);
            mc.gui.hud.setTimes(5, 40, 10);
            mc.gui.hud.setSubtitle(Component.empty());
            mc.gui.hud.setTitle(Component.literal(Corpse.title(shaft, count)));
        }
    }

    private static void render() {
        Config cfg = Config.INSTANCE;
        if (!cfg.corpseEsp || found.isEmpty()) return;
        long now = System.currentTimeMillis();
        for (Found f : found.values()) {
            long left = cfg.espLife * 1000L - (now - f.at());
            if (cfg.espLife > 0 && left <= 0 || f.clicked() || f.stand().isRemoved()) continue;
            int rgb = cfg.color(f.type());
            AABB box = f.stand().getBoundingBox();
            Gizmos.cuboid(box, GizmoStyle.strokeAndFill(0xFF000000 | rgb, 2f, 0x55000000 | rgb)).setAlwaysOnTop();
            if (cfg.espLife > 0) {
                Gizmos.billboardText((left + 999) / 1000 + "s", box.getCenter().add(0, box.getYsize() / 2 + 0.4, 0),
                        TextGizmo.Style.forColorAndCentered(0xFF000000 | rgb)).setAlwaysOnTop();
            }
        }
    }
}
