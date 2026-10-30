package shaftcoords;

import at.hannibal2.skyhanni.api.event.HandleEvent;
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule;
import at.hannibal2.skyhanni.data.MiningApi;
import at.hannibal2.skyhanni.data.model.TabWidget;
import at.hannibal2.skyhanni.events.IslandChangeEvent;
import at.hannibal2.skyhanni.events.WidgetUpdateEvent;
import at.hannibal2.skyhanni.events.mining.OreMinedEvent;
import at.hannibal2.skyhanni.features.mining.MineshaftPityDisplay;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

/**
 * Bugged pity alarm: mine a block that counts towards Glacite Mineshaft pity in the Glacite Tunnels and, if the tab's
 * Pity widget hasn't moved PityWatch.WAIT_MS later, "BUGGED PITY" goes up as a title with a jingle (once, till it moves).
 * Needs the Pity widget in your tab list; without it, it stays quiet.
 */
@SkyHanniModule  // SkyHanni's event bus takes handlers only from these (and public)
public final class BuggedPity {
    static final BuggedPity INSTANCE = new BuggedPity();
    private static final float[] JINGLE = {0.5f, 0.63f, 0.75f, 1f, 0.75f, 1f};  // note block pling pitches, JINGLE_GAP ticks apart
    private static final int JINGLE_GAP = 3;
    private final PityWatch watch = new PityWatch();
    private int jingleAt = -1, tick;

    void init() {
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    @HandleEvent
    public void onWidget(WidgetUpdateEvent event) {
        if (!event.isWidget(TabWidget.PITY)) return;
        for (String line : event.getCleanLines()) {
            Integer pity = PityWatch.parse(line);
            if (pity != null) watch.pity(pity);
        }
    }

    @HandleEvent
    public void onOreMined(OreMinedEvent event) {
        if (Config.INSTANCE.buggedPity && MiningApi.INSTANCE.inGlacialTunnels()
                && MineshaftPityDisplay.PityBlock.Companion.getPityBlock(event.getOriginalOre()) != null) {
            watch.mined(System.currentTimeMillis());
        }
    }

    @HandleEvent
    public void onIslandChange(IslandChangeEvent event) {
        watch.reset();
    }

    private void tick(Minecraft mc) {
        tick++;
        if (watch.due(System.currentTimeMillis()) && Config.INSTANCE.buggedPity && mc.player != null) {
            mc.gui.hud.setTimes(5, 50, 15);
            mc.gui.hud.setSubtitle(Component.empty());
            mc.gui.hud.setTitle(Component.literal("§c§lBUGGED PITY"));
            jingleAt = tick;
        }
        int note = jingleAt < 0 ? -1 : tick - jingleAt;
        if (note >= 0 && note % JINGLE_GAP == 0) {
            if (note / JINGLE_GAP >= JINGLE.length) jingleAt = -1;
            else mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_PLING.value(), JINGLE[note / JINGLE_GAP], 1f));
        }
    }
}
