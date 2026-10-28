package pt.flawless.fkits.menu;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import pt.flawless.fapi.enums.FInventorySize;
import pt.flawless.fapi.inventory.FInventory;
import pt.flawless.fkits.FKits;
import pt.flawless.fkits.model.Kit;

import java.util.ArrayList;
import java.util.List;

public final class KitItemsMenu {
    private static FKits plugin;

    private KitItemsMenu() {}

    public static void register(FKits p) {
        plugin = p;
        Bukkit.getPluginManager().registerEvents(new Handler(), p);
    }

    public static void openView(Player player, Kit kit) {
        open(player, kit, false);
    }

    public static void openEdit(Player player, Kit kit) {
        open(player, kit, true);
    }

    private static void open(Player player, Kit kit, boolean edit) {
        Holder holder = new Holder(kit, edit);
        Inventory inv = new FInventory(holder, FInventorySize.SIZE_36, "Kit " + kit.getId()).getInventory();
        holder.inventory = inv;
        for (int i = 0; i < Math.min(kit.getItems().size(), 36); i++) inv.setItem(i, kit.getItems().get(i).clone());
        player.openInventory(inv);
    }

    private static final class Holder implements InventoryHolder {
        final Kit kit;
        final boolean edit;
        Inventory inventory;

        Holder(Kit kit, boolean edit) {
            this.kit = kit;
            this.edit = edit;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    private static final class Handler implements Listener {
        // edição por cópia: o jogador nunca perde nem ganha itens reais
        @EventHandler
        public void onClick(InventoryClickEvent e) {
            if (!(e.getInventory().getHolder() instanceof Holder holder)) return;
            e.setCancelled(true);
            if (!holder.edit) return;
            Inventory menu = e.getInventory();
            ItemStack clicked = e.getCurrentItem();
            if (e.getClickedInventory() == menu) {
                ItemStack cursor = e.getCursor();
                if (!cursor.getType().isAir()) {
                    menu.setItem(e.getSlot(), cursor.clone());
                } else if (clicked != null) {
                    menu.setItem(e.getSlot(), null);
                }
            } else if (e.getClickedInventory() != null && clicked != null && !clicked.getType().isAir()) {
                int free = menu.firstEmpty();
                if (free >= 0) menu.setItem(free, clicked.clone());
            }
        }

        @EventHandler
        public void onDrag(InventoryDragEvent e) {
            if (e.getInventory().getHolder() instanceof Holder) e.setCancelled(true);
        }

        @EventHandler
        public void onClose(InventoryCloseEvent e) {
            if (!(e.getInventory().getHolder() instanceof Holder holder) || !holder.edit) return;
            List<ItemStack> items = new ArrayList<>();
            for (ItemStack it : e.getInventory().getContents()) if (it != null && !it.getType().isAir()) items.add(it);
            holder.kit.setItems(items);
            plugin.service().save();
        }
    }
}
