package shaftcoords;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Moving the Profit Tracker's HUD: drag it, scroll to resize it; Esc when done. Left of the middle it's kept from the
 * left edge, right of it from the right (so it grows away from the edge), as Config.Profit.x says.
 */
final class HudMoveScreen extends Screen {
    private final Screen parent;
    private final Config.Profit cfg = Config.INSTANCE.profit;
    private double grabX, grabY;  // where in the HUD it was grabbed
    private boolean dragging;

    HudMoveScreen(Screen parent) {
        super(Component.literal("Move the Profit Tracker"));
        this.parent = parent;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        // none: the HUD where it'll be
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        ProfitTracker.INSTANCE.render(g, true, false, -1, -1);
        g.centeredText(font, "Drag the tracker to move it, scroll to resize it (" + Math.round(cfg.scale * 100) + "%), Esc when done",
                width / 2, height - 20, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        ProfitTracker t = ProfitTracker.INSTANCE;
        dragging = event.x() >= t.hudX && event.x() <= t.hudX + t.hudW && event.y() >= t.hudY && event.y() <= t.hudY + t.hudH;
        grabX = event.x() - t.hudX;
        grabY = event.y() - t.hudY;
        return dragging || super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (!dragging) return super.mouseDragged(event, dx, dy);
        ProfitTracker t = ProfitTracker.INSTANCE;
        int left = (int) Math.round(event.x() - grabX), top = (int) Math.round(event.y() - grabY);
        left = Math.max(0, Math.min(left, width - t.hudW));
        cfg.y = Math.max(0, Math.min(top, height - t.hudH));
        cfg.x = left + t.hudW / 2 > width / 2 ? Math.min(-1, left + t.hudW - width) : left;
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        dragging = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        cfg.scale = Math.max(0.5f, Math.min(2f, Math.round((cfg.scale + (scrollY > 0 ? 0.1f : -0.1f)) * 10) / 10f));
        return true;
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
