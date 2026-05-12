package com.tommustbe12.housing.houses;

import com.tommustbe12.housing.debug.Debug;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.Plugin;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class HouseManager {
    private final Plugin plugin;
    private final Debug debug;
    private final HouseStorage storage;

    private final Map<String, ActiveHouse> active = new ConcurrentHashMap<>();
    private final Map<String, BukkitTask> deactivateTasks = new ConcurrentHashMap<>();
    private final Map<String, String> worldToHouseId = new ConcurrentHashMap<>();
    private final Map<UUID, PermissionAttachment> ownerAttachments = new ConcurrentHashMap<>();
    private final Map<UUID, PermissionAttachment> commandAttachments = new ConcurrentHashMap<>();
    private volatile Consumer<World> onHouseDeactivated;

    public HouseManager(Plugin plugin, Debug debug) {
        this.plugin = plugin;
        this.debug = debug;
        this.storage = new HouseStorage(plugin);
    }

    public void setOnHouseDeactivated(Consumer<World> onHouseDeactivated) {
        this.onHouseDeactivated = onHouseDeactivated;
    }

    public HouseData getHouse(UUID owner, HouseSlot slot) {
        return storage.load(owner, slot);
    }

    public void saveHouse(HouseData data) {
        storage.save(data);
    }

    public boolean houseExists(UUID owner, HouseSlot slot) {
        return storage.fileExists(owner, slot);
    }

    public void createIfMissing(UUID owner, HouseSlot slot) {
        if (houseExists(owner, slot)) return;
        HouseData data = new HouseData(owner, slot);
        saveHouse(data);
        debug.toOps("Created new house data owner=" + owner + " slot=" + slot.index());
    }

    public ActiveHouse startOrGetActiveHouse(UUID owner, HouseSlot slot) {
        String id = id(owner, slot);
        ActiveHouse existing = active.get(id);
        if (existing != null) {
            cancelDeactivate(id);
            return existing;
        }

        HouseData data = getHouse(owner, slot);
        debug.toOps("Starting house " + id + " (name=" + data.name() + ")");

        WorldCreateResult res = createOrLoadWorld(worldName(owner, slot));
        World world = res.world;
        world.setTime(data.timeOfDay());
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setDifficulty(Difficulty.NORMAL);
        applyWorldBorder(world);

        // Only build the starter platform for brand-new worlds.
        if (res.createdFresh) ensureStarterPlatform(world);

        Location spawn = data.spawnInWorld(world);
        if (spawn == null) {
            spawn = new Location(world, 0.5, starterY() + 1.0, 0.5, 0f, 0f);
        }

        ActiveHouse activeHouse = new ActiveHouse(id, data, world, spawn);
        active.put(id, activeHouse);
        worldToHouseId.put(world.getName(), id);
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

        debug.toOps(player.getName() + " joining house " + activeHouse.id() +
                " spawn=" + activeHouse.spawn().getBlockX() + "," + activeHouse.spawn().getBlockY() + "," + activeHouse.spawn().getBlockZ());
        activeHouse.spawn().getChunk().load();
        player.teleport(activeHouse.spawn());
        applyOwnerState(player, owner);
        player.setAllowFlight(true);
        player.setFlying(false);
        player.sendMessage("§aJoining §f" + ChatColor.translateAlternateColorCodes('&', data.name()) + "§a...");
        return true;
    }

    public void sendToHub(Player player) {
        String hubWorldName = plugin.getConfig().getString("hub.world", "");
        World hub = hubWorldName == null || hubWorldName.isBlank() ? Bukkit.getWorlds().getFirst() : Bukkit.getWorld(hubWorldName);
        if (hub == null) hub = Bukkit.getWorlds().getFirst();
        player.teleport(hub.getSpawnLocation());
        player.sendMessage("§aReturned to the hub.");
    }

    public void addCookie(UUID owner, HouseSlot slot, int amount) {
        HouseData data = getHouse(owner, slot);
        String currentWeek = com.tommustbe12.housing.cookies.WeekKey.currentWeekKey();
        if (!currentWeek.equals(data.cookiesWeek())) {
            data.setCookies(0);
            data.setCookiesWeek(currentWeek);
        }
        data.addCookies(amount);
        saveHouse(data);
    }

    public List<HouseData> topHousesByCookies(int limit) {
        File[] files = storage.housesDir().listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return List.of();

        List<HouseData> all = new ArrayList<>();
        for (File file : files) {
            try {
                String base = file.getName().substring(0, file.getName().length() - 4); // trim .yml
                int dash = base.lastIndexOf('-');
                if (dash <= 0 || dash >= base.length() - 1) continue;
                UUID owner = UUID.fromString(base.substring(0, dash));
                int slotIndex = Integer.parseInt(base.substring(dash + 1));
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
        for (UUID uuid : ownerAttachments.keySet()) {
            revokeOwnerPerms(uuid);
        }
        for (BukkitTask task : deactivateTasks.values()) {
            task.cancel();
        }
        deactivateTasks.clear();
        worldToHouseId.clear();
        active.clear();
    }

    public void deleteHouse(UUID owner, HouseSlot slot) {
        String id = id(owner, slot);
        ActiveHouse house = active.remove(id);
        String worldName = worldName(owner, slot);
        if (house != null) worldName = house.world().getName();

        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            for (Player player : List.copyOf(world.getPlayers())) {
                sendToHub(player);
            }
        }

        if (house != null) {
            worldToHouseId.remove(house.world().getName());
        }

        if (world != null) {
            Consumer<World> cb = onHouseDeactivated;
            if (cb != null) {
                try {
                    cb.accept(world);
                } catch (Exception ignored) {
                }
            }
            Bukkit.unloadWorld(world, true);
        }

        // Ensure the world folder is deleted so recreating the house is truly fresh.
        deleteWorldFolder(worldName);
        storage.delete(owner, slot);
    }

    public HouseWorldInfo getHouseInfoByWorld(World world) {
        if (world == null) return null;
        String id = worldToHouseId.get(world.getName());
        if (id == null) return null;
        ActiveHouse house = active.get(id);
        if (house == null) return null;
        return new HouseWorldInfo(house.data().owner(), house.data().slot());
    }

    public Location getSpawn(UUID owner, HouseSlot slot) {
        ActiveHouse activeHouse = active.get(id(owner, slot));
        if (activeHouse != null) return activeHouse.spawn();
        // If not active, derive default spawn for that house world name (may not be loaded)
        World world = Bukkit.getWorld(worldName(owner, slot));
        if (world == null) return null;
        HouseData data = getHouse(owner, slot);
        Location spawn = data.spawnInWorld(world);
        if (spawn == null) {
            return new Location(world, 0.5, starterY() + 1.0, 0.5, 0f, 0f);
        }
        return spawn;
    }

    public void scheduleDeactivateIfEmpty(World world) {
        if (world == null) return;
        String id = worldToHouseId.get(world.getName());
        if (id == null) return;
        if (!world.getPlayers().isEmpty()) return;

        cancelDeactivate(id);
        int seconds = plugin.getConfig().getInt("houses.deactivate-seconds", 10);
        debug.toOps("Scheduling deactivate for " + id + " in " + seconds + "s");
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> deactivateWorldIfStillEmpty(world.getName()), seconds * 20L);
        deactivateTasks.put(id, task);
    }

    private void deactivateWorldIfStillEmpty(String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) return;
        if (!world.getPlayers().isEmpty()) return;

        String id = worldToHouseId.get(worldName);
        if (id == null) return;
        debug.toOps("Deactivating house " + id + " due to inactivity");

        cancelDeactivate(id);
        worldToHouseId.remove(worldName);
        active.remove(id);
        Consumer<World> cb = onHouseDeactivated;
        if (cb != null) {
            try {
                cb.accept(world);
            } catch (Exception ignored) {
            }
        }
        Bukkit.unloadWorld(world, true);
    }

    private void cancelDeactivate(String id) {
        BukkitTask task = deactivateTasks.remove(id);
        if (task != null) task.cancel();
    }

    private void grantOwnerPerms(Player player) {
        PermissionAttachment existing = ownerAttachments.get(player.getUniqueId());
        if (existing != null) return;
        PermissionAttachment attachment = player.addAttachment(plugin);
        attachment.setPermission("minecraft.command.gamemode", true);
        attachment.setPermission("minecraft.command.gamemode.creative", true);
        attachment.setPermission("minecraft.command.gamemode.survival", true);
        attachment.setPermission("minecraft.command.gamemode.adventure", true);
        attachment.setPermission("minecraft.command.time", true);
        ownerAttachments.put(player.getUniqueId(), attachment);
    }

    private void revokeOwnerPerms(UUID playerId) {
        PermissionAttachment attachment = ownerAttachments.remove(playerId);
        if (attachment != null) attachment.remove();
    }

    public void applyHouseCommandPerms(Player player, boolean allowSwitchGamemode, boolean allowCreative) {
        if (player == null) return;
        UUID id = player.getUniqueId();
        PermissionAttachment existing = commandAttachments.get(id);
        if (!allowSwitchGamemode) {
            if (existing != null) {
                existing.remove();
                commandAttachments.remove(id);
            }
            return;
        }
        if (existing == null) {
            existing = player.addAttachment(plugin);
            commandAttachments.put(id, existing);
        }
        existing.setPermission("minecraft.command.gamemode", true);
        existing.setPermission("minecraft.command.gamemode.survival", true);
        existing.setPermission("minecraft.command.gamemode.adventure", true);
        existing.setPermission("minecraft.command.gamemode.creative", allowCreative);
    }

    public void applyOwnerState(Player player, UUID houseOwner) {
        boolean isOwner = player.getUniqueId().equals(houseOwner);
        player.setGameMode(isOwner ? GameMode.CREATIVE : GameMode.ADVENTURE);
        if (isOwner) {
            grantOwnerPerms(player);
        } else {
            revokeOwnerPerms(player.getUniqueId());
        }
    }

    private WorldCreateResult createOrLoadWorld(String name) {
        World existing = Bukkit.getWorld(name);
        if (existing != null) return new WorldCreateResult(existing, false);

        boolean createdFresh = true;
        File container = Bukkit.getWorldContainer();
        if (container != null) {
            File folder = new File(container, name);
            // If the world folder already exists on disk, we're loading an existing house, not creating a new one.
            createdFresh = !folder.exists();
        }

        WorldCreator creator = new WorldCreator(name);
        creator.environment(World.Environment.NORMAL);
        creator.type(WorldType.FLAT);
        creator.generator(new EmptyVoidGenerator());
        World world = creator.createWorld();
        if (world == null) throw new IllegalStateException("Failed to create world: " + name);
        return new WorldCreateResult(world, createdFresh);
    }

    private void applyWorldBorder(World world) {
        // Size is full width (diameter). A 256 border gives -128..+128-ish playable area around center.
        double size = plugin.getConfig().getDouble("houses.world-border.size", 256.0);
        if (size <= 0) size = 256.0;
        world.getWorldBorder().setCenter(0.0, 0.0);
        world.getWorldBorder().setSize(size);
    }

    private void deleteWorldFolder(String worldName) {
        if (worldName == null || worldName.isBlank()) return;
        File container = Bukkit.getWorldContainer();
        if (container == null) return;
        File folder = new File(container, worldName);
        if (!folder.exists()) return;
        if (!isInside(container, folder)) {
            debug.warn("Refusing to delete world folder outside world container: " + folder.getPath());
            return;
        }
        deleteDir(folder);
    }

    private static boolean isInside(File parent, File child) {
        try {
            String p = parent.getCanonicalPath();
            String c = child.getCanonicalPath();
            return c.startsWith(p + File.separator);
        } catch (IOException e) {
            return false;
        }
    }

    private static void deleteDir(File dir) {
        if (dir == null || !dir.exists()) return;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) deleteDir(f);
                //noinspection ResultOfMethodCallIgnored
                f.delete();
            }
        }
        //noinspection ResultOfMethodCallIgnored
        dir.delete();
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

    public record HouseWorldInfo(UUID owner, HouseSlot slot) {
    }

    private static final class EmptyVoidGenerator extends ChunkGenerator {
    }

    private static final class WorldCreateResult {
        private final World world;
        private final boolean createdFresh;

        private WorldCreateResult(World world, boolean createdFresh) {
            this.world = world;
            this.createdFresh = createdFresh;
        }
    }
}
