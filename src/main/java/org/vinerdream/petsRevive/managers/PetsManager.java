package org.vinerdream.petsRevive.managers;

import com.artillexstudios.axapi.hologram.Hologram;
import com.artillexstudios.axapi.hologram.HologramLine;
import com.artillexstudios.axapi.utils.placeholder.Placeholder;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.InheritanceNode;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Tameable;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.vinerdream.petsRevive.PetsRevive;
import org.vinerdream.petsRevive.data.PendingPetData;
import org.vinerdream.petsRevive.utils.TimeUtils;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.vinerdream.petsRevive.utils.TimeUtils.getCurrentSecond;

public class PetsManager {
    private final PetsRevive plugin;
    private final NamespacedKey deathTimestampKey;
    private final NamespacedKey resurrectionTimestampKey;
    private final NamespacedKey ownerKey;
    private final List<Map.Entry<String, Integer>> deathTimes = new ArrayList<>();
    private final List<Map.Entry<String, Integer>> resurrectionTimes = new ArrayList<>();
    private final Map<LivingEntity, PendingPetData> pendingDeadPets = new HashMap<>();
    private final Map<LivingEntity, PendingPetData> pendingResurrectingPets = new HashMap<>();

    public PetsManager(PetsRevive plugin) {
        this.plugin = plugin;
        this.deathTimestampKey = new NamespacedKey(plugin, "death-timestamp");
        this.resurrectionTimestampKey = new NamespacedKey(plugin, "resurrection-timestamp");
        this.ownerKey = new NamespacedKey(plugin, "owner");

        loadTimes("death-times", deathTimes, true);
        loadTimes("resurrection-times", resurrectionTimes, false);
    }

    public UUID getOwnerUUID(LivingEntity entity) {
        if (entity instanceof Tameable pet) {
            return pet.getOwnerUniqueId();
        }
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        String uuid = pdc.get(ownerKey, PersistentDataType.STRING);
        if (uuid == null) {
            return null;
        }
        return UUID.fromString(uuid);


    }

    public boolean setOwnerUUID(LivingEntity entity, UUID uuid) {
        if (entity instanceof Tameable) {
            return false;
        }
        if (getOwnerUUID(entity) != null) {
            return false;
        }
        if (plugin.getConfig().getStringList("mob-blacklist").contains(entity.getType().getKey().toString())) {
            return false;
        }
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        pdc.set(ownerKey, PersistentDataType.STRING, uuid.toString());
        return true;

    }

    public boolean registerDeath(LivingEntity pet) {
        if (pet.getPersistentDataContainer().has(deathTimestampKey)) {
            return false;
        }
        pet.setAI(false);
        pet.setSilent(true);
        pet.setInvulnerable(true);

        getTime(getOwnerUUID(pet), deathTimes).thenApply(
                deathTime -> {
                    final long endTime = getCurrentSecond() + deathTime;
                    pet.getPersistentDataContainer().set(deathTimestampKey, PersistentDataType.LONG, endTime);
                    pendingDeadPets.put(pet, new PendingPetData(
                            createHologram(pet, "messages.will-die", endTime),
                            Bukkit.getScheduler().runTaskLater(plugin, () -> kill(pet), deathTime * 20)
                    ));
                    return null;
                }
        );

        return true;
    }

    private void resurrect(LivingEntity pet) {
        if (!pet.getPersistentDataContainer().has(resurrectionTimestampKey)) return;

        pet.setAI(true);
        pet.setSilent(false);
        pet.setInvulnerable(false);
        pet.getPersistentDataContainer().remove(resurrectionTimestampKey);

        final PendingPetData data = pendingResurrectingPets.remove(pet);
        if (data != null) {
            data.hologram().remove();
        }
    }

    private void kill(LivingEntity pet) {
        if (!pet.getPersistentDataContainer().has(deathTimestampKey)) return;
        pet.setHealth(0);

        final PendingPetData data = pendingDeadPets.remove(pet);
        if (data != null) {
            data.hologram().remove();
        }
    }

    public void handlePetUnload(LivingEntity pet) {
        if (pendingDeadPets.containsKey(pet)) {
            final PendingPetData data = pendingDeadPets.remove(pet);
            data.hologram().remove();
            data.task().cancel();
        }
        if (pendingResurrectingPets.containsKey(pet)) {
            final PendingPetData data = pendingResurrectingPets.remove(pet);
            data.hologram().remove();
            data.task().cancel();
        }
    }

    public void handlePetLoad(LivingEntity pet) {
        final Long deathTimestamp = pet.getPersistentDataContainer().get(deathTimestampKey, PersistentDataType.LONG);
        if (deathTimestamp != null) {
            final long timeLeft = deathTimestamp - getCurrentSecond();
            if (timeLeft <= 0) {
                kill(pet);
            } else {
                pendingDeadPets.put(pet, new PendingPetData(
                        createHologram(pet, "messages.will-die", deathTimestamp),
                        Bukkit.getScheduler().runTaskLater(plugin, () -> kill(pet), timeLeft * 20)
                ));
            }
        }

        final Long resurrectionTimestamp = pet.getPersistentDataContainer().get(resurrectionTimestampKey, PersistentDataType.LONG);
        if (resurrectionTimestamp != null) {
            final long timeLeft = resurrectionTimestamp - getCurrentSecond();
            if (timeLeft <= 0) {
                resurrect(pet);
            } else {
                pendingResurrectingPets.put(pet, new PendingPetData(
                        createHologram(pet, "messages.will-resurrect", resurrectionTimestamp),
                        Bukkit.getScheduler().runTaskLater(plugin, () -> resurrect(pet), timeLeft * 20)
                ));
            }
        }
    }

    public boolean startResurrection(LivingEntity pet) {
        final PendingPetData deathData = pendingDeadPets.remove(pet);
        if (deathData == null) return false;
        deathData.hologram().remove();
        deathData.task().cancel();
        pet.getPersistentDataContainer().remove(deathTimestampKey);

        getTime(getOwnerUUID(pet), resurrectionTimes).thenApply(resurrectionTime -> {
            final long endTime = getCurrentSecond() + resurrectionTime;
            pet.getPersistentDataContainer().set(resurrectionTimestampKey, PersistentDataType.LONG, endTime);
            pendingResurrectingPets.put(pet, new PendingPetData(
                    createHologram(pet, "messages.will-resurrect", endTime),
                    Bukkit.getScheduler().runTaskLater(plugin, () -> resurrect(pet), resurrectionTime * 20)
            ));
            return null;
        });

        return true;
    }

    public boolean isManaged(LivingEntity pet) {
        return pendingDeadPets.containsKey(pet) || pendingResurrectingPets.containsKey(pet);
    }

    private Hologram createHologram(LivingEntity pet, String configKey, long endTime) {
        final Hologram hologram = new Hologram(pet.getEyeLocation().clone().add(0, 0.6, 0), pet.getUniqueId().toString(), 0.3);
        final String ownerName = Objects.requireNonNullElseGet(
                Bukkit.getOfflinePlayer(Objects.requireNonNull(getOwnerUUID(pet))).getName(),
                () -> Objects.requireNonNull(plugin.getConfig().getString("messages.unknown-player"))
        );
        hologram.addPlaceholder(new Placeholder(
                (player, string) -> string
                        .replace("%time%", TimeUtils.formatTime(endTime - getCurrentSecond()))
                        .replace("%owner%", ownerName)
        ));
        for (String line : plugin.getConfig().getStringList(configKey)) {
            hologram.addLine(line, HologramLine.Type.TEXT);
        }
        return hologram;
    }

    private void loadTimes(String section, List<Map.Entry<String, Integer>> times, boolean reverse) {
        final ConfigurationSection timesConfig = plugin.getConfig().getConfigurationSection(section);
        if (timesConfig != null) {
            for (String key : timesConfig.getKeys(false)) {
                times.add(Map.entry(key, timesConfig.getInt(key)));
            }
            times.sort(Comparator.comparingInt(entry -> reverse ? -entry.getValue() : entry.getValue()));
        }
    }

    private CompletableFuture<Integer> getTime(UUID userId, List<Map.Entry<String, Integer>> times) {
        return plugin.getLuckPerms().getUserManager().loadUser(userId).thenApply(user -> {
            if (user == null) return times.getLast().getValue();
            List<String> groups = user.getNodes(NodeType.INHERITANCE).stream().map(InheritanceNode::getGroupName).toList();
            for (Map.Entry<String, Integer> entry : times) {
                if (groups.contains(entry.getKey())) {
                    return entry.getValue();
                }
            }
            return times.getLast().getValue();
        });
    }
}
