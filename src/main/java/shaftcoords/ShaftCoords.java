package shaftcoords;

import at.hannibal2.skyhanni.api.event.HandleEvent;
import at.hannibal2.skyhanni.api.event.SkyHanniEvents;
import at.hannibal2.skyhanni.config.storage.OrderedWaypointsRoutes;
import at.hannibal2.skyhanni.data.IslandType;
import at.hannibal2.skyhanni.data.ProfileStorageData;
import at.hannibal2.skyhanni.data.model.waypoints.SkyHanniWaypoint;
import at.hannibal2.skyhanni.data.model.waypoints.WaypointFormats;
import at.hannibal2.skyhanni.data.model.waypoints.Waypoints;
import at.hannibal2.skyhanni.data.model.TabWidget;
import at.hannibal2.skyhanni.events.IslandChangeEvent;
import at.hannibal2.skyhanni.events.PlaySoundEvent;
import at.hannibal2.skyhanni.events.SecondPassedEvent;
import at.hannibal2.skyhanni.events.WidgetUpdateEvent;
import at.hannibal2.skyhanni.events.entity.EntityClickEvent;
import at.hannibal2.skyhanni.events.mining.GlaciteMineshaftDetectEvent;
import at.hannibal2.skyhanni.features.mining.glacitemineshaft.MineshaftDetection;
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule;
import com.mojang.brigadier.arguments.StringArgumentType;
import kotlin.Pair;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;

/**
 * SAMMUI SHAFTIN SUPPLEMENTARIES, a SkyHanni addon: the moment SkyHanni's Mineshaft Detection knows which Glacite Mineshaft you're in,
 * that mineshaft's route (see Routes, bundled from src/main/resources/routes) goes into SkyHanni's Ordered Waypoints: saved
 * in SkyHanni's routes as ROUTE (replaced each time) and loaded with /sho load, so SkyHanni does the rest (nearest
 * waypoint first, its chat message, /sho skip and the others). /sss load <shaft> does the same by hand, for a type as
 * SkyHanni names it (AMBE_1, RUBY_C, ...; or "peri 1"). Chat says ">> Loaded crystal route", ">> No route for Topaz 1
 * mineshaft", or, DETECT_MS into a mineshaft with no type from SkyHanni, ">> Failed to detect shaft". Also Corpse ESP
 * (CorpseEsp), the bugged pity alarm (BuggedPity) and the mining Profit Tracker (ProfitTracker). /sss alone opens the
 * settings (ConfigScreen): with "Auto start route" off, or a route turned off there, entering doesn't load it (/sss load
 * still does).
 */
@SkyHanniModule
public final class ShaftCoords implements ClientModInitializer {
    static final Logger LOG = LoggerFactory.getLogger("sammui-shaftin-supplementaries");
    static final String ROUTE = "shaftcoords";
    static final long DETECT_MS = 10_000;  // in a mineshaft this long with no type from SkyHanni: "Failed to detect shaft"
    private long enteredAt;                // when you entered the mineshaft, until its type is detected (0: not waiting)
    private boolean openSettings;          // next tick: the chat that ran /sss has to close first

    @Override
    public void onInitializeClient() {
        SkyHanniEvents.INSTANCE.register(this);
        SkyHanniEvents.INSTANCE.register(BuggedPity.INSTANCE);
        SkyHanniEvents.INSTANCE.register(ProfitTracker.INSTANCE);
        CorpseEsp.init();
        BuggedPity.INSTANCE.init();
        ProfitTracker.INSTANCE.init();
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) -> dispatcher.register(
                ClientCommands.literal("sss").executes(c -> {  // SAMMUI SHAFTIN SUPPLEMENTARIES
                    openSettings = true;
                    return 1;
                }).then(ClientCommands.literal("load").then(ClientCommands.argument("shaft", StringArgumentType.greedyString()).executes(c -> {
                    String type = StringArgumentType.getString(c, "shaft").trim().replace(' ', '_').toUpperCase(Locale.ROOT);  // "peri 1" too
                    load(type, type, false);
                    return 1;
                })))));
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (!openSettings) return;
            openSettings = false;
            mc.gui.setScreen(new ConfigScreen());
        });
    }

    @HandleEvent
    public void onIslandChange(IslandChangeEvent event) {
        enteredAt = event.getNewIsland() == IslandType.MINESHAFT && Config.INSTANCE.autoStart ? System.currentTimeMillis() : 0;
    }

    @HandleEvent
    public void onMineshaft(GlaciteMineshaftDetectEvent event) {
        enteredAt = 0;
        MineshaftDetection.MineshaftType type = event.getType();
        Minecraft.getInstance().execute(() -> {  // on the client thread
            CorpseEsp.shaft(type.name(), type.getColor().getChatColor());
            if (Config.INSTANCE.autoStart) load(type.name(), type.getDisplayName(), true);
        });
    }

    @HandleEvent
    public void onWidget(WidgetUpdateEvent event) {
        if (event.isWidget(TabWidget.FROZEN_CORPSES)) CorpseEsp.tabList(event.getCleanLines());
    }

    @HandleEvent(onlyOnIsland = IslandType.MINESHAFT)
    public void onEntityClick(EntityClickEvent event) {
        CorpseEsp.clicked(event.getClickedEntity());
    }

    /** Mute Corpses, as YOTS Ball's: the Organ Donor's corpse sound, muted in every mineshaft (SkyHanni's waits till all are found). */
    @HandleEvent(onlyOnIsland = IslandType.MINESHAFT)
    public void onSound(PlaySoundEvent event) {
        if (Config.INSTANCE.muteCorpses && event.getSoundName().equals("block.note_block.harp")) event.cancel();
    }

    @HandleEvent
    public void onSecond(SecondPassedEvent event) {
        if (enteredAt == 0 || System.currentTimeMillis() - enteredAt < DETECT_MS) return;
        enteredAt = 0;
        Minecraft.getInstance().execute(() -> say("§c>> Failed to detect shaft"));
    }

    /**
     * Loads the route for mineshaft `type` (SkyHanni's name: AMBE_1, ...), `name` as you'd say it, and says how it went;
     * `auto` (on entering) unless its route is turned off.
     */
    private static void load(String type, String name, boolean auto) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        String file = Routes.file(type);
        if (file == null) {
            say("§e>> No route for " + name + "§e mineshaft");
            return;
        }
        String route = Routes.name(file);
        if (auto && Config.INSTANCE.routesOff.contains(file)) {
            say("§e>> The " + route + " route is turned off (/sss)");
            return;
        }
        Pair<Waypoints<SkyHanniWaypoint>, String> parsed = WaypointFormats.INSTANCE.load(read(file));
        OrderedWaypointsRoutes saved = ProfileStorageData.INSTANCE.getOrderedWaypointsRoutes();  // set once you join Hypixel
        if (parsed == null || saved == null) {
            say("§c>> Failed to load " + route + " route ("
                    + (parsed == null ? "SkyHanni can't read " + file : "SkyHanni's routes aren't loaded: join Hypixel first") + ")");
            return;
        }
        if (saved.getRoutes() == null) saved.setRoutes(new HashMap<>());
        saved.getRoutes().put(ROUTE, parsed.getFirst());
        LOG.info("{} mineshaft: loading {}", type, file);
        mc.player.connection.sendCommand("sho load " + ROUTE);
        say("§a>> Loaded " + route + " route");
    }

    private static void say(String message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.player.sendSystemMessage(Component.literal(message));
    }

    /** A bundled route's text, or "" if it isn't there. */
    private static String read(String file) {
        try (InputStream in = ShaftCoords.class.getResourceAsStream("/routes/" + file)) {
            return in == null ? "" : new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.warn("Can't read route {}", file, e);
            return "";
        }
    }
}
