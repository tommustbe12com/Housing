package com.tommustbe12.housing.util;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

public final class ItemStackSerialization {
    private ItemStackSerialization() {}

    public static String toBase64(ItemStack item) {
        if (item == null) return "";
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (BukkitObjectOutputStream out = new BukkitObjectOutputStream(baos)) {
                out.writeObject(item);
            }
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Failed serializing item", e);
        }
    }

    public static ItemStack fromBase64(String data) {
        if (data == null || data.isBlank()) return null;
        try {
            byte[] bytes = Base64.getDecoder().decode(data);
            try (BukkitObjectInputStream in = new BukkitObjectInputStream(new ByteArrayInputStream(bytes))) {
                return (ItemStack) in.readObject();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed deserializing item", e);
        }
    }
}

