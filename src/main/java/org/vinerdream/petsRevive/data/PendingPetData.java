package org.vinerdream.petsRevive.data;

import com.artillexstudios.axapi.hologram.Hologram;
import org.bukkit.scheduler.BukkitTask;

public record PendingPetData(Hologram hologram, BukkitTask task) {
}
