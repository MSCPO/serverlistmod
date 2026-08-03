package mc506lw.mscpoServerlist.client.api;

/**
 * Lightweight UI model shared between the API list, favorites and recently played.
 * Also used for local persistence of favorites / recents.
 */
public final class MscpoServer {
	public String id = "";
	public String name = "";
	public String description = "";
	public String address = "";
	public String iconUrl;
	public String category = "";
	public String subCategory = "";
	public String badge = "";
	public long favoritedAt;
	public long lastPlayedAt;

	public MscpoServer() {
	}

	public static MscpoServer from(ApiModels.ServerEntry entry) {
		MscpoServer server = new MscpoServer();
		if (entry.id != null) server.id = entry.id;
		if (entry.name != null) server.name = entry.name;
		if (entry.description != null) server.description = entry.description;
		if (entry.address != null) server.address = entry.address;
		server.iconUrl = entry.iconUrl;
		if (entry.categories != null && !entry.categories.isEmpty()) {
			ApiModels.ServerCategory cat = entry.categories.get(0);
			if (cat.category != null && cat.category.name != null) server.category = cat.category.name;
			if (cat.subCategory != null && cat.subCategory.name != null) server.subCategory = cat.subCategory.name;
		}
		if (entry.badges != null && !entry.badges.isEmpty()) {
			ApiModels.ServerBadge badge = entry.badges.get(0);
			if (badge.badge != null && badge.badge.name != null) server.badge = badge.badge.name;
		}
		return server;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof MscpoServer that)) return false;
		return id.equals(that.id);
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}
}
