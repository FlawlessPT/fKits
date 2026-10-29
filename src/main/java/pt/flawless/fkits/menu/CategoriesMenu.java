package pt.flawless.fkits.menu;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import pt.flawless.fapi.enums.FInventorySize;
import pt.flawless.fapi.inventory.FInventory;
import pt.flawless.fkits.FKits;
import pt.flawless.fkits.model.KitCategory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CategoriesMenu {
    private static FKits plugin;

    private CategoriesMenu() {}

    public static void register(FKits p) {
        plugin = p;
        Bukkit.getPluginManager().registerEvents(new Handler(), p);
    }

    /** Thread principal. */
    public static void open(Player player) {
        Holder holder = new Holder();
        Inventory inv = new FInventory(holder, FInventorySize.SIZE_27, "Kits").getInventory();
        holder.inventory = inv;
        // máx. 9 categorias na fila do meio; 3 ficam espaçadas (colunas 2, 5 e 8), as restantes centradas
        List<KitCategory> cats = new ArrayList<>(plugin.service().categories());
        int n = Math.min(cats.size(), 9);
        int start = 9 + (9 - n) / 2;
        for (int i = 0; i < n; i++) {
            KitCategory c = cats.get(i);
            int slot = n == 3 ? 10 + i * 3 : start + i;
            String name = MenuFrame.color(c.getName());
            inv.setItem(slot, MenuFrame.withLore(c.getIcon(), name, List.of("§7Clica para ver os kits " + name)));
            holder.slots.put(slot, c.getId());
        }
        player.openInventory(inv);
    }

    private static final class Holder implements InventoryHolder {
        final Map<Integer, String> slots = new HashMap<>();
        Inventory inventory;

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
            String cat = holder.slots.get(e.getRawSlot());
            if (cat != null) KitsMenu.open(p, cat);
        }

        @EventHandler
        public void onDrag(InventoryDragEvent e) {
            if (e.getInventory().getHolder() instanceof Holder) e.setCancelled(true);
        }
    }
}
