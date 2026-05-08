package com.tommustbe12.housing.npcs;

import com.tommustbe12.housing.actions.ActionContext;
import com.tommustbe12.housing.actions.ActionsEngine;
import com.tommustbe12.housing.actions.placeholders.Placeholders;
import com.tommustbe12.housing.actions.placeholders.VariablesStore;
import com.tommustbe12.housing.actions.storage.SimpleActionCodec;
import com.tommustbe12.housing.actions.storage.SimpleActionSerializer;
import com.tommustbe12.housing.debug.Debug;
import com.tommustbe12.housing.houses.HouseManager;
import com.tommustbe12.housing.houses.HouseSlot;
import com.tommustbe12.housing.inventorylayouts.InventoryLayoutsService;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.api.trait.trait.Equipment;
import net.citizensnpcs.trait.SkinTrait;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class NpcManager {
    private final Plugin plugin;
    private final Debug debug;
    private final HouseManager houses;
    private final NpcStorage storage;
    private final SimpleActionSerializer serializer = new SimpleActionSerializer();
    private final ActionsEngine engine;

    private final SimpleActionCodec codec;

    private final Map<String, List<NpcData>> dataByHouse = new ConcurrentHashMap<>();
    private final Map<Integer, NpcData> npcByCitizensId = new ConcurrentHashMap<>();

    private BukkitTask behaviorTask;
    private final boolean citizensAvailable;

    public NpcManager(Plugin plugin, Debug debug, HouseManager houses) {
        this.plugin = plugin;
        this.debug = debug;
        this.houses = houses;
        this.storage = new NpcStorage(plugin);
        this.engine = new ActionsEngine(plugin, debug);

        VariablesStore vars = new VariablesStore(plugin);
        Placeholders ph = new Placeholders(vars);
        this.codec = new SimpleActionCodec(ph, vars, houses, (ctx, fn, global) -> {},
                new InventoryLayoutsService(plugin),
                new com.tommustbe12.housing.custommenus.CustomMenusService(plugin, houses));

        this.citizensAvailable = plugin.getServer().getPluginManager().isPluginEnabled("Citizens");
    }

    public boolean isCitizensAvailable() {
        return citizensAvailable;
    }

    public void start() {
        if (behaviorTask != null) return;
        if (!citizensAvailable) {
            debug.warn("Citizens not found - NPCs disabled.");
            return;
        }
        behaviorTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tickBehaviors, 20L, 20L);
    }

    public void stop() {
        if (behaviorTask != null) behaviorTask.cancel();
        behaviorTask = null;
        npcByCitizensId.clear();
        dataByHouse.clear();
    }

    public List<NpcData> getNpcs(UUID owner, HouseSlot slot, World world) {
        String key = key(owner, slot);
        return dataByHouse.computeIfAbsent(key, k -> storage.load(owner, slot, world, codec));
    }

    public void save(UUID owner, HouseSlot slot, List<NpcData> npcs) {
        storage.save(owner, slot, npcs, serializer);
    }

    public void spawnAll(UUID owner, HouseSlot slot, World world) {
        if (!citizensAvailable) return;
        List<NpcData> npcs = getNpcs(owner, slot, world);
        for (NpcData npc : npcs) spawnOrUpdateOne(owner, slot, world, npc);
    }

    public void despawnAll(World world) {
        if (!citizensAvailable || world == null) return;
        for (NpcData npc : new ArrayList<>(npcByCitizensId.values())) {
            try {
                NPC c = registry().getById(npc.citizensNpcId());
                if (c != null && c.isSpawned() && c.getEntity() != null && c.getEntity().getWorld().equals(world)) {
                    c.despawn();
                }
            } catch (Exception ignored) {
            }
        }
    }

    public NpcData createNpc(UUID owner, HouseSlot slot, World world, Location location) {
        if (!citizensAvailable) return null;
        NpcData npc = new NpcData(UUID.randomUUID());
        npc.setLocation(location.clone());
        getNpcs(owner, slot, world).add(npc);
        save(owner, slot, getNpcs(owner, slot, world));
        spawnOrUpdateOne(owner, slot, world, npc);
        return npc;
    }

    public void deleteNpc(UUID owner, HouseSlot slot, World world, UUID npcId) {
        List<NpcData> list = getNpcs(owner, slot, world);
        NpcData target = null;
        for (NpcData n : list) if (n.id().equals(npcId)) { target = n; break; }
        if (target == null) return;
        list.remove(target);
        save(owner, slot, list);

        if (citizensAvailable && target.citizensNpcId() > 0) {
            try {
                NPC c = registry().getById(target.citizensNpcId());
                if (c != null) {
                    if (c.isSpawned()) c.despawn();
                    registry().deregister(c);
                }
            } catch (Exception ignored) {
            }
        }
        npcByCitizensId.remove(target.citizensNpcId());
    }

    public void refreshNpcEntity(UUID owner, HouseSlot slot, World world, NpcData npc) {
        if (!citizensAvailable) return;
        spawnOrUpdateOne(owner, slot, world, npc);
    }

    public void clickNpc(Player clicker, UUID owner, HouseSlot slot, World world, NpcData npc) {
        engine.run(npc.clickActions(), new ActionContext(plugin, debug, owner, slot, world, clicker, null, clicker.getLocation()));
    }

    public NpcData getByCitizensId(UUID owner, HouseSlot slot, World world, int citizensId) {
        if (!citizensAvailable) return null;
        NpcData cached = npcByCitizensId.get(citizensId);
        if (cached != null) return cached;
        for (NpcData n : getNpcs(owner, slot, world)) {
            if (n.citizensNpcId() == citizensId) {
                npcByCitizensId.put(citizensId, n);
                return n;
            }
        }
        return null;
    }

    private void spawnOrUpdateOne(UUID owner, HouseSlot slot, World world, NpcData npc) {
        if (!citizensAvailable || world == null || npc == null) return;
        Location loc = npc.location();
        if (loc == null) return;
        loc = new Location(world, loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());

        NPCRegistry reg = registry();
        NPC c = npc.citizensNpcId() > 0 ? reg.getById(npc.citizensNpcId()) : null;
        if (c == null) {
            c = reg.createNPC(org.bukkit.entity.EntityType.PLAYER, npc.name());
            npc.setCitizensNpcId(c.getId());
            npcByCitizensId.put(c.getId(), npc);
            if (owner != null && slot != null) save(owner, slot, getNpcs(owner, slot, world));
        } else {
            c.setName(npc.name());
            npcByCitizensId.put(c.getId(), npc);
        }

        try {
            SkinTrait skin = c.getOrAddTrait(SkinTrait.class);
            skin.setSkinName(npc.skinUsername());
        } catch (Throwable ignored) {
        }

        try {
            Equipment eq = c.getOrAddTrait(Equipment.class);
            eq.set(Equipment.EquipmentSlot.HELMET, npc.helmet());
            eq.set(Equipment.EquipmentSlot.CHESTPLATE, npc.chestplate());
            eq.set(Equipment.EquipmentSlot.LEGGINGS, npc.leggings());
            eq.set(Equipment.EquipmentSlot.BOOTS, npc.boots());
            eq.set(Equipment.EquipmentSlot.HAND, npc.hand());
            eq.set(Equipment.EquipmentSlot.OFF_HAND, npc.offhand());
        } catch (Throwable ignored) {
        }

        if (c.isSpawned() && c.getEntity() != null && c.getEntity().getWorld().equals(world)) {
            c.teleport(loc, org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.PLUGIN);
        } else {
            if (c.isSpawned()) c.despawn();
            c.spawn(loc);
        }
    }

    private void tickBehaviors() {
        if (!citizensAvailable) return;
        for (NpcData npc : npcByCitizensId.values()) {
            if (npc.behavior() == NpcBehavior.STATIC) continue;
            try {
                NPC c = registry().getById(npc.citizensNpcId());
                if (c == null || !c.isSpawned() || c.getEntity() == null) continue;

                if ((npc.behavior() == NpcBehavior.JUMP || npc.behavior() == NpcBehavior.WALK_JUMP) && Math.random() < 0.35) {
                    c.getEntity().setVelocity(c.getEntity().getVelocity().setY(0.35));
                }

                if (npc.behavior() == NpcBehavior.WALK || npc.behavior() == NpcBehavior.WALK_JUMP) {
                    if (c.getNavigator().isNavigating()) continue;
                    if (Math.random() < 0.75) {
                        Location base = c.getEntity().getLocation();
                        double dx = (Math.random() * 8.0) - 4.0;
                        double dz = (Math.random() * 8.0) - 4.0;
                        Location target = base.clone().add(dx, 0, dz);
                        c.getNavigator().setTarget(target);
                    }
                }
            } catch (Throwable ignored) {
            }
        }
    }

    private NPCRegistry registry() {
        return CitizensAPI.getNPCRegistry();
    }

    private static String key(UUID owner, HouseSlot slot) {
        return owner + ":" + slot.index();
    }
}
