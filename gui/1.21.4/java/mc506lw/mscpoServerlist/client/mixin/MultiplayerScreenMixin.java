package mc506lw.mscpoServerlist.client.mixin;

import mc506lw.mscpoServerlist.client.gui.MscpoServerListPanel;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(JoinMultiplayerScreen.class)
public abstract class MultiplayerScreenMixin extends Screen {
	@Shadow
	protected ServerSelectionList serverSelectionList;

	@Shadow
	private HeaderAndFooterLayout layout;

	@Unique
	private MscpoServerListPanel mscpo$panel;

	@Unique
	private int mscpo$lastVanillaWidth = -1;

	public MultiplayerScreenMixin(Component title) {
		super(title);
	}

	@Inject(method = "init", at = @At("RETURN"))
	private void mscpo$init(CallbackInfo ci) {
		if (this.mscpo$panel == null) {
			this.mscpo$panel = new MscpoServerListPanel((JoinMultiplayerScreen) (Object) this, this.minecraft);
			this.mscpo$panel.setDrawerStateListener(this::mscpo$updateServerListWidth);
		}
		this.addRenderableWidget(this.mscpo$panel);
		this.mscpo$layoutPanel();
	}

	@Inject(method = "repositionElements", at = @At("RETURN"))
	private void mscpo$repositionElements(CallbackInfo ci) {
		this.mscpo$layoutPanel();
	}

	@Inject(method = "tick", at = @At("RETURN"))
	private void mscpo$tick(CallbackInfo ci) {
		if (this.mscpo$panel != null) {
			this.mscpo$panel.tick();
			this.mscpo$updateServerListWidth();
		}
	}

	@Unique
	private void mscpo$layoutPanel() {
		if (this.mscpo$panel == null || this.serverSelectionList == null) return;
		int headerHeight = this.layout.getHeaderHeight();
		int contentHeight = this.layout.getContentHeight();
		this.mscpo$panel.setDrawerMode(true);
		this.mscpo$lastVanillaWidth = -1;
		this.mscpo$updateServerListWidth();
		this.mscpo$panel.updatePanel(this.width, headerHeight, contentHeight);
	}

	@Unique
	private void mscpo$updateServerListWidth() {
		if (this.mscpo$panel == null || this.serverSelectionList == null) return;
		int headerHeight = this.layout.getHeaderHeight();
		int contentHeight = this.layout.getContentHeight();
		int leftWidth;
		if (this.minecraft.getWindow().getGuiScaledWidth() < MscpoServerListPanel.DRAWER_WIDTH_THRESHOLD) {
			leftWidth = this.mscpo$panel.isDrawerOpen() ? 0 : this.width - MscpoServerListPanel.DRAWER_TAB_WIDTH - 2;
		} else {
			leftWidth = Math.max(0, this.mscpo$panel.getX() - 2);
		}
		if (leftWidth != this.mscpo$lastVanillaWidth) {
			this.mscpo$lastVanillaWidth = leftWidth;
			this.serverSelectionList.updateSizeAndPosition(leftWidth, contentHeight, headerHeight);
		}
	}
}
