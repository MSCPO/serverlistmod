package mc506lw.mscpoServerlist.client.api;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import mc506lw.mscpoServerlist.client.config.LocalDataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MscpoApiClient {
	private static final Logger LOGGER = LoggerFactory.getLogger("MSCPO-Serverlist");
	private static final Gson GSON = new Gson();
	private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
	private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool(runnable -> {
		Thread thread = new Thread(runnable, "MSCPO-API-Worker");
		thread.setDaemon(true);
		return thread;
	});

	private MscpoApiClient() {
	}

	public static CompletableFuture<ApiModels.ServerListResult> fetchServers(int page, int pageSize, String query, String category, String subCategory) {
		return CompletableFuture.supplyAsync(() -> {
			StringBuilder url = new StringBuilder(LocalDataStore.apiBaseUrl()).append("/servers");
			url.append("?page=").append(page).append("&pageSize=").append(pageSize);
			if (query != null && !query.isBlank()) {
				url.append("&q=").append(encode(query));
			}
			if (category != null && !category.isBlank()) {
				url.append("&category=").append(encode(category));
			}
			if (subCategory != null && !subCategory.isBlank()) {
				url.append("&subCategory=").append(encode(subCategory));
			}
			String body = get(url.toString());
			if (body == null) return new ApiModels.ServerListResult();
			try {
				return GSON.fromJson(body, ApiModels.ServerListResult.class);
			} catch (JsonParseException e) {
				LOGGER.error("Failed to parse server list response", e);
				return new ApiModels.ServerListResult();
			}
		}, EXECUTOR);
	}

	public static CompletableFuture<List<ApiModels.Category>> fetchCategories() {
		return CompletableFuture.supplyAsync(() -> {
			String body = get(LocalDataStore.apiBaseUrl() + "/categories");
			if (body == null) return List.of();
			try {
				return GSON.fromJson(body, new TypeToken<List<ApiModels.Category>>() {
				}.getType());
			} catch (JsonParseException e) {
				LOGGER.error("Failed to parse categories response", e);
				return List.of();
			}
		}, EXECUTOR);
	}

	private static String get(String url) {
		try {
			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(url))
				.timeout(Duration.ofSeconds(15))
				.header("Accept", "application/json")
				.header("User-Agent", "MSCPO-serverlist/1.0 (+https://github.com/MSCPO)")
				.GET()
				.build();
			HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
			if (response.statusCode() != 200) {
				LOGGER.error("API returned status {} for {}", response.statusCode(), url);
				return null;
			}
			return response.body();
		} catch (Exception e) {
			LOGGER.error("API request failed for {}", url, e);
			return null;
		}
	}

	private static String encode(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}
}
