package mc506lw.mscpoServerlist.client;

import mc506lw.mscpoServerlist.client.config.LocalDataStore;
import net.fabricmc.api.ClientModInitializer;

public class MscpoServerlistClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        LocalDataStore.init();
    }
}
