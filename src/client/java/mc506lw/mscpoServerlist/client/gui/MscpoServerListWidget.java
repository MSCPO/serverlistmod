package mc506lw.mscpoServerlist.client.gui;

import mc506lw.mscpoServerlist.client.api.MscpoServer;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.world.WorldIcon;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.network.MultiplayerServerListPinger;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.network.NetworkingBackend;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MscpoServerListWidget extends AlwaysSelectedEntryListWidget<MscpoServerListWidget.Entry> {
	private static final int ROW_HEIGHT = 36;
	private static final ExecutorService PING_POOL = Executors.newFixedThreadPool(8, runnable -> {
		Thread thread = new Thread(runnable, "MSCPO-Server-Ping");
		thread.setDaemon(true);
		return thread;
	});
	private static final Identifier PING_1_TEXTURE = Identifier.ofVanilla("server_list/ping_1");
	private static final Identifier PING_2_TEXTURE = Identifier.ofVanilla("server_list/ping_2");
	private static final Identifier PING_3_TEXTURE = Identifier.ofVanilla("server_list/ping_3");
	private static final Identifier PING_4_TEXTURE = Identifier.ofVanilla("server_list/ping_4");
	private static final Identifier PING_5_TEXTURE = Identifier.ofVanilla("server_list/ping_5");
	private static final Identifier PINGING_1_TEXTURE = Identifier.ofVanilla("server_list/pinging_1");
	private static final Identifier PINGING_2_TEXTURE = Identifier.ofVanilla("server_list/pinging_2");
	private static final Identifier PINGING_3_TEXTURE = Identifier.ofVanilla("server_list/pinging_3");
	private static final Identifier PINGING_4_TEXTURE = Identifier.ofVanilla("server_list/pinging_4");
	private static final Identifier PINGING_5_TEXTURE = Identifier.ofVanilla("server_list/pinging_5");
	private static final Identifier INCOMPATIBLE_TEXTURE = Identifier.ofVanilla("server_list/incompatible");
	private static final Identifier UNREACHABLE_TEXTURE = Identifier.ofVanilla("server_list/unreachable");
	private static final long PING_CACHE_TTL_MS = 5 * 60 * 1000L;
	private static final Map<String, CachedPing> PING_CACHE = new HashMap<>();

	private final MscpoServerListPanel panel;

	public MscpoServerListWidget(MscpoServerListPanel panel, MinecraftClient client, int width, int height, int y) {
		super(client, width, height, y, ROW_HEIGHT);
		this.panel = panel;
	}

	private static ServerInfo getPingInfo(MscpoServer server) {
		String name = server.name == null ? "" : server.name;
		String address = server.address == null ? "" : server.address;
		synchronized (PING_CACHE) {
			CachedPing cached = PING_CACHE.get(address);
			if (cached != null && System.currentTimeMillis() - cached.time < PING_CACHE_TTL_MS
				&& cached.info.getStatus() != ServerInfo.Status.INITIAL && cached.info.getStatus() != ServerInfo.Status.PINGING) {
				return cached.info;
			}
			ServerInfo info = new ServerInfo(name, address, ServerInfo.ServerType.OTHER);
			PING_CACHE.put(address, new CachedPing(info, System.currentTimeMillis()));
			return info;
		}
	}

	@Override
	public int getRowWidth() {
		return Math.max(200, Math.min(320, this.getWidth() - 10));
	}

	@Override
	protected int getScrollbarX() {
		return this.getRight() - 6;
	}

	@Override
	public void setSelected(@Nullable Entry entry) {
		super.setSelected(entry);
		this.panel.onSelectionChanged();
	}

	@Nullable
	public MscpoServer getSelectedServer() {
		Entry entry = this.getSelectedOrNull();
		return entry instanceof ServerEntry serverEntry ? serverEntry.server : null;
	}

	public void setServers(List<MscpoServer> servers) {
		List<Entry> entries = new ArrayList<>();
		for (MscpoServer server : servers) {
			entries.add(new ServerEntry(this.panel, server));
		}
		this.replaceEntries(entries);
		this.refreshScroll();
	}

	public void setStatus(Text text) {
		this.replaceEntries(List.of(new StatusEntry(text, null)));
	}

	public void setError(Text text, Runnable onClick) {
		this.replaceEntries(List.of(new StatusEntry(text, onClick)));
	}

	public abstract static class Entry extends AlwaysSelectedEntryListWidget.Entry<Entry> {
	}

	public static class ServerEntry extends Entry {
		private final MscpoServerListPanel panel;
		private final MscpoServer server;
		private final ServerInfo pingInfo;
		private final WorldIcon icon;
		private byte @Nullable [] shownFavicon;

		ServerEntry(MscpoServerListPanel panel, MscpoServer server) {
			this.panel = panel;
			this.server = server;
			this.pingInfo = getPingInfo(server);
			String address = server.address == null ? "" : server.address;
			this.icon = WorldIcon.forServer(MinecraftClient.getInstance().getTextureManager(), address);
		}

		@Override
		public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
			MinecraftClient client = MinecraftClient.getInstance();
			if (this.pingInfo.getStatus() == ServerInfo.Status.INITIAL) {
				this.pingInfo.setStatus(ServerInfo.Status.PINGING);
				this.pingInfo.label = ScreenTexts.EMPTY;
				this.pingInfo.playerCountLabel = ScreenTexts.EMPTY;
				ServerInfo target = this.pingInfo;
				MinecraftClient mc = client;
				MultiplayerServerListPinger pinger = this.panel.getScreen().getServerListPinger();
				PING_POOL.submit(() -> {
					try {
						pinger.add(
							target,
							() -> {
							},
							() -> {
								target.setStatus(
									target.protocolVersion == SharedConstants.getGameVersion().protocolVersion()
										? ServerInfo.Status.SUCCESSFUL
										: ServerInfo.Status.INCOMPATIBLE
								);
							},
							NetworkingBackend.remote(mc.options.shouldUseNativeTransport())
						);
					} catch (UnknownHostException unknownHostException) {
						target.setStatus(ServerInfo.Status.UNREACHABLE);
						target.label = Text.translatable("multiplayer.status.cannot_resolve");
					} catch (Exception exception) {
						target.setStatus(ServerInfo.Status.UNREACHABLE);
						target.label = Text.translatable("multiplayer.status.cannot_connect");
					}
				});
			}

			int x = this.getContentX();
			int y = this.getContentY();
			int contentW = this.getContentWidth();

			context.drawTexture(RenderPipelines.GUI_TEXTURED, this.icon.getTextureId(), x, y, 0.0F, 0.0F, 32, 32, 32, 32);
			String name = this.server.name != null ? this.server.name : "";
			context.drawTextWithShadow(client.textRenderer, name, x + 35, y + 1, 0xFFFFFFFF);

			Text star = this.panel.isFavorite(this.server.id)
				? Text.literal("★").formatted(Formatting.GOLD)
				: Text.literal("☆").formatted(Formatting.DARK_GRAY);
			context.drawTextWithShadow(client.textRenderer, star, x + 1, y + 1, 0xFFFFFFFF);

			int statusX = x + contentW - 15;
			Text count = this.pingInfo.playerCountLabel != null ? this.pingInfo.playerCountLabel : ScreenTexts.EMPTY;
			int countWidth = client.textRenderer.getWidth(count);
			int countX = statusX - countWidth - 4;
			context.drawTextWithShadow(client.textRenderer, count, countX, y + 1, 0xFF808080);

			Text label = this.pingInfo.label != null ? this.pingInfo.label : ScreenTexts.EMPTY;
			List<OrderedText> lines = client.textRenderer.wrapLines(label, Math.max(40, contentW - 35 - 10 - 55));
			for (int i = 0; i < Math.min(lines.size(), 2); i++) {
				context.drawTextWithShadow(client.textRenderer, lines.get(i), x + 35, y + 12 + 9 * i, -8355712);
			}

			Identifier statusIcon = this.getStatusIconTexture();
			if (statusIcon != null) {
				context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, statusIcon, statusX, y, 10, 8);
			}

			Text statusTooltip = this.getStatusTooltipText();
			if (statusTooltip != null && mouseX >= statusX && mouseX <= statusX + 10 && mouseY >= y && mouseY <= y + 8) {
				context.drawTooltip(statusTooltip, mouseX, mouseY);
			} else if (this.pingInfo.playerListSummary != null && !this.pingInfo.playerListSummary.isEmpty()
				&& mouseX >= countX && mouseX <= countX + countWidth && mouseY >= y && mouseY <= y + 9) {
				List<OrderedText> summary = new ArrayList<>();
				for (Text player : this.pingInfo.playerListSummary) {
					summary.add(player.asOrderedText());
				}
				context.drawTooltip(summary, mouseX, mouseY);
			}

			byte[] favicon = this.pingInfo.getFavicon();
			if (!Arrays.equals(favicon, this.shownFavicon)) {
				if (this.uploadFavicon(favicon)) {
					this.shownFavicon = favicon;
				} else {
					this.pingInfo.setFavicon(null);
				}
			}
		}

		@Nullable
		private Text getStatusTooltipText() {
			return switch (this.pingInfo.getStatus()) {
				case PINGING -> Text.translatable("multiplayer.status.pinging");
				case INCOMPATIBLE -> Text.translatable("multiplayer.status.incompatible");
				case UNREACHABLE -> Text.translatable("multiplayer.status.no_connection");
				case SUCCESSFUL -> Text.translatable("multiplayer.status.ping", this.pingInfo.ping);
				case INITIAL -> null;
			};
		}

		@Nullable
		private Identifier getStatusIconTexture() {
			return switch (this.pingInfo.getStatus()) {
				case INITIAL, SUCCESSFUL -> {
					long ping = this.pingInfo.ping;
					if (ping < 150L) yield PING_5_TEXTURE;
					if (ping < 300L) yield PING_4_TEXTURE;
					if (ping < 600L) yield PING_3_TEXTURE;
					if (ping < 1000L) yield PING_2_TEXTURE;
					yield PING_1_TEXTURE;
				}
				case PINGING -> {
					int j = (int) (Util.getMeasuringTimeMs() / 100L & 7L);
					if (j > 4) j = 8 - j;
					yield switch (j) {
						case 1 -> PINGING_2_TEXTURE;
						case 2 -> PINGING_3_TEXTURE;
						case 3 -> PINGING_4_TEXTURE;
						case 4 -> PINGING_5_TEXTURE;
						default -> PINGING_1_TEXTURE;
					};
				}
				case INCOMPATIBLE -> INCOMPATIBLE_TEXTURE;
				case UNREACHABLE -> UNREACHABLE_TEXTURE;
			};
		}

		private boolean uploadFavicon(byte @Nullable [] bytes) {
			if (bytes == null) {
				this.icon.destroy();
			} else {
				try {
					this.icon.load(NativeImage.read(bytes));
				} catch (Throwable throwable) {
					return false;
				}
			}
			return true;
		}

		@Override
		public boolean mouseClicked(Click click, boolean doubled) {
			if (doubled) {
				this.panel.joinServer(this.server);
				return true;
			}
			return super.mouseClicked(click, doubled);
		}

		@Override
		public Text getNarration() {
			return Text.literal(this.server.name + ", " + this.server.address);
		}
	}

	public static class StatusEntry extends Entry {
		private final Text text;
		@Nullable
		private final Runnable onClick;

		StatusEntry(Text text, @Nullable Runnable onClick) {
			this.text = text;
			this.onClick = onClick;
		}

		@Override
		public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
			MinecraftClient client = MinecraftClient.getInstance();
			context.drawTextWithShadow(client.textRenderer, this.text, this.getContentX() + 2, this.getContentY() + 12, 0xFFA0A0A0);
		}

		@Override
		public boolean mouseClicked(Click click, boolean doubled) {
			if (this.onClick != null) {
				this.onClick.run();
			}
			return true;
		}

		@Override
		public Text getNarration() {
			return this.text;
		}
	}

	private static final class CachedPing {
		final ServerInfo info;
		final long time;

		CachedPing(ServerInfo info, long time) {
			this.info = info;
			this.time = time;
		}
	}
}
