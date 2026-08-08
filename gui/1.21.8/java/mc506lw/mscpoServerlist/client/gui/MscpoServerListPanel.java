package mc506lw.mscpoServerlist.client.gui;

import mc506lw.mscpoServerlist.client.api.ApiModels;
import mc506lw.mscpoServerlist.client.api.MscpoApiClient;
import mc506lw.mscpoServerlist.client.api.MscpoServer;
import mc506lw.mscpoServerlist.client.config.LocalDataStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractContainerWidget;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class MscpoServerListPanel extends AbstractContainerWidget {
	public static final int DRAWER_TAB_WIDTH = 54;
	public static final int DRAWER_WIDTH_THRESHOLD = 1280;
	private static final int PAGE_SIZE = 50;
	private static final int MARGIN = 4;
	private static final int TAB_ALL = 0;
	private static final int TAB_FAVORITE = 1;
	private static final int TAB_RECENT = 2;
	private static final int SEARCH_COOLDOWN = 6;

	private final Minecraft client;
	private final JoinMultiplayerScreen screen;
	private final List<AbstractWidget> children = new ArrayList<>();

	private final Button tabAll;
	private final Button tabFavorite;
	private final Button tabRecent;
	private final EditBox searchField;
	private final MscpoDropdown categoryDropdown;
	private final MscpoDropdown subCategoryDropdown;
	private final MscpoServerListWidget serverList;
	private final Button joinButton;
	private final Button favoriteButton;
	private boolean drawerMode;
	private boolean drawerOpen;
	private Runnable onDrawerStateChanged;

	private int currentTab = TAB_ALL;
	private final List<ApiModels.Category> categories = new ArrayList<>();
	private int categoryIndex = -1;
	private int subIndex = -1;
	private boolean categoriesLoaded;

	private final List<MscpoServer> apiServers = new ArrayList<>();
	private int currentPage = 1;
	private int totalPages = 1;
	private boolean loading;
	private boolean loadFailed;
	private String committedQuery = "";
	private String pendingQuery = "";
	private int searchCooldown;
	private long requestSerial;

	public MscpoServerListPanel(JoinMultiplayerScreen screen, Minecraft client) {
		super(0, 0, 300, 200, Component.translatable("mscpo.title"));
		this.screen = screen;
		this.client = client;

		this.tabAll = button(Component.translatable("mscpo.tab.all"), button -> this.selectTab(TAB_ALL));
		this.tabFavorite = button(Component.translatable("mscpo.tab.favorite"), button -> this.selectTab(TAB_FAVORITE));
		this.tabRecent = button(Component.translatable("mscpo.tab.recent"), button -> this.selectTab(TAB_RECENT));
		this.searchField = new EditBox(client.font, 0, 0, 200, 16, Component.translatable("mscpo.search"));
		this.searchField.setHint(Component.translatable("mscpo.search.placeholder"));
		this.searchField.setMaxLength(64);
		this.searchField.setResponder(text -> {
			this.pendingQuery = text == null ? "" : text;
			this.searchCooldown = SEARCH_COOLDOWN;
		});
		this.categoryDropdown = new MscpoDropdown(0, 0, 100, 16, this::onCategorySelected);
		this.categoryDropdown.setOptions(List.of(Component.translatable("mscpo.filter.all").getString()));
		this.categoryDropdown.active = false;
		this.subCategoryDropdown = new MscpoDropdown(0, 0, 100, 16, this::onSubCategorySelected);
		this.subCategoryDropdown.setOptions(List.of(Component.translatable("mscpo.filter.allSub").getString()));
		this.subCategoryDropdown.active = false;
		this.serverList = new MscpoServerListWidget(this, client, 200, 100, 0);
		this.joinButton = button(Component.translatable("mscpo.btn.join"), button -> {
			MscpoServer server = this.serverList.getSelectedServer();
			if (server != null) this.joinServer(server);
		});
		this.favoriteButton = button(Component.translatable("mscpo.btn.favorite"), button -> this.toggleFavorite());

		this.children.add(this.tabAll);
		this.children.add(this.tabFavorite);
		this.children.add(this.tabRecent);
		this.children.add(this.searchField);
		this.children.add(this.categoryDropdown);
		this.children.add(this.subCategoryDropdown);
		this.children.add(this.serverList);
		this.children.add(this.joinButton);
		this.children.add(this.favoriteButton);

		this.fetchCategories();
		this.reloadFromApi();
		this.updateTabButtons();
		this.updateButtons();
	}

	private Button button(Component text, Button.OnPress action) {
		return Button.builder(text, action).bounds(0, 0, 100, 20).build();
	}

	@Override
	public List<? extends GuiEventListener> children() {
		return this.children;
	}

	@Override
	protected void renderWidget(GuiGraphics context, int mouseX, int mouseY, float deltaTicks) {
		int x = this.getX();
		int y = this.getY();
		int w = this.getWidth();
		int h = this.getHeight();
		if (this.drawerMode && !this.drawerOpen) {
			context.fill(x, y, x + w, y + h, 0x66101010);
			context.fill(x, y, x + 1, y + h, 0xFF3C3C3C);
			this.drawTabStrip(context, x, y, w, h, "MSCPO ▸");
			return;
		}
		context.fill(x, y, x + w, y + h, 0x66101010);
		context.fill(x, y, x + 1, y + h, 0xFF3C3C3C);
		int contentX = x + MARGIN + (this.drawerMode ? DRAWER_TAB_WIDTH : 0);
		context.drawString(this.client.font, Component.translatable("mscpo.title"), contentX, y + 1, -1);
		for (AbstractWidget child : this.children) {
			if (child.visible) {
				child.render(context, mouseX, mouseY, deltaTicks);
			}
		}
		if (this.categoryDropdown.isOpen()) {
			this.categoryDropdown.render(context, mouseX, mouseY, deltaTicks);
		}
		if (this.subCategoryDropdown.isOpen()) {
			this.subCategoryDropdown.render(context, mouseX, mouseY, deltaTicks);
		}
		if (this.drawerMode) {
			this.drawTabStrip(context, x, y, w, h, this.drawerOpen ? "MSCPO ◂" : "MSCPO ▸");
		}
	}

	private void drawTabStrip(GuiGraphics context, int x, int y, int w, int h, String label) {
		context.fill(x, y, x + DRAWER_TAB_WIDTH, y + h, 0x99101010);
		context.fill(x + DRAWER_TAB_WIDTH - 1, y, x + DRAWER_TAB_WIDTH, y + h, 0xFF3C3C3C);
		context.drawCenteredString(this.client.font, label, x + DRAWER_TAB_WIDTH / 2, y + h / 2 - 4, -1);
	}

	public void tick() {
		if (this.searchCooldown > 0) {
			this.searchCooldown--;
			if (this.searchCooldown == 0) {
				this.commitSearch();
			}
		}
		if (this.currentTab == TAB_ALL && !this.loading && !this.loadFailed && this.currentPage < this.totalPages) {
			if (this.serverList.maxScrollAmount() - this.serverList.scrollAmount() < 60) {
				this.loadMoreFromApi();
			}
		}
		if (this.drawerMode) {
			int openW = this.getOpenWidth();
			if (this.getWidth() != openW) {
				this.setSize(openW, this.getHeight());
			}
			int target = this.drawerOpen ? this.getOpenX() : this.getClosedX();
			if (this.getX() != target) {
				int next = this.getX() + (target - this.getX()) / 3;
				if (Math.abs(target - next) < 2) next = target;
				this.setPosition(next, this.getY());
				this.layout();
			}
		}
	}

	public JoinMultiplayerScreen getScreen() {
		return this.screen;
	}

	public void setDrawerMode(boolean drawerMode) {
		if (this.drawerMode != drawerMode) {
			this.drawerMode = drawerMode;
			if (drawerMode) {
				this.drawerOpen = false;
			}
		}
	}

	public boolean isDrawerMode() {
		return this.drawerMode;
	}

	public boolean isSmallScreen() {
		return this.client.getWindow().getGuiScaledWidth() < DRAWER_WIDTH_THRESHOLD;
	}

	public int getOpenWidth() {
		return this.isSmallScreen() ? this.screen.width : Math.min(340, this.screen.width - 40);
	}

	public int getClosedX() {
		return this.screen.width - DRAWER_TAB_WIDTH;
	}

	public int getOpenX() {
		if (this.isSmallScreen()) return 0;
		return this.screen.width - this.getOpenWidth();
	}

	public void setDrawerOpen(boolean open) {
		this.drawerOpen = open;
		if (this.onDrawerStateChanged != null) {
			this.onDrawerStateChanged.run();
		}
	}

	public void setDrawerStateListener(Runnable listener) {
		this.onDrawerStateChanged = listener;
	}

	public boolean isDrawerOpen() {
		return this.drawerOpen;
	}

	public void updatePanel(int screenWidth, int headerHeight, int contentHeight) {
		int w = this.getOpenWidth();
		int x = this.drawerOpen ? this.getOpenX() : this.getClosedX();
		this.setSize(w, contentHeight);
		this.setPosition(x, headerHeight);
		this.layout();
	}

	public void layout() {
		if (this.drawerMode && !this.drawerOpen) return;
		int x = this.getX();
		int y = this.getY();
		int w = this.getWidth();
		int h = this.getHeight();
		int contentX = x + MARGIN + (this.drawerMode ? DRAWER_TAB_WIDTH : 0);
		int innerW = w - MARGIN - (this.drawerMode ? DRAWER_TAB_WIDTH : 0) - MARGIN;
		int btnHeight = 16;
		int third = (innerW - 6) / 3;
		int half = (innerW - 3) / 2;

		int yCursor = y + 12;
		this.tabAll.setSize(third, btnHeight);
		this.tabAll.setPosition(contentX, yCursor);
		this.tabFavorite.setSize(third, btnHeight);
		this.tabFavorite.setPosition(contentX + third + 3, yCursor);
		this.tabRecent.setSize(third, btnHeight);
		this.tabRecent.setPosition(contentX + third * 2 + 6, yCursor);

		yCursor += btnHeight + 4;
		this.searchField.setSize(innerW, btnHeight);
		this.searchField.setPosition(contentX, yCursor);

		yCursor += btnHeight + 4;
		this.categoryDropdown.setSize(half, btnHeight);
		this.categoryDropdown.setPosition(contentX, yCursor);
		this.subCategoryDropdown.setSize(half, btnHeight);
		this.subCategoryDropdown.setPosition(contentX + half + 3, yCursor);

		int listY = yCursor + btnHeight + 4;
		int listBottom = y + h - 26;
		int listH = Math.max(24, listBottom - listY);
		this.serverList.setSize(innerW, listH);
		this.serverList.setPosition(contentX, listY);

		int btnY = y + h - 22;
		this.joinButton.setSize(half, 20);
		this.joinButton.setPosition(contentX, btnY);
		this.favoriteButton.setSize(half, 20);
		this.favoriteButton.setPosition(contentX + half + 3, btnY);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (this.drawerMode) {
			boolean inTab = mouseX >= this.getX() && mouseX < this.getX() + DRAWER_TAB_WIDTH;
			if (inTab) {
				this.setDrawerOpen(!this.drawerOpen);
				this.playDownSound(Minecraft.getInstance().getSoundManager());
				return true;
			}
			if (!this.drawerOpen) return false;
		}
		if (this.categoryDropdown.isOpen() && !this.categoryDropdown.isMouseOver(mouseX, mouseY)) {
			this.categoryDropdown.close();
		}
		if (this.subCategoryDropdown.isOpen() && !this.subCategoryDropdown.isMouseOver(mouseX, mouseY)) {
			this.subCategoryDropdown.close();
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		for (int i = this.children.size() - 1; i >= 0; i--) {
			GuiEventListener child = this.children.get(i);
			if (child.isMouseOver(mouseX, mouseY) && child.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput builder) {
	}

	@Override
	protected double scrollRate() {
		return 0.0;
	}

	@Override
	protected int contentHeight() {
		return 0;
	}

	// ---------------------------------------------------------------- data

	public void onSelectionChanged() {
		this.updateButtons();
	}

	public boolean isFavorite(String id) {
		return LocalDataStore.isFavorite(id);
	}

	public void joinServer(MscpoServer server) {
		if (server == null || server.address == null || server.address.isEmpty()) return;
		LocalDataStore.addRecent(server);
		ServerData info = new ServerData(server.name, server.address, ServerData.Type.OTHER);
		ConnectScreen.startConnecting(this.screen, this.client, ServerAddress.parseString(server.address), info, false, null);
	}

	private void toggleFavorite() {
		MscpoServer server = this.serverList.getSelectedServer();
		if (server == null) return;
		LocalDataStore.toggleFavorite(server);
		if (this.currentTab == TAB_FAVORITE) {
			this.refreshLocalTab();
		} else {
			this.updateButtons();
		}
	}

	private void updateButtons() {
		MscpoServer server = this.serverList.getSelectedServer();
		this.joinButton.active = server != null;
		this.favoriteButton.active = server != null;
		if (server == null) {
			this.favoriteButton.setMessage(Component.translatable("mscpo.btn.favorite"));
			return;
		}
		this.favoriteButton.setMessage(Component.translatable(LocalDataStore.isFavorite(server.id) ? "mscpo.btn.unfavorite" : "mscpo.btn.favorite"));
	}

	private void selectTab(int tab) {
		this.currentTab = tab;
		this.updateTabButtons();
		this.committedQuery = "";
		this.searchField.setValue("");
		this.pendingQuery = "";
		this.serverList.setScrollAmount(0.0);
		if (tab == TAB_ALL) {
			if (this.apiServers.isEmpty()) {
				this.reloadFromApi();
			} else {
				this.serverList.setServers(this.apiServers);
			}
		} else {
			this.refreshLocalTab();
		}
	}

	private void refreshLocalTab() {
		List<MscpoServer> source = this.currentTab == TAB_FAVORITE ? LocalDataStore.favorites() : LocalDataStore.recents();
		List<MscpoServer> filtered = this.filterLocal(source);
		this.serverList.setServers(filtered);
		if (filtered.isEmpty()) {
			this.serverList.setStatus(Component.translatable(this.currentTab == TAB_FAVORITE ? "mscpo.status.noFavorite" : "mscpo.status.noRecent"));
		}
		this.updateButtons();
	}

	private List<MscpoServer> filterLocal(List<MscpoServer> source) {
		String query = this.committedQuery.trim().toLowerCase();
		if (query.isEmpty()) return new ArrayList<>(source);
		List<MscpoServer> result = new ArrayList<>();
		for (MscpoServer server : source) {
			if ((server.name != null && server.name.toLowerCase().contains(query))
				|| (server.address != null && server.address.toLowerCase().contains(query))
				|| (server.description != null && server.description.toLowerCase().contains(query))) {
				result.add(server);
			}
		}
		return result;
	}

	private void commitSearch() {
		String query = this.pendingQuery;
		if (query.equals(this.committedQuery)) return;
		this.committedQuery = query;
		if (this.currentTab != TAB_ALL) {
			this.refreshLocalTab();
			return;
		}
		this.reloadFromApi();
	}

	private void onCategorySelected(int index) {
		this.categoryIndex = index - 1;
		this.subIndex = -1;
		this.updateSubCategoryDropdown();
		this.reloadFromApi();
	}

	private void onSubCategorySelected(int index) {
		this.subIndex = index - 1;
		this.reloadFromApi();
	}

	private void updateSubCategoryDropdown() {
		List<String> options = new ArrayList<>();
		options.add(Component.translatable("mscpo.filter.allSub").getString());
		ApiModels.Category category = this.categoryIndex >= 0 && this.categoryIndex < this.categories.size() ? this.categories.get(this.categoryIndex) : null;
		if (category != null && category.subCategories != null) {
			for (ApiModels.SubCategory sub : category.subCategories) {
				options.add(sub.name);
			}
		}
		this.subCategoryDropdown.setOptions(options);
		this.subCategoryDropdown.setSelected(this.subIndex + 1);
		this.subCategoryDropdown.active = category != null && options.size() > 1;
	}

	private void updateTabButtons() {
		this.tabAll.active = this.currentTab != TAB_ALL;
		this.tabFavorite.active = this.currentTab != TAB_FAVORITE;
		this.tabRecent.active = this.currentTab != TAB_RECENT;
	}

	private String selectedCategoryName() {
		if (this.categoryIndex < 0 || this.categoryIndex >= this.categories.size()) return null;
		return this.categories.get(this.categoryIndex).name;
	}

	private String selectedSubName() {
		if (this.categoryIndex < 0 || this.subIndex < 0) return null;
		ApiModels.Category category = this.categories.get(this.categoryIndex);
		if (category.subCategories == null || this.subIndex >= category.subCategories.size()) return null;
		return category.subCategories.get(this.subIndex).name;
	}

	private void fetchCategories() {
		MscpoApiClient.fetchCategories().whenComplete((result, err) -> this.client.execute(() -> {
			if (result != null) {
				this.categories.clear();
				this.categories.addAll(result);
				this.categoriesLoaded = true;
				List<String> options = new ArrayList<>();
				options.add(Component.translatable("mscpo.filter.all").getString());
				for (ApiModels.Category category : this.categories) {
					options.add(category.name);
				}
				this.categoryDropdown.setOptions(options);
				this.categoryDropdown.setSelected(this.categoryIndex + 1);
				this.categoryDropdown.active = true;
				this.updateSubCategoryDropdown();
			}
		}));
	}

	private void reloadFromApi() {
		this.currentPage = 1;
		this.apiServers.clear();
		this.loading = true;
		this.loadFailed = false;
		this.serverList.setScrollAmount(0.0);
		this.serverList.setStatus(Component.translatable("mscpo.status.loading"));
		long serial = ++this.requestSerial;
		String query = this.committedQuery.isBlank() ? null : this.committedQuery;
		MscpoApiClient.fetchServers(1, PAGE_SIZE, query, this.selectedCategoryName(), this.selectedSubName())
			.whenComplete((result, err) -> this.client.execute(() -> {
				if (serial != this.requestSerial) return;
				this.loading = false;
				if (err != null || result == null || result.hasError()) {
					this.loadFailed = true;
					this.serverList.setError(Component.translatable("mscpo.status.error"), this::reloadFromApi);
					return;
				}
				this.loadFailed = false;
				this.currentPage = 1;
				this.totalPages = result.pagination != null ? Math.max(1, result.pagination.totalPages) : 1;
				for (ApiModels.ServerEntry entry : result.servers()) {
					this.apiServers.add(MscpoServer.from(entry));
				}
				this.serverList.setServers(this.apiServers);
				if (this.apiServers.isEmpty()) {
					this.serverList.setStatus(Component.translatable("mscpo.status.empty"));
				}
				this.updateButtons();
			}));
	}

	private void loadMoreFromApi() {
		if (this.loading || this.loadFailed || this.currentPage >= this.totalPages) return;
		this.loading = true;
		int page = this.currentPage + 1;
		long serial = ++this.requestSerial;
		String query = this.committedQuery.isBlank() ? null : this.committedQuery;
		MscpoApiClient.fetchServers(page, PAGE_SIZE, query, this.selectedCategoryName(), this.selectedSubName())
			.whenComplete((result, err) -> this.client.execute(() -> {
				if (serial != this.requestSerial) return;
				this.loading = false;
				if (err != null || result == null || result.hasError()) return;
				this.currentPage = page;
				this.totalPages = result.pagination != null ? Math.max(1, result.pagination.totalPages) : 1;
				for (ApiModels.ServerEntry entry : result.servers()) {
					this.apiServers.add(MscpoServer.from(entry));
				}
				this.serverList.setServers(this.apiServers);
				this.updateButtons();
			}));
	}
}
