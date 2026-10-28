package pt.flawless.fkits.menu;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import pt.flawless.fapi.enums.FInventorySize;
import pt.flawless.fapi.inventory.FInventory;
import pt.flawless.fkits.FKits;
import pt.flawless.fkits.model.Kit;
import pt.flawless.fkits.model.KitCategory;
import pt.flawless.fkits.service.KitService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class KitsMenu {
    private static final int BACK_SLOT = 49;
    private static final int MAX_KITS = 28;
    private static FKits plugin;

    private KitsMenu() {}

    public static void register(FKits p) {
        plugin = p;
        Bukkit.getPluginManager().registerEvents(new Handler(), p);
    }

    /** Thread principal; abre o menu depois de ler os cooldowns. */
    public static void open(Player player, String categoryId) {
        KitService service = plugin.service();
        KitCategory category = service.category(categoryId);
        if (category == null) return;
        service.lastClaims(player.getUniqueId()).whenComplete((claims, err) ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!player.isOnline()) return;
                    build(player, category, err != null ? Map.of() : claims);
                }));
    }

    private static void build(Player player, KitCategory category, Map<String, Long> claims) {
        KitService service = plugin.service();
        Holder holder = new Holder(category.getId());
        Inventory inv = new FInventory(holder, FInventorySize.SIZE_54, MenuFrame.color(category.getName())).getInventory();
        holder.inventory = inv;
        MenuFrame.frame(inv);
        inv.setItem(BACK_SLOT, MenuFrame.item(Material.ARROW, "§eVoltar",
                List.of("§7Volta à lista de categorias", "", "§7Clica para voltar")));

        List<Kit> kits = service.kitsIn(category.getId());
        long now = System.currentTimeMillis();
        for (int i = 0; i < Math.min(kits.size(), MAX_KITS); i++) {
            Kit kit = kits.get(i);
            int slot = (1 + i / 7) * 9 + 1 + i % 7;
            List<String> lore = new ArrayList<>();
            lore.add("§7Delay: §f" + (kit.getDelaySeconds() > 0 ? KitService.formatDuration(kit.getDelaySeconds() * 1000) : "sem delay"));
            lore.add("§7Itens: §f" + kit.getItems().size());
            lore.add("");
            if (!service.canUse(player, kit)) {
                lore.add("§cSem permissão");
            } else {
                Long last = claims.get(kit.getId());
                long remaining = last == null || service.hasBypass(player) ? 0 : last + kit.getDelaySeconds() * 1000 - now;
                lore.add(remaining > 0 ? "§cDisponível em " + KitService.formatDuration(remaining) : "§aClica para receber");
            }
            lore.add("§7Botão direito: ver itens");
            ItemStack base = kit.getIcon();
            boolean named = base.hasItemMeta() && base.getItemMeta().hasDisplayName();
            inv.setItem(slot, MenuFrame.withLore(base, named ? null : "§e" + kit.getId(), lore));
            holder.slots.put(slot, kit.getId());
        }
        player.openInventory(inv);
    }

    private static final class Holder implements InventoryHolder {
        final String category;
        final Map<Integer, String> slots = new HashMap<>();
        Inventory inventory;

        Holder(String category) {
            this.category = category;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    private static final class Handler implements Listener {
        @EventHandler
        public void onClick(InventoryClickEvent e) {
            if (!(e.getInventory().getHolder() instanceof Holder holder)) return;
            e.setCancelled(true);
            if (e.getClickedInventory() != e.getInventory() || !(e.getWhoClicked() instanceof Player p)) return;
            int slot = e.getRawSlot();
            if (slot == BACK_SLOT) {
                CategoriesMenu.open(p);
                return;
            }
            String id = holder.slots.get(slot);
            Kit kit = id == null ? null : plugin.service().kit(id);
            if (kit == null) return;
            if (e.isRightClick()) {
                KitItemsMenu.openView(p, kit);
            } else if (e.isLeftClick()) {
                claim(p, kit, holder.category);
            }
        }

        private void claim(Player p, Kit kit, String category) {
            KitService service = plugin.service();
            if (!service.canUse(p, kit)) {
                p.sendMessage("§cNão tens permissão para este kit.");
                return;
            }
            service.claim(p, kit, false).whenComplete((r, err) -> Bukkit.getScheduler().runTask(plugin, () -> {
                if (err != null) {
                    p.sendMessage("§cOcorreu um erro ao receber o kit.");
                    return;
                }
                switch (r.status()) {
                    case OK -> p.sendMessage("§aRecebeste o kit " + kit.getId() + ".");
                    case COOLDOWN -> p.sendMessage("§cPodes receber o kit " + kit.getId() + " daqui a "
                            + KitService.formatDuration(r.remainingMillis()) + ".");
                    case FULL -> p.sendMessage("§cNão tens espaço no inventário para o kit.");
                    case BUSY -> { return; }
                }
                // só atualiza se o jogador ainda estiver neste menu
                if (p.isOnline() && p.getOpenInventory().getTopInventory().getHolder() instanceof Holder) open(p, category);
            }));
        }

        @EventHandler
        public void onDrag(InventoryDragEvent e) {
            if (e.getInventory().getHolder() instanceof Holder) e.setCancelled(true);
        }
    }
}
