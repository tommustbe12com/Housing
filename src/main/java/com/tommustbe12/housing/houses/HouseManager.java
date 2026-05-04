package com.tommustbe12.housing.houses;

import com.tommustbe12.housing.debug.Debug;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class HouseManager {
    private final Plugin plugin;
    private final Debug debug;
    private final HouseStorage storage;

    private final Map<String, ActiveHouse> active = new ConcurrentHashMap<>();

    public HouseManager(Plugin plugin, Debug debug) {
        this.plugin = plugin;
        this.debug = debug;
        this.storage = new HouseStorage(plugin);
    }

    public HouseData getHouse(UUID owner, HouseSlot slot) {
        return storage.load(owner, slot);
    }

    public void saveHouse(HouseData data) {
        storage.save(data);
    }

    public ActiveHouse startOrGetActiveHouse(UUID owner, HouseSlot slot) {
        String id = id(owner, slot);
        ActiveHouse existing = active.get(id);
        if (existing != null) return existing;

        HouseData data = getHouse(owner, slot);
        debug.toOps("Starting house " + id + " (name=" + data.name() + ")");

        World world = createOrLoadWorld(worldName(owner, slot));
        world.setTime(data.timeOfDay());
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        world.setDifficulty(Difficulty.NORMAL);

        ensureStarterPlatform(world);

        Location spawn = data.spawn();
        if (spawn == null || spawn.getWorld() == null) {
            spawn = new Location(world, 0.5, starterY() + 1.0, 0.5, 0f, 0f);
        } else {
            spawn = new Location(world, spawn.getX(), spawn.getY(), spawn.getZ(), spawn.getYaw(), spawn.getPitch());
        }

        ActiveHouse activeHouse = new ActiveHouse(id, data, world, spawn);
        active.put(id, activeHouse);
        return activeHouse;
    }

    public boolean joinHouse(Player player, UUID owner, HouseSlot slot) {
        HouseData data = getHouse(owner, slot);
        ActiveHouse activeHouse = startOrGetActiveHouse(owner, slot);

        int online = activeHouse.world().getPlayers().size();
        if (online >= data.maxPlayers()) {
            player.sendMessage("§cThat house is full.");
            return false;
        }

        debug.toOps(player.getName() + " joining house " + activeHouse.id());
        player.teleport(activeHouse.spawn());
        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlight(true);
        player.setFlying(false);
        player.sendMessage("§aJoining §f" + ChatColor.translateAlternateColorCodes('&', data.name()) + "§a...");
        return true;
    }

    public void addCookie(UUID owner, HouseSlot slot, int amount) {
        HouseData data = getHouse(owner, slot);
        data.addCookies(amount);
        saveHouse(data);
    }

    public List<HouseData> topHousesByCookies(int limit) {
        File[] files = storage.housesDir().listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return List.of();

        List<HouseData> all = new ArrayList<>();
        for (File file : files) {
            String base = file.getName().replace(".yml", "");
            String[] parts = base.split("-");
            if (parts.length < 2) continue;
            try {
                UUID owner = UUID.fromString(parts[0]);
                int slotIndex = Integer.parseInt(parts[1]);
                HouseSlot slot = HouseSlot.fromIndex(slotIndex);
                if (slot == null) continue;
                all.add(getHouse(owner, slot));
            } catch (Exception ignored) {
            }
        }

        all.sort(Comparator.comparingInt(HouseData::cookies).reversed());
        if (all.size() > limit) return all.subList(0, limit);
        return all;
    }

    public void shutdown() {
        for (ActiveHouse house : active.values()) {
            debug.toOps("Shutting down active house " + house.id());
        }
        active.clear();
    }

    private World createOrLoadWorld(String name) {
        World existing = Bukkit.getWorld(name);
        if (existing != null) return existing;

        WorldCreator creator = new WorldCreator(name);
        creator.environment(World.Environment.NORMAL);
        creator.type(WorldType.FLAT);
        creator.generator(new EmptyVoidGenerator());
        World world = creator.createWorld();
        if (world == null) throw new IllegalStateException("Failed to create world: " + name);
        return world;
    }

    private void ensureStarterPlatform(World world) {
        int size = plugin.getConfig().getInt("houses.starter-platform.size", 8);
        int y = starterY();
        int half = size / 2;
        for (int x = -half; x < -half + size; x++) {
            for (int z = -half; z < -half + size; z++) {
                Block base = world.getBlockAt(x, y - 1, z);
                if (base.getType() != Material.BEDROCK) base.setType(Material.BEDROCK, false);
                Block grass = world.getBlockAt(x, y, z);
                if (grass.getType() != Material.GRASS_BLOCK) grass.setType(Material.GRASS_BLOCK, false);
                for (int yy = y + 1; yy <= y + 6; yy++) {
                    Block air = world.getBlockAt(x, yy, z);
                    if (air.getType() != Material.AIR) air.setType(Material.AIR, false);
                }
            }
        }
    }

    private int starterY() {
        return plugin.getConfig().getInt("houses.starter-platform.y", 64);
    }

    private static String id(UUID owner, HouseSlot slot) {
        return owner + ":" + slot.index();
    }

    private static String worldName(UUID owner, HouseSlot slot) {
        return "housing_" + owner.toString().replace("-", "") + "_" + slot.index();
    }

    public record ActiveHouse(String id, HouseData data, World world, Location spawn) {
    }

    private static final class EmptyVoidGenerator extends ChunkGenerator {
    }
}

