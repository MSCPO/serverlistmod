package mc506lw.mscpoServerlist.client.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ApiModels {
	private ApiModels() {
	}

	public static final class Pagination {
		public int page;
		public int pageSize;
		public int total;
		public int totalPages;
	}

	public static final class ServerListResult {
		public List<ServerEntry> data;
		public Pagination pagination;

		public List<ServerEntry> servers() {
			return data != null ? data : Collections.emptyList();
		}

		public boolean hasError() {
			return data == null;
		}
	}

	public static final class ServerEntry {
		public String id;
		public String name;
		public String description;
		public String address;
		public List<String> javaVersion = new ArrayList<>();
		public List<String> bedrockVersion = new ArrayList<>();
		public String iconUrl;
		public String bannerUrl;
		public List<String> galleryUrls = new ArrayList<>();
		public Object links;
		public String status;
		public String ownerEmail;
		public List<ServerCategory> categories = new ArrayList<>();
		public List<ServerBadge> badges = new ArrayList<>();
	}

	public static final class ServerCategory {
		public String serverId;
		public String categoryId;
		public String subCategoryId;
		public CategoryNode category;
		public CategoryNode subCategory;
	}

	public static final class CategoryNode {
		public String id;
		public String name;
		public String categoryId;
	}

	public static final class ServerBadge {
		public String serverId;
		public String badgeId;
		public Badge badge;
	}

	public static final class Badge {
		public String id;
		public String name;
		public String description;
		public String iconUrl;
		public Object style;
	}

	public static final class Category {
		public String id;
		public String name;
		public List<SubCategory> subCategories = new ArrayList<>();
	}

	public static final class SubCategory {
		public String id;
		public String name;
		public String categoryId;
	}
}
