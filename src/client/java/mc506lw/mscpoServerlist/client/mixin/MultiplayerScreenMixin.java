package mc506lw.mscpoServerlist.client.mixin;

import mc506lw.mscpoServerlist.client.gui.MscpoServerListPanel;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget;
import net.minecraft.client.gui.widget.ThreePartsLayoutWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiplayerScreen.class)
public abstract class MultiplayerScreenMixin extends Screen {
	@Shadow
	protected MultiplayerServerListWidget serverListWidget;

	@Shadow
	private ThreePartsLayoutWidget layout;

	@Unique
	private MscpoServerListPanel mscpo$panel;

	@Unique
	private int mscpo$lastVanillaWidth = -1;

	public MultiplayerScreenMixin(Text title) {
		super(title);
	}

	@Inject(method = "init", at = @At("RETURN"))
	private void mscpo$init(CallbackInfo ci) {
		if (this.mscpo$panel == null) {
			this.mscpo$panel = new MscpoServerListPanel((MultiplayerScreen) (Object) this, this.client);
			this.mscpo$panel.setDrawerStateListener(this::mscpo$updateServerListWidth);
		}
		this.addDrawableChild(this.mscpo$panel);
		this.mscpo$layoutPanel();
	}

	@Inject(method = "refreshWidgetPositions", at = @At("RETURN"))
	private void mscpo$refreshWidgetPositions(CallbackInfo ci) {
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
		if (this.mscpo$panel == null || this.serverListWidget == null) return;
		int headerHeight = this.layout.getHeaderHeight();
		int contentHeight = this.layout.getContentHeight();
		this.mscpo$panel.setDrawerMode(true);
		this.mscpo$lastVanillaWidth = -1;
		this.mscpo$updateServerListWidth();
		this.mscpo$panel.updatePanel(this.width, headerHeight, contentHeight);
	}

	@Unique
	private void mscpo$updateServerListWidth() {
		if (this.mscpo$panel == null || this.serverListWidget == null) return;
		int headerHeight = this.layout.getHeaderHeight();
		int contentHeight = this.layout.getContentHeight();
		int leftWidth;
		if (this.client.getWindow().getWidth() < MscpoServerListPanel.DRAWER_WIDTH_THRESHOLD) {
			leftWidth = this.mscpo$panel.isDrawerOpen() ? 0 : this.width - MscpoServerListPanel.DRAWER_TAB_WIDTH - 2;
		} else {
			leftWidth = Math.max(0, this.mscpo$panel.getX() - 2);
		}
		if (leftWidth != this.mscpo$lastVanillaWidth) {
			this.mscpo$lastVanillaWidth = leftWidth;
			this.serverListWidget.position(leftWidth, contentHeight, headerHeight);
		}
	}
}
