package org.vinerdream.petsRevive.listeners;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.vinerdream.petsRevive.PetsRevive;
import org.vinerdream.petsRevive.utils.ItemUtils;

import java.util.Objects;
import java.util.UUID;

public class EntityListener implements Listener {
    private final PetsRevive plugin;

    public EntityListener(PetsRevive plugin) {
        this.plugin = plugin;
    }

    private boolean handleNameTagInteraction(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof LivingEntity entity)) {
            return false;
        }
        final ItemStack item = event.getPlayer().getInventory().getItemInMainHand();

        if (item.getType() == Material.NAME_TAG && item.hasItemMeta() && Objects.equals(plugin.getConfig().getString("resurrection-name-tag"), ItemUtils.getCustomName(item))) {

            String msg;
            if (plugin.getPetsManager().setOwnerUUID(entity, event.getPlayer().getUniqueId())) {
                msg = plugin.getConfig().getString("messages.bind-success");
            } else {
                msg = plugin.getConfig().getString("messages.bind-fail");
            }
            event.getPlayer().sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(Objects.requireNonNull(msg)));

            return true;
        }
        return false;
    }

    @EventHandler
    private void onDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        final UUID ownerId = plugin.getPetsManager().getOwnerUUID(entity);
        if (ownerId == null) return;
        if (plugin.getPetsManager().registerDeath(entity)) {
            event.setCancelled(true);
            for (Entity passenger : entity.getPassengers()) {
                entity.removePassenger(passenger);
            }
        }
    }

    @EventHandler
    private void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) {
            return;
        }
        if (plugin.getPetsManager().isManaged(entity)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onEntitiesLoad(EntitiesLoadEvent event) {
        for (Entity entity : event.getEntities()) {
            if (entity instanceof LivingEntity pet) {
                plugin.getPetsManager().handlePetLoad(pet);
            }
        }
    }

    @EventHandler
    private void onEntitiesUnload(EntitiesUnloadEvent event) {
        for (Entity entity : event.getEntities()) {
            if (entity instanceof LivingEntity pet) {
                plugin.getPetsManager().handlePetUnload(pet);
            }
        }
    }

    @EventHandler
    private void onInteractEntity(PlayerInteractEntityEvent event) {
        if (handleNameTagInteraction(event)) {
            event.setCancelled(true);
            return;
        }

        if (!(event.getRightClicked() instanceof LivingEntity entity)) {
            return;
        }
        final ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
        if (
                item.getType().getKey().toString().equals(
                        plugin.getConfig().getString("resurrection-item")
                )
        ) {
            if (plugin.getPetsManager().startResurrection(entity)) {
                if (!event.getPlayer().getGameMode().equals(GameMode.CREATIVE)) {
                    item.setAmount(item.getAmount() - 1);
                }
                event.setCancelled(true);
            }
        } else if (plugin.getPetsManager().isManaged(entity)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onEntityTeleport(EntityTeleportEvent event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) {
            return;
        }
        if (plugin.getPetsManager().isManaged(entity)) {
            event.setCancelled(true);
        }
    }
}