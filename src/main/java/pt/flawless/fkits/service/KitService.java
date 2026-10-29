package pt.flawless.fkits.service;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import pt.flawless.fapi.database.DatabaseService;
import pt.flawless.fkits.config.KitsStore;
import pt.flawless.fkits.model.Kit;
import pt.flawless.fkits.model.KitCategory;
import pt.flawless.fkits.storage.CooldownRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/** Dados na thread principal; os futuros de BD completam fora dela, exceto {@link #claim}. */
public class KitService {
    private final JavaPlugin plugin;
    private final KitsStore store;
    private final DatabaseService database;
    private final CooldownRepository cooldowns;
    private final Set<UUID> claiming = ConcurrentHashMap.newKeySet();
    private Map<String, KitCategory> categories = new LinkedHashMap<>();
    private Map<String, Kit> kits = new LinkedHashMap<>();

    public KitService(JavaPlugin plugin, KitsStore store, DatabaseService database, CooldownRepository cooldowns) {
        this.plugin = plugin;
        this.store = store;
        this.database = database;
        this.cooldowns = cooldowns;
        reload();
    }

    public void reload() {
        KitsStore.Data data = store.load();
        categories = new LinkedHashMap<>(data.categories());
        kits = new LinkedHashMap<>(data.kits());
    }

    public void save() {
        try {
            store.save(categories, kits);
        } catch (IOException e) {
            plugin.getLogger().severe("não foi possível gravar kits.yml: " + e.getMessage());
        }
    }

    public Collection<KitCategory> categories() { return categories.values(); }

    public KitCategory category(String id) { return categories.get(id); }

    public Collection<Kit> kits() { return kits.values(); }

    public List<Kit> kitsIn(String categoryId) {
        List<Kit> out = new ArrayList<>();
        for (Kit k : kits.values()) if (k.getCategory().equals(categoryId)) out.add(k);
        return out;
    }

    public Kit kit(String id) { return kits.get(id); }

    /** Slot (0 até linhas×9-1) -> kit; os sem posição válida (ou repetida) ocupam os primeiros slots livres. */
    public Map<Integer, Kit> layout(String categoryId) {
        Map<Integer, Kit> slots = new TreeMap<>();
        int rows = category(categoryId).getRows();
        List<Kit> loose = new ArrayList<>();
        for (Kit k : kitsIn(categoryId)) {
            int c = k.getColumn(), r = k.getRow();
            if (c >= 1 && c <= 9 && r >= 1 && r <= rows && slots.putIfAbsent((r - 1) * 9 + c - 1, k) == null) continue;
            loose.add(k);
        }
        int next = 0;
        for (Kit k : loose) {
            while (slots.containsKey(next)) next++;
            if (next >= rows * 9) break;
            slots.put(next, k);
        }
        return slots;
    }

    public KitCategory createCategory(String id, String name) {
        KitCategory c = new KitCategory(id, name, new ItemStack(Material.CHEST));
        categories.put(id, c);
        save();
        return c;
    }

    public void deleteCategory(String id) {
        categories.remove(id);
        save();
    }

    public Kit createKit(String id, String categoryId, long delay, int column, int row) {
        Kit k = new Kit(id, categoryId, delay, new ItemStack(Material.CHEST));
        k.setPosition(column, row);
        kits.put(id, k);
        save();
        return k;
    }

    public void deleteKit(String id) {
        kits.remove(id);
        save();
    }

    public boolean canUse(Player player, Kit kit) {
        return player.hasPermission("fkits.kit." + kit.getId());
    }

    public boolean hasBypass(Player player) {
        return player.hasPermission("fkits.bypass");
    }

    public CompletableFuture<Map<String, Long>> lastClaims(UUID uuid) {
        return database.supplyAsync(c -> cooldowns.findAll(c, uuid));
    }

    public CompletableFuture<Void> resetCooldown(UUID uuid, String kitId) {
        return database.runAsync(c -> cooldowns.delete(c, uuid, kitId));
    }

    /** O futuro completa na thread principal; falhas de BD completam-no excecionalmente. */
    public CompletableFuture<ClaimResult> claim(Player target, Kit kit, boolean force) {
        UUID uuid = target.getUniqueId();
        if (!claiming.add(uuid)) {
            return CompletableFuture.completedFuture(new ClaimResult(ClaimResult.Status.BUSY, 0));
        }
        CompletableFuture<ClaimResult> result = new CompletableFuture<>();
        lastClaims(uuid).whenComplete((claims, err) -> Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                if (err != null) {
                    claiming.remove(uuid);
                    result.completeExceptionally(err);
                    return;
                }
                finishClaim(target, kit, force, claims.get(kit.getId()), result);
            } catch (RuntimeException e) {
                claiming.remove(uuid);
                result.completeExceptionally(e);
            }
        }));
        return result;
    }

    private void finishClaim(Player target, Kit kit, boolean force, Long last, CompletableFuture<ClaimResult> result) {
        UUID uuid = target.getUniqueId();
        long now = System.currentTimeMillis();
        if (!force && !hasBypass(target) && last != null && kit.getDelaySeconds() > 0) {
            long remaining = last + kit.getDelaySeconds() * 1000 - now;
            if (remaining > 0) {
                claiming.remove(uuid);
                result.complete(new ClaimResult(ClaimResult.Status.COOLDOWN, remaining));
                return;
            }
        }
        // simula o addItem num inventário de 36 slots para saber se tudo cabe
        Inventory temp = Bukkit.createInventory(null, 36);
        temp.setContents(target.getInventory().getStorageContents());
        for (ItemStack item : kit.getItems()) {
            if (item != null && !temp.addItem(item.clone()).isEmpty()) {
                claiming.remove(uuid);
                result.complete(new ClaimResult(ClaimResult.Status.FULL, 0));
                return;
            }
        }
        for (ItemStack item : kit.getItems()) if (item != null) target.getInventory().addItem(item.clone());
        if (force) {
            claiming.remove(uuid);
            result.complete(new ClaimResult(ClaimResult.Status.OK, 0));
            return;
        }
        database.runAsync(c -> cooldowns.upsert(c, uuid, kit.getId(), now)).whenComplete((v, err) ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    claiming.remove(uuid);
                    if (err != null) result.completeExceptionally(err);
                    else result.complete(new ClaimResult(ClaimResult.Status.OK, 0));
                }));
    }

    /** "1d 12h 30m 15s" (qualquer subconjunto) ou só segundos ("90"); -1 se inválido. */
    public static long parseDuration(String text) {
        String t = text.replaceAll("\\s+", "").toLowerCase();
        if (t.matches("\\d{1,9}")) return Long.parseLong(t);
        if (!t.matches("(\\d{1,6}[dhms])+")) return -1;
        long total = 0;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)([dhms])").matcher(t);
        while (m.find()) {
            long n = Long.parseLong(m.group(1));
            total += n * switch (m.group(2).charAt(0)) {
                case 'd' -> 86400L;
                case 'h' -> 3600L;
                case 'm' -> 60L;
                default -> 1L;
            };
        }
        return total;
    }

    public static String formatDuration(long millis) {
        long s = Math.max(0, millis / 1000);
        long d = s / 86400, h = s % 86400 / 3600, m = s % 3600 / 60, sec = s % 60;
        StringBuilder sb = new StringBuilder();
        if (d > 0) sb.append(d).append("d ");
        if (h > 0) sb.append(h).append("h ");
        if (m > 0) sb.append(m).append("m ");
        if (d == 0 && h == 0 && m == 0) sb.append(sec).append("s ");
        return sb.toString().trim();
    }
}
