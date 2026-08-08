package mc506lw.mscpoServerlist.fabric;

import mc506lw.mscpoServerlist.client.config.LocalDataStore;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public class MscpoServerlistClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		Path configFile = FabricLoader.getInstance().getConfigDir().resolve("mscpo-serverlist.json");
		LocalDataStore.init(configFile);
	}
}
