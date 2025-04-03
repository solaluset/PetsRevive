package org.vinerdream.petsRevive;

import com.artillexstudios.axapi.AxPlugin;
import com.artillexstudios.axapi.utils.featureflags.FeatureFlags;
import net.luckperms.api.LuckPerms;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.vinerdream.petsRevive.listeners.EntityListener;
import org.vinerdream.petsRevive.managers.PetsManager;

public final class PetsRevive extends AxPlugin {
    private LuckPerms luckPerms;
    private PetsManager petsManager;

    @Override
    public void enable() {
        saveDefaultConfig();

        final RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (provider != null) {
            luckPerms = provider.getProvider();
        } else {
            getLogger().warning("LuckPerms not found, group support disabled");
        }

        petsManager = new PetsManager(this);
        Bukkit.getPluginManager().registerEvents(new EntityListener(this), this);
    }

    @Override
    public void disable() {
        // Plugin shutdown logic
    }

    public void updateFlags(FeatureFlags flags) {
        flags.USE_LEGACY_HEX_FORMATTER.set(true);
        flags.PACKET_ENTITY_TRACKER_ENABLED.set(true);
        flags.HOLOGRAM_UPDATE_TICKS.set(20L);
    }

    public LuckPerms getLuckPerms() { return luckPerms; }

    public PetsManager getPetsManager() { return petsManager; }
}
