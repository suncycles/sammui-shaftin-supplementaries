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
import java.util.function.Consumer;

/**
 * The mining Profit Tracker's settings (Config.Profit), from /sss: on/off, the period shown, prices, how it looks, the
 * summary lines (green shown, red not), moving it, the items, and resetting a period (click twice).
 */
final class ProfitScreen extends Screen {
    private static final int W = 400, GREY = 0xFFA0A0A0;
    private static final List<String> LINE_NAMES = List.of("Coins", "Total Profit", "Profit/h", "Blocks Mined", "Uptime");  // Config.Profit.LINES'
    private final Screen parent;
    private final Config.Profit cfg = Config.INSTANCE.profit;
    private String armed;  // the period whose reset was clicked once
    private long armedAt;

    ProfitScreen(Screen parent) {
        super(Component.literal("Mining Profit Tracker"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int x = (width - W) / 2;
        addRenderableWidget(CycleButton.onOffBuilder(cfg.enabled).create(x, 22, 98, 20, Component.literal("Tracker"), (b, v) -> cfg.enabled = v));
        addRenderableWidget(CycleButton.builder((String p) -> Component.literal(ProfitTracker.periodName(p)), cfg.period)
                .withValues(ProfitTracker.PERIODS).create(x + 100, 22, 98, 20, Component.literal("Show"), (b, v) -> cfg.period = v));
        addRenderableWidget(CycleButton.builder((String s) -> Component.literal(ProfitTracker.sourceName(s)), cfg.priceSource)
                .withValues(ProfitTracker.SOURCES).create(x + 200, 22, 98, 20, Component.literal("Prices"), (b, v) -> cfg.priceSource = v));
        addRenderableWidget(CycleButton.booleanBuilder(Component.literal("Left"), Component.literal("Right"), cfg.quantityLeft)
                .create(x + 300, 22, 100, 20, Component.literal("Quantity"), (b, v) -> cfg.quantityLeft = v));
        addRenderableWidget(CycleButton.onOffBuilder(cfg.icons).create(x, 44, 98, 20, Component.literal("Icons"), (b, v) -> cfg.icons = v));
        addRenderableWidget(CycleButton.onOffBuilder(cfg.highlight).create(x + 100, 44, 98, 20, Component.literal("Highlight"), (b, v) -> cfg.highlight = v));
        addRenderableWidget(CycleButton.onOffBuilder(cfg.background).create(x + 200, 44, 98, 20, Component.literal("Background"), (b, v) -> cfg.background = v));
        addRenderableWidget(Button.builder(Component.literal("Items..."), b -> minecraft.gui.setScreen(new ProfitItemsScreen(this))).bounds(x + 300, 44, 100, 20).build());
        number(x, String.valueOf(cfg.pauseAfter), v -> cfg.pauseAfter = v);
        number(x + 100, String.valueOf(cfg.maxItems), v -> cfg.maxItems = v);
        addRenderableWidget(Button.builder(Component.literal("Move HUD"), b -> minecraft.gui.setScreen(new HudMoveScreen(this))).bounds(x + 200, 79, 98, 20).build());
        addRenderableWidget(CycleButton.booleanBuilder(Component.literal("Flawless"), Component.literal("Own"), cfg.gemsByFlawless)
                .create(x + 300, 79, 100, 20, Component.literal("Gems"), (b, v) -> cfg.gemsByFlawless = v))
                .setTooltip(Tooltip.create(Component.literal("Price rough, flawed and fine gemstones from the flawless one: a fine is 1/80 of it, "
                        + "a flawed 1/6,400, a rough 1/512,000 (80 of each make the next)")));
        for (int i = 0; i < Config.Profit.LINES.size(); i++) {
            String line = Config.Profit.LINES.get(i), name = LINE_NAMES.get(i);
            addRenderableWidget(Button.builder(lineLabel(line, name), b -> {
                if (!cfg.lines.remove(line)) cfg.lines.add(line);
                b.setMessage(lineLabel(line, name));
            }).bounds(x + i * 80, 116, 78, 20).build());
        }
        for (int i = 0; i < ProfitTracker.PERIODS.size(); i++) {
            String period = ProfitTracker.PERIODS.get(i);
            addRenderableWidget(Button.builder(resetLabel(period), b -> {
                long now = System.currentTimeMillis();
                if (period.equals(armed) && now - armedAt < 3000) {
                    ProfitTracker.INSTANCE.reset(period);
                    armed = null;
                    b.setMessage(Component.literal("§a" + ProfitTracker.periodName(period) + " reset"));
                } else {
                    armed = period;
                    armedAt = now;
                    b.setMessage(Component.literal("§cClick again to reset"));
                }
            }).bounds(x + i * 134, 142, 132, 20).build());
        }
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose()).bounds(width / 2 - 75, 172, 150, 20).build());
    }

    private void number(int x, String value, Consumer<Integer> changed) {
        EditBox box = new EditBox(font, x, 80, 98, 18, Component.empty());
        box.setMaxLength(5);
        box.setValue(value);
        box.setResponder(s -> {
            if (s.matches("\\d{1,5}")) changed.accept(Integer.parseInt(s));
        });
        addRenderableWidget(box);
    }

    private Component lineLabel(String line, String name) {
        return Component.literal((cfg.lines.contains(line) ? "§a" : "§c") + name);
    }

    private static Component resetLabel(String period) {
        return Component.literal("Reset " + ProfitTracker.periodName(period));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        int x = (width - W) / 2;
        g.centeredText(font, title.getString(), width / 2, 8, 0xFFFFFFFF);
        g.text(font, "Pause after (s, 0: never)", x, 69, GREY);
        g.text(font, "Most items shown", x + 100, 69, GREY);
        g.text(font, "Summary lines: green shown, red not", x, 105, GREY);
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }

    @Override
    public void removed() {
        Config.INSTANCE.save();
    }
}
