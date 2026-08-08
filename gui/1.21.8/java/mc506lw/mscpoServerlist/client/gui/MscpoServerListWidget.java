package mc506lw.mscpoServerlist.client.gui;

import com.mojang.blaze3d.platform.NativeImage;
import mc506lw.mscpoServerlist.client.api.MscpoServer;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.FaviconTexture;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerStatusPinger;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.Util;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MscpoServerListWidget extends ObjectSelectionList<MscpoServerListWidget.Entry> {
	private static final int ROW_HEIGHT = 36;
	private static final ExecutorService PING_POOL = Executors.newFixedThreadPool(8, runnable -> {
		Thread thread = new Thread(runnable, "MSCPO-Server-Ping");
		thread.setDaemon(true);
		return thread;
	});
	private static final ResourceLocation PING_1_TEXTURE = ResourceLocation.withDefaultNamespace("server_list/ping_1");
	private static final ResourceLocation PING_2_TEXTURE = ResourceLocation.withDefaultNamespace("server_list/ping_2");
	private static final ResourceLocation PING_3_TEXTURE = ResourceLocation.withDefaultNamespace("server_list/ping_3");
	private static final ResourceLocation PING_4_TEXTURE = ResourceLocation.withDefaultNamespace("server_list/ping_4");
	private static final ResourceLocation PING_5_TEXTURE = ResourceLocation.withDefaultNamespace("server_list/ping_5");
	private static final ResourceLocation PINGING_1_TEXTURE = ResourceLocation.withDefaultNamespace("server_list/pinging_1");
	private static final ResourceLocation PINGING_2_TEXTURE = ResourceLocation.withDefaultNamespace("server_list/pinging_2");
	private static final ResourceLocation PINGING_3_TEXTURE = ResourceLocation.withDefaultNamespace("server_list/pinging_3");
	private static final ResourceLocation PINGING_4_TEXTURE = ResourceLocation.withDefaultNamespace("server_list/pinging_4");
	private static final ResourceLocation PINGING_5_TEXTURE = ResourceLocation.withDefaultNamespace("server_list/pinging_5");
	private static final ResourceLocation INCOMPATIBLE_TEXTURE = ResourceLocation.withDefaultNamespace("server_list/incompatible");
	private static final ResourceLocation UNREACHABLE_TEXTURE = ResourceLocation.withDefaultNamespace("server_list/unreachable");
	private static final long PING_CACHE_TTL_MS = 5 * 60 * 1000L;
	private static final Map<String, CachedPing> PING_CACHE = new HashMap<>();

	private final MscpoServerListPanel panel;

	public MscpoServerListWidget(MscpoServerListPanel panel, Minecraft client, int width, int height, int y) {
		super(client, width, height, y, ROW_HEIGHT);
		this.panel = panel;
	}

	private static ServerData getPingInfo(MscpoServer server) {
		String name = server.name == null ? "" : server.name;
		String address = server.address == null ? "" : server.address;
		synchronized (PING_CACHE) {
			CachedPing cached = PING_CACHE.get(address);
			if (cached != null && System.currentTimeMillis() - cached.time < PING_CACHE_TTL_MS
				&& cached.info.state() != ServerData.State.INITIAL && cached.info.state() != ServerData.State.PINGING) {
				return cached.info;
			}
			ServerData info = new ServerData(name, address, ServerData.Type.OTHER);
			PING_CACHE.put(address, new CachedPing(info, System.currentTimeMillis()));
			return info;
		}
	}

	@Override
	public int getRowWidth() {
		return Math.max(200, Math.min(320, this.getWidth() - 10));
	}

	@Override
	protected int scrollBarX() {
		return this.getRight() - 6;
	}

	@Override
	public void setSelected(Entry entry) {
		super.setSelected(entry);
		this.panel.onSelectionChanged();
	}

	public MscpoServer getSelectedServer() {
		Entry entry = this.getSelected();
		return entry instanceof ServerEntry serverEntry ? serverEntry.server : null;
	}

	public void setServers(List<MscpoServer> servers) {
		List<Entry> entries = new ArrayList<>();
		for (MscpoServer server : servers) {
			entries.add(new ServerEntry(this.panel, server));
		}
		this.replaceEntries(entries);
		this.refreshScrollAmount();
	}

	public void setStatus(Component text) {
		this.replaceEntries(List.of(new StatusEntry(text, null)));
	}

	public void setError(Component text, Runnable onClick) {
		this.replaceEntries(List.of(new StatusEntry(text, onClick)));
	}

	public abstract static class Entry extends ObjectSelectionList.Entry<Entry> {
	}

	public static class ServerEntry extends Entry {
		private final MscpoServerListPanel panel;
		private final MscpoServer server;
		private final ServerData pingInfo;
		private final FaviconTexture icon;
		private byte[] shownFavicon;
		private long lastClickTime;

		ServerEntry(MscpoServerListPanel panel, MscpoServer server) {
			this.panel = panel;
			this.server = server;
			this.pingInfo = getPingInfo(server);
			String address = server.address == null ? "" : server.address;
			this.icon = FaviconTexture.forServer(Minecraft.getInstance().getTextureManager(), address);
		}

		@Override
		public void render(GuiGraphics context, int index, int rowTop, int rowLeft, int rowWidth, int rowHeight, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
			Minecraft client = Minecraft.getInstance();
			if (this.pingInfo.state() == ServerData.State.INITIAL) {
				this.pingInfo.setState(ServerData.State.PINGING);
				this.pingInfo.motd = CommonComponents.EMPTY;
				this.pingInfo.status = CommonComponents.EMPTY;
				ServerData target = this.pingInfo;
				Minecraft mc = client;
				ServerStatusPinger pinger = this.panel.getScreen().getPinger();
				PING_POOL.submit(() -> {
					try {
						pinger.pingServer(
							target,
							() -> {
							},
							() -> mc.execute(() -> target.setState(
								target.protocol == SharedConstants.getCurrentVersion().protocolVersion()
									? ServerData.State.SUCCESSFUL
									: ServerData.State.INCOMPATIBLE
							))
						);
					} catch (UnknownHostException unknownHostException) {
						target.setState(ServerData.State.UNREACHABLE);
						target.motd = Component.translatable("multiplayer.status.cannot_resolve");
					} catch (Exception exception) {
						target.setState(ServerData.State.UNREACHABLE);
						target.motd = Component.translatable("multiplayer.status.cannot_connect");
					}
				});
			}

			int x = rowLeft;
			int y = rowTop;
			int contentW = rowWidth;

			context.blit(RenderPipelines.GUI_TEXTURED, this.icon.textureLocation(), x, y, 0.0F, 0.0F, 32, 32, 32, 32);
			String name = this.server.name != null ? this.server.name : "";
			context.drawString(client.font, name, x + 35, y + 1, -1);

			Component star = this.panel.isFavorite(this.server.id)
				? Component.literal("★").withStyle(ChatFormatting.GOLD)
				: Component.literal("☆").withStyle(ChatFormatting.DARK_GRAY);
			context.drawString(client.font, star, x + 1, y + 1, -1);

			int statusX = x + contentW - 15;
			Component count = this.pingInfo.status != null ? this.pingInfo.status : CommonComponents.EMPTY;
			int countWidth = client.font.width(count);
			int countX = statusX - countWidth - 4;
			context.drawString(client.font, count, countX, y + 1, 0xFF808080);

			Component label = this.pingInfo.motd != null ? this.pingInfo.motd : CommonComponents.EMPTY;
			List<FormattedCharSequence> lines = client.font.split(label, Math.max(40, contentW - 35 - 10 - 55));
			for (int i = 0; i < Math.min(lines.size(), 2); i++) {
				context.drawString(client.font, lines.get(i), x + 35, y + 12 + 9 * i, -8355712);
			}

			ResourceLocation statusIcon = this.getStatusIconTexture();
			if (statusIcon != null) {
				context.blitSprite(RenderPipelines.GUI_TEXTURED, statusIcon, statusX, y, 10, 8);
			}

			Component statusTooltip = this.getStatusTooltipText();
			if (statusTooltip != null && mouseX >= statusX && mouseX <= statusX + 10 && mouseY >= y && mouseY <= y + 8) {
				context.setTooltipForNextFrame(statusTooltip, mouseX, mouseY);
			} else if (this.pingInfo.playerList != null && !this.pingInfo.playerList.isEmpty()
				&& mouseX >= countX && mouseX <= countX + countWidth && mouseY >= y && mouseY <= y + 9) {
				List<FormattedCharSequence> summary = new ArrayList<>();
				for (Component player : this.pingInfo.playerList) {
					summary.add(player.getVisualOrderText());
				}
				context.setTooltipForNextFrame(summary, mouseX, mouseY);
			}

			byte[] favicon = this.pingInfo.getIconBytes();
			if (!Arrays.equals(favicon, this.shownFavicon)) {
				if (this.uploadFavicon(favicon)) {
					this.shownFavicon = favicon;
				} else {
					this.pingInfo.setIconBytes(null);
				}
			}
		}

		private Component getStatusTooltipText() {
			return switch (this.pingInfo.state()) {
				case PINGING -> Component.translatable("multiplayer.status.pinging");
				case INCOMPATIBLE -> Component.translatable("multiplayer.status.incompatible");
				case UNREACHABLE -> Component.translatable("multiplayer.status.no_connection");
				case SUCCESSFUL -> Component.translatable("multiplayer.status.ping", this.pingInfo.ping);
				case INITIAL -> null;
			};
		}

		private ResourceLocation getStatusIconTexture() {
			return switch (this.pingInfo.state()) {
				case INITIAL, SUCCESSFUL -> {
					long ping = this.pingInfo.ping;
					if (ping < 150L) yield PING_5_TEXTURE;
					if (ping < 300L) yield PING_4_TEXTURE;
					if (ping < 600L) yield PING_3_TEXTURE;
					if (ping < 1000L) yield PING_2_TEXTURE;
					yield PING_1_TEXTURE;
				}
				case PINGING -> {
					int j = (int) (Util.getMillis() / 100L & 7L);
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

		private boolean uploadFavicon(byte[] bytes) {
			if (bytes == null) {
				this.icon.clear();
			} else {
				try {
					this.icon.upload(NativeImage.read(bytes));
				} catch (Throwable throwable) {
					return false;
				}
			}
			return true;
		}

		@Override
		public boolean mouseClicked(double mouseX, double mouseY, int button) {
			if (Util.getMillis() - this.lastClickTime < 250L) {
				this.panel.joinServer(this.server);
			}
			this.lastClickTime = Util.getMillis();
			return super.mouseClicked(mouseX, mouseY, button);
		}

		@Override
		public Component getNarration() {
			return Component.literal(this.server.name + ", " + this.server.address);
		}
	}

	public static class StatusEntry extends Entry {
		private final Component text;
		private final Runnable onClick;

		StatusEntry(Component text, Runnable onClick) {
			this.text = text;
			this.onClick = onClick;
		}

		@Override
		public void render(GuiGraphics context, int index, int rowTop, int rowLeft, int rowWidth, int rowHeight, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
			Minecraft client = Minecraft.getInstance();
			context.drawString(client.font, this.text, rowLeft + 2, rowTop + 12, 0xFFA0A0A0);
		}

		@Override
		public boolean mouseClicked(double mouseX, double mouseY, int button) {
			if (this.onClick != null) {
				this.onClick.run();
			}
			return true;
		}

		@Override
		public Component getNarration() {
			return this.text;
		}
	}

	private static final class CachedPing {
		final ServerData info;
		final long time;

		CachedPing(ServerData info, long time) {
			this.info = info;
			this.time = time;
		}
	}
}
