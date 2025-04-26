package org.vinerdream.petsRevive.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ItemUtils {
    public static String getCustomName(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        Component customName = meta.customName();
        if (customName == null) {
            return null;
        }
        return PlainTextComponentSerializer.plainText().serialize(customName);
    }
}
