package org.vinerdream.petsRevive.listeners;

import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.vinerdream.petsRevive.PetsRevive;

import java.util.UUID;

public class EntityListener implements Listener {
    private final PetsRevive plugin;

    public EntityListener(PetsRevive plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    private void onDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Tameable pet)) return;
        final UUID ownerId = pet.getOwnerUniqueId();
        if (ownerId == null) return;
        if (plugin.getPetsManager().registerDeath(pet)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onEntitiesLoad(EntitiesLoadEvent event) {
        for (Entity entity : event.getEntities()) {
            if (entity instanceof Tameable pet) {
                plugin.getPetsManager().handlePetLoad(pet);
            }
        }
    }

    @EventHandler
    private void onEntitiesUnload(EntitiesUnloadEvent event) {
        for (Entity entity : event.getEntities()) {
            if (entity instanceof Tameable pet) {
                plugin.getPetsManager().handlePetUnload(pet);
            }
        }
    }

    @EventHandler
    private void onInteractEntity(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Tameable pet)) return;
        final ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
        if (
                item.getType().getKey().toString().equals(
                        plugin.getConfig().getString("resurrection-item")
                )
        ) {
            if (plugin.getPetsManager().startResurrection(pet) && !event.getPlayer().getGameMode().equals(GameMode.CREATIVE)) {
                item.setAmount(item.getAmount() - 1);
            }
        }
    }

    @EventHandler
    private void onEntityTeleport(EntityTeleportEvent event) {
        if (!(event.getEntity() instanceof Tameable pet)) return;
        if (plugin.getPetsManager().isManaged(pet)) {
            event.setCancelled(true);
        }
    }
}
