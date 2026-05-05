package com.tommustbe12.housing.inventory;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

final class ItemSerialization {
    private ItemSerialization() {
    }

    static String toBase64(ItemStack[] items) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (BukkitObjectOutputStream out = new BukkitObjectOutputStream(baos)) {
                out.writeInt(items.length);
                for (ItemStack item : items) {
                    out.writeObject(item);
                }
            }
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Failed serializing inventory", e);
        }
    }

    static ItemStack[] fromBase64(String data) {
        try {
            byte[] bytes = Base64.getDecoder().decode(data);
            try (BukkitObjectInputStream in = new BukkitObjectInputStream(new ByteArrayInputStream(bytes))) {
                int len = in.readInt();
                ItemStack[] items = new ItemStack[len];
                for (int i = 0; i < len; i++) {
                    items[i] = (ItemStack) in.readObject();
                }
                return items;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed deserializing inventory", e);
        }
    }
}

