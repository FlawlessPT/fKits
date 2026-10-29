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
        for (int i = 0; i < Math.min(kit.getItems().size(), 36); i++) {
            ItemStack it = kit.getItems().get(i);
            if (it != null) inv.setItem(i, it.clone());
        }
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
        // modo edição: baú normal, os itens movem-se de verdade e a posição de cada slot fica guardada
        @EventHandler
        public void onClick(InventoryClickEvent e) {
            if (e.getInventory().getHolder() instanceof Holder holder && !holder.edit) e.setCancelled(true);
        }

        @EventHandler
        public void onDrag(InventoryDragEvent e) {
            if (e.getInventory().getHolder() instanceof Holder holder && !holder.edit) e.setCancelled(true);
        }

        @EventHandler
        public void onClose(InventoryCloseEvent e) {
            if (!(e.getInventory().getHolder() instanceof Holder holder) || !holder.edit) return;
            // lista com nulos nos slots vazios, cortada no último item
            List<ItemStack> items = new ArrayList<>();
            for (ItemStack it : e.getInventory().getContents()) items.add(it == null || it.getType().isAir() ? null : it);
            while (!items.isEmpty() && items.get(items.size() - 1) == null) items.remove(items.size() - 1);
            holder.kit.setItems(items);
            plugin.service().save();
        }
    }
}
