package mc506lw.mscpoServerlist.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import mc506lw.mscpoServerlist.client.api.MscpoServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class LocalDataStore {
	private static final Logger LOGGER = LoggerFactory.getLogger("MSCPO-Serverlist");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final ReentrantReadWriteLock LOCK = new ReentrantReadWriteLock();
	private static final int MAX_RECENTS = 30;

	private static Path file;
	private static String apiBaseUrl = "https://api.mscpo.com/api/serverlist";
	private static final Map<String, MscpoServer> favorites = new LinkedHashMap<>();
	private static final List<MscpoServer> recents = new ArrayList<>();

	private LocalDataStore() {
	}

	public static void init(java.nio.file.Path configFile) {
		file = configFile;
		load();
	}

	public static String apiBaseUrl() {
		LOCK.readLock().lock();
		try {
			return apiBaseUrl;
		} finally {
			LOCK.readLock().unlock();
		}
	}

	public static void setApiBaseUrl(String url) {
		LOCK.writeLock().lock();
		try {
			apiBaseUrl = url;
		} finally {
			LOCK.writeLock().unlock();
		}
		save();
	}

	public static List<MscpoServer> favorites() {
		LOCK.readLock().lock();
		try {
			return new ArrayList<>(favorites.values());
		} finally {
			LOCK.readLock().unlock();
		}
	}

	public static List<MscpoServer> recents() {
		LOCK.readLock().lock();
		try {
			return new ArrayList<>(recents);
		} finally {
			LOCK.readLock().unlock();
		}
	}

	public static boolean isFavorite(String id) {
		LOCK.readLock().lock();
		try {
			return id != null && favorites.containsKey(id);
		} finally {
			LOCK.readLock().unlock();
		}
	}

	public static void toggleFavorite(MscpoServer server) {
		if (server == null || server.id == null || server.id.isEmpty()) return;
		LOCK.writeLock().lock();
		try {
			if (favorites.remove(server.id) == null) {
				server.favoritedAt = System.currentTimeMillis();
				favorites.put(server.id, server);
			}
		} finally {
			LOCK.writeLock().unlock();
		}
		save();
	}

	public static void addRecent(MscpoServer server) {
		if (server == null || server.id == null || server.id.isEmpty()) return;
		LOCK.writeLock().lock();
		try {
			recents.removeIf(s -> s.id.equals(server.id));
			server.lastPlayedAt = System.currentTimeMillis();
			recents.add(0, server);
			while (recents.size() > MAX_RECENTS) {
				recents.remove(recents.size() - 1);
			}
		} finally {
			LOCK.writeLock().unlock();
		}
		save();
	}

	private static void load() {
		if (file == null || !Files.exists(file)) return;
		try {
			String json = Files.readString(file, StandardCharsets.UTF_8);
			Map<String, Object> root = GSON.fromJson(json, new TypeToken<Map<String, Object>>() {
			}.getType());
			if (root == null) return;
			LOCK.writeLock().lock();
			try {
				Object base = root.get("apiBaseUrl");
				if (base instanceof String s && !s.isBlank()) apiBaseUrl = s;

				favorites.clear();
				List<MscpoServer> favs = GSON.fromJson(GSON.toJson(root.get("favorites")), new TypeToken<List<MscpoServer>>() {
				}.getType());
				if (favs != null) {
					for (MscpoServer s : favs) {
						if (s.id != null && !s.id.isEmpty()) favorites.put(s.id, s);
					}
				}

				recents.clear();
				List<MscpoServer> rec = GSON.fromJson(GSON.toJson(root.get("recents")), new TypeToken<List<MscpoServer>>() {
				}.getType());
				if (rec != null) recents.addAll(rec);
			} finally {
				LOCK.writeLock().unlock();
			}
		} catch (Exception e) {
			LOGGER.error("Failed to load MSCPO serverlist data", e);
		}
	}

	private static void save() {
		if (file == null) return;
		LOCK.readLock().lock();
		try {
			Map<String, Object> root = new LinkedHashMap<>();
			root.put("apiBaseUrl", apiBaseUrl);
			root.put("favorites", new ArrayList<>(favorites.values()));
			root.put("recents", new ArrayList<>(recents));
			String json = GSON.toJson(root);
			Path dir = file.getParent();
			if (dir != null) Files.createDirectories(dir);
			Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
			Files.writeString(tmp, json, StandardCharsets.UTF_8);
			Files.move(tmp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			LOGGER.error("Failed to save MSCPO serverlist data", e);
		} finally {
			LOCK.readLock().unlock();
		}
	}
}
