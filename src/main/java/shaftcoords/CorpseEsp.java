package shaftcoords;

import at.hannibal2.skyhanni.data.IslandType;
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
 * till you leave); and when corpses are found the title gives the mineshaft and counts them, "PERI 1 2L 1U" (Lapis blue,
 * Umber orange, Tungsten grey, Vanguard dark aqua), TITLE_WAIT ticks after the last one found or the mineshaft's type, so
 * what comes in together shares one title.
 */
final class CorpseEsp {
    static final int TITLE_WAIT = 50;  // ticks: 2.5 s, for the server to send the whole shaft
    private record Found(ArmorStand stand, Corpse type, long at) {}
    private static final Map<UUID, Found> found = new LinkedHashMap<>();  // by UUID: a corpse sent again isn't new
    private static ClientLevel level;  // found's: a new one starts afresh
    private static String shaft;       // the mineshaft's type in SkyHanni's colour, "§2PERI 1", once SkyHanni detects it
    private static int tick, titleAt = -1;

    static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(CorpseEsp::tick);
        LevelRenderEvents.BEFORE_GIZMOS.register(context -> render());  // just before the frame's gizmos are taken
    }

    /** SkyHanni detected this mineshaft's type (AMBE_1, ...), its colour "§6": on the client thread. */
    static void shaft(String type, String color) {
        shaft = color + type.replace('_', ' ');
        if (!found.isEmpty()) titleAt = tick + TITLE_WAIT;
    }

    private static void tick(Minecraft mc) {
        tick++;
        Config cfg = Config.INSTANCE;
        boolean in = mc.level != null && IslandType.MINESHAFT.isInIsland();
        if (mc.level != level || !in) {
            found.clear();
            level = mc.level;
            shaft = null;
            titleAt = -1;
        }
        if (!in || !(cfg.corpseEsp || cfg.corpseTitle)) return;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (!(e instanceof ArmorStand stand)) continue;
            UUID at = stand.getUUID();
            Found f = found.get(at);
            if (f != null) {
                if (f.stand() != stand) found.put(at, new Found(stand, f.type(), f.at()));
                continue;
            }
            ItemStack boots = stand.getItemBySlot(EquipmentSlot.FEET);
            Corpse type = Corpse.of(stand.getItemBySlot(EquipmentSlot.HEAD).is(Items.SEA_LANTERN), boots.is(Items.LEATHER_BOOTS),
                    boots.getHoverName().getString());
            if (type == null) continue;
            found.put(at, new Found(stand, type, System.currentTimeMillis()));
            titleAt = tick + TITLE_WAIT;
        }
        if (tick == titleAt && cfg.corpseTitle) {
            Map<Corpse, Integer> count = new EnumMap<>(Corpse.class);
            for (Found f : found.values()) count.merge(f.type(), 1, Integer::sum);
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
            if (cfg.espLife > 0 && left <= 0 || f.stand().isRemoved()) continue;
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
