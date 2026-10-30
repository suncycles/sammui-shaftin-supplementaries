package shaftcoords;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * /sss: SAMMUI SHAFTIN SUPPLEMENTARIES' settings (Config), saved when you close it. The main toggles, the Profit Tracker's
 * (ProfitScreen), the ESP's life and its colours (hex, a swatch beside each), and a button per route: green loads when you
 * enter its mineshaft, red doesn't.
 */
final class ConfigScreen extends Screen {
    private static final int W = 400, COL = 80;  // the settings' width, centred; a colour's column
    private static final int TOGGLES = 22, MORE = 44, BOXES = 80, ROUTES = 118;  // rows' tops
    private static final int GREY = 0xFFA0A0A0;
    private final Config cfg = Config.INSTANCE;

    ConfigScreen() {
        super(Component.literal("SAMMUI SHAFTIN SUPPLEMENTARIES"));
    }

    @Override
    protected void init() {
        int x = (width - W) / 2;
        addRenderableWidget(CycleButton.onOffBuilder(cfg.autoStart).create(x, TOGGLES, 98, 20, Component.literal("Auto route"), (b, v) -> cfg.autoStart = v))
                .setTooltip(Tooltip.create(Component.literal("Auto start route: load the mineshaft's route when SkyHanni detects it")));
        addRenderableWidget(CycleButton.onOffBuilder(cfg.corpseEsp).create(x + 100, TOGGLES, 98, 20, Component.literal("Corpse ESP"), (b, v) -> cfg.corpseEsp = v));
        addRenderableWidget(CycleButton.onOffBuilder(cfg.corpseTitle).create(x + 200, TOGGLES, 98, 20, Component.literal("Title"), (b, v) -> cfg.corpseTitle = v))
                .setTooltip(Tooltip.create(Component.literal("The mineshaft and its corpses on screen when they're found: PERI 1 2L 1U")));
        addRenderableWidget(CycleButton.onOffBuilder(cfg.muteCorpses).create(x + 300, TOGGLES, 100, 20, Component.literal("Mute corpses"), (b, v) -> cfg.muteCorpses = v))
                .setTooltip(Tooltip.create(Component.literal("Mute the Organ Donor's corpse sound in every mineshaft")));
        addRenderableWidget(CycleButton.onOffBuilder(cfg.buggedPity).create(x, MORE, 98, 20, Component.literal("Bugged pity"), (b, v) -> cfg.buggedPity = v))
                .setTooltip(Tooltip.create(Component.literal("BUGGED PITY and a jingle when mining a block in the Glacite Tunnels doesn't move the tab's pity in 3 s")));
        addRenderableWidget(Button.builder(Component.literal("Mining Profit Tracker..."), b -> minecraft.gui.setScreen(new ProfitScreen(this)))
                .bounds(x + 100, MORE, 198, 20).build());
        box(x, COL - 10, String.valueOf(cfg.espLife), s -> {
            if (s.matches("\\d{1,5}")) cfg.espLife = Integer.parseInt(s);
        }).setTooltip(Tooltip.create(Component.literal("Seconds each corpse's box shows once it's found; 0: till you leave")));
        Corpse[] corpses = Corpse.values();
        for (int i = 0; i < corpses.length; i++) {
            Corpse c = corpses[i];
            box(x + COL * (i + 1), COL - 22, String.format("%06X", cfg.color(c)), s -> {
                if (Corpse.rgb(s, -1) >= 0) cfg.colors.put(c, s.replace("#", "").toUpperCase(Locale.ROOT));
            });
        }
        List<String> files = Routes.files();
        for (int i = 0; i < files.size(); i++) {
            String file = files.get(i);
            addRenderableWidget(Button.builder(route(file), b -> {
                if (!cfg.routesOff.remove(file)) cfg.routesOff.add(file);
                b.setMessage(route(file));
            }).bounds(x + i % 5 * 80, ROUTES + i / 5 * 22, 78, 20).build());
        }
        int under = ROUTES + (files.size() + 4) / 5 * 22 + 6;
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose()).bounds(width / 2 - 75, under, 150, 20).build());
    }

    private EditBox box(int x, int w, String value, Consumer<String> changed) {
        EditBox box = new EditBox(font, x, BOXES, w, 18, Component.empty());
        box.setMaxLength(7);
        box.setValue(value);
        box.setResponder(changed);
        return addRenderableWidget(box);
    }

    private Component route(String file) {
        String name = Routes.name(file);
        return Component.literal((cfg.routesOff.contains(file) ? "§c" : "§a") + Character.toUpperCase(name.charAt(0)) + name.substring(1));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        int x = (width - W) / 2;
        g.centeredText(font, title.getString(), width / 2, 8, 0xFFFFFFFF);
        g.text(font, "ESP life (s)", x, BOXES - 11, GREY);
        Corpse[] corpses = Corpse.values();
        for (int i = 0; i < corpses.length; i++) {
            int cx = x + COL * (i + 1);
            String name = corpses[i].name();
            g.text(font, name.charAt(0) + name.substring(1).toLowerCase(Locale.ROOT), cx, BOXES - 11, GREY);
            g.fill(cx + COL - 20, BOXES, cx + COL - 2, BOXES + 18, 0xFF000000 | cfg.color(corpses[i]));
        }
        g.text(font, "Routes: green loads when you enter its mineshaft, red doesn't", x, ROUTES - 11, GREY);
    }

    @Override
    public void removed() {
        cfg.save();
    }
}
