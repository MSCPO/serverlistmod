package mc506lw.mscpoServerlist.neoforge;

import mc506lw.mscpoServerlist.client.config.LocalDataStore;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLPaths;

@Mod(value = "mscpo-serverlist", dist = Dist.CLIENT)
public class MscpoServerlist {

	public MscpoServerlist(IEventBus modBus) {
		modBus.addListener(this::onClientSetup);
	}

	private void onClientSetup(FMLClientSetupEvent event) {
		LocalDataStore.init(FMLPaths.CONFIGDIR.get().resolve("mscpo-serverlist.json"));
	}
}
