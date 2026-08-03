package mc506lw.mscpoServerlist.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class MscpoDropdown extends ClickableWidget {
	private static final int OPTION_HEIGHT = 14;
	private static final int MAX_VISIBLE = 8;

	private final List<String> options = new ArrayList<>();
	private final Consumer<Integer> onSelect;
	private int selectedIndex;
	private boolean open;

	public MscpoDropdown(int x, int y, int width, int height, Consumer<Integer> onSelect) {
		super(x, y, width, height, Text.empty());
		this.onSelect = onSelect;
	}

	public void setOptions(List<String> options) {
		this.options.clear();
		this.options.addAll(options);
		if (this.selectedIndex >= this.options.size()) {
			this.selectedIndex = Math.max(0, this.options.size() - 1);
		}
		if (this.selectedIndex < 0) {
			this.selectedIndex = 0;
		}
	}

	public void setSelected(int index) {
		this.selectedIndex = index;
	}

	public int getSelectedIndex() {
		return this.selectedIndex;
	}

	public boolean isOpen() {
		return this.open;
	}

	public void close() {
		this.open = false;
	}

	private int getPopupY() {
		return this.getY() + this.getHeight() + 1;
	}

	private int getPopupHeight() {
		return Math.min(this.options.size(), MAX_VISIBLE) * OPTION_HEIGHT + 2;
	}

	private int getScrollStart() {
		return Math.max(0, this.selectedIndex - MAX_VISIBLE + 1);
	}

	@Override
	protected void renderWidget(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		int x = this.getX();
		int y = this.getY();
		int w = this.getWidth();
		int h = this.getHeight();
		context.fill(x, y, x + w, y + h, 0xFF141414);
		context.fill(x + 1, y + 1, x + w - 1, y + h - 1, this.hovered ? 0xFF2A2A2A : 0xFF1D1D1D);
		String label = this.selectedIndex >= 0 && this.selectedIndex < this.options.size() ? this.options.get(this.selectedIndex) : "";
		context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, label, x + 4, y + (h - 8) / 2, 0xFFFFFFFF);
		context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, this.open ? "▲" : "▼", x + w - 9, y + (h - 8) / 2, 0xFFA0A0A0);
		if (this.open) {
			int popupY = this.getPopupY();
			int popupH = this.getPopupHeight();
			int scrollStart = this.getScrollStart();
			context.fill(x, popupY, x + w, popupY + popupH, 0xF0000000);
			for (int i = 0; i < this.options.size(); i++) {
				int itemY = popupY + 1 + i * OPTION_HEIGHT - scrollStart * OPTION_HEIGHT;
				if (itemY < popupY + 1 || itemY + OPTION_HEIGHT > popupY + popupH - 1) continue;
				if (i == this.selectedIndex) {
					context.fill(x + 1, itemY, x + w - 1, itemY + OPTION_HEIGHT, 0xFF3C3C3C);
				}
				context.drawTextWithShadow(
					MinecraftClient.getInstance().textRenderer,
					this.options.get(i),
					x + 4,
					itemY + 3,
					i == this.selectedIndex ? 0xFF55FF55 : 0xFFFFFFFF
				);
			}
			context.fill(x, popupY, x + w, popupY + 1, 0xFF3C3C3C);
		}
	}

	@Override
	public boolean mouseClicked(Click click, boolean doubled) {
		if (!this.isInteractable()) return false;
		double mx = click.x();
		double my = click.y();
		if (mx >= this.getX() && mx < this.getRight() && my >= this.getY() && my < this.getBottom()) {
			this.open = !this.open;
			this.playDownSound(MinecraftClient.getInstance().getSoundManager());
			return true;
		}
		if (this.open) {
			int popupY = this.getPopupY();
			int popupH = this.getPopupHeight();
			if (mx >= this.getX() && mx < this.getRight() && my >= popupY && my < popupY + popupH) {
				int relY = (int) my - (popupY + 1);
				int index = (relY + this.getScrollStart() * OPTION_HEIGHT) / OPTION_HEIGHT;
				if (index >= 0 && index < this.options.size()) {
					this.selectedIndex = index;
					this.open = false;
					this.playDownSound(MinecraftClient.getInstance().getSoundManager());
					if (this.onSelect != null) {
						this.onSelect.accept(index);
					}
				}
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isMouseOver(double mouseX, double mouseY) {
		if (this.isInteractable() && mouseX >= this.getX() && mouseX < this.getRight() && mouseY >= this.getY() && mouseY < this.getBottom()) {
			return true;
		}
		if (this.open) {
			int popupY = this.getPopupY();
			int popupH = this.getPopupHeight();
			return mouseX >= this.getX() && mouseX < this.getRight() && mouseY >= popupY && mouseY < popupY + popupH;
		}
		return false;
	}

	@Override
	protected void appendClickableNarrations(NarrationMessageBuilder builder) {
	}
}
