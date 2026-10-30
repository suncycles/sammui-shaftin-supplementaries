package shaftcoords;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The Profit Tracker's items, PER_PAGE a page (those you've got in the period shown first): each tracked (green) or
 * not (red), and its price source (the tracker's by default); and an item to track besides, by internal name.
 */
final class ProfitItemsScreen extends Screen {
    private static final int W = 400, PER_PAGE = 8, TOP = 26;

    private final Screen parent;
    private final Config.Profit cfg = Config.INSTANCE.profit;
    private int page;
    private List<String> items;

    ProfitItemsScreen(Screen parent) {
        super(Component.literal("Profit Tracker items"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        ProfitStats shown = ProfitTracker.INSTANCE.stats(cfg.period);
        Set<String> all = new LinkedHashSet<>(ProfitTracker.MINING);
        all.addAll(cfg.custom);
        items = new ArrayList<>(all);
        items.sort(Comparator.comparingLong((String i) -> -shown.items.getOrDefault(i, 0L)));  // stable: the preset's order after
        page = Math.min(page, (items.size() - 1) / PER_PAGE);
        int x = (width - W) / 2;
        for (int r = 0; r < PER_PAGE && page * PER_PAGE + r < items.size(); r++) {
            String item = items.get(page * PER_PAGE + r);
            int y = TOP + r * 22;
            addRenderableWidget(Button.builder(tracked(item), b -> {
                if (!cfg.excluded.remove(item)) cfg.excluded.add(item);
                b.setMessage(tracked(item));
            }).bounds(x + 230, y, 70, 20).build());
            addRenderableWidget(CycleButton.builder((String s) -> Component.literal(s.equals("DEFAULT") ? "Default" : ProfitTracker.sourceName(s)),
                    cfg.itemSources.getOrDefault(item, "DEFAULT")).withValues(ProfitTracker.ITEM_SOURCES).displayOnlyValue()
                    .create(x + 302, y, 98, 20, Component.literal("Price"), (b, v) -> {
                        if (v.equals("DEFAULT")) cfg.itemSources.remove(item);
                        else cfg.itemSources.put(item, v);
                    }));
        }
        int y = TOP + PER_PAGE * 22 + 6;
        addRenderableWidget(Button.builder(Component.literal("<"), b -> turn(-1)).bounds(x, y, 20, 20).build()).active = page > 0;
        addRenderableWidget(Button.builder(Component.literal(">"), b -> turn(1)).bounds(x + 22, y, 20, 20).build()).active = (page + 1) * PER_PAGE < items.size();
        EditBox add = new EditBox(font, x + 46, y + 1, 150, 18, Component.empty());
        add.setMaxLength(64);
        add.setHint(Component.literal("Item ID to add, e.g. TITANIUM_ORE"));
        addRenderableWidget(add);
        addRenderableWidget(Button.builder(Component.literal("Add"), b -> {
            String item = add.getValue().trim().toUpperCase(Locale.ROOT).replace(' ', '_');
            if (item.isEmpty()) return;
            cfg.excluded.remove(item);
            if (!ProfitTracker.MINING.contains(item)) cfg.custom.add(item);
            rebuildWidgets();
        }).bounds(x + 200, y, 60, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose()).bounds(x + 300, y, 100, 20).build());
    }

    private void turn(int by) {
        page += by;
        rebuildWidgets();
    }

    private Component tracked(String item) {
        return Component.literal(cfg.excluded.contains(item) ? "§cNot tracked" : "§aTracked");
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        int x = (width - W) / 2;
        g.centeredText(font, title.getString() + " (" + (page + 1) + "/" + ((items.size() + PER_PAGE - 1) / PER_PAGE) + ")", width / 2, 8, 0xFFFFFFFF);
        ProfitStats shown = ProfitTracker.INSTANCE.stats(cfg.period);
        for (int r = 0; r < PER_PAGE && page * PER_PAGE + r < items.size(); r++) {
            String item = items.get(page * PER_PAGE + r);
            long n = shown.items.getOrDefault(item, 0L);
            String name = Sh.name(item) + (cfg.custom.contains(item) ? " §8(added)" : "") + (n > 0 ? " §7" + ProfitStats.count(n) + "x" : "");
            g.text(font, name, x, TOP + r * 22 + 6, 0xFFFFFFFF, true);
        }
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
