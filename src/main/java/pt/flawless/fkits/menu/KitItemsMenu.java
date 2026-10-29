package pt.flawless.fkits.menu;

import org.bukkit.Bukkit;
import org.bukkit.Material;
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
    private static final int BACK_SLOT = 49;
    private static FKits plugin;

    private KitItemsMenu() {}

    public static void register(FKits p) {
        plugin = p;
        Bukkit.getPluginManager().registerEvents(new Handler(), p);
    }

    /** Preview aberto fora do menu de kits (ex.: /verkit): o botão de voltar fecha. */
    public static void openView(Player player, Kit kit) {
        open(player, kit, false, false);
    }

    public static void openEdit(Player player, Kit kit) {
        open(player, kit, true, false);
    }

    /** Preview aberto a partir do menu de kits: o botão de voltar regressa a ele. */
    public static void openViewFromKits(Player player, Kit kit) {
        open(player, kit, false, true);
    }

    private static void open(Player player, Kit kit, boolean edit, boolean fromKits) {
        Holder holder = new Holder(kit, edit, fromKits);
        Inventory inv = new FInventory(holder, edit ? FInventorySize.SIZE_36 : FInventorySize.SIZE_54, "Kit " + kit.getId()).getInventory();
        holder.inventory = inv;
        for (int i = 0; i < Math.min(kit.getItems().size(), 36); i++) {
            ItemStack it = kit.getItems().get(i);
            if (it != null) inv.setItem(i, it.clone());
        }
        if (!edit) {
            // 5.ª linha de vidro a separar os itens, 6.ª só com o botão de voltar
            ItemStack glass = MenuFrame.item(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
            for (int i = 36; i < 45; i++) inv.setItem(i, glass);
            inv.setItem(BACK_SLOT, MenuFrame.item(Material.ARROW, "§eVoltar",
                    List.of(fromKits ? "§7Volta ao menu de kits" : "§7Fecha o menu", "", fromKits ? "§7Clica para voltar" : "§7Clica para fechar")));
        }
        player.openInventory(inv);
    }

    private static final class Holder implements InventoryHolder {
        final Kit kit;
        final boolean edit;
        final boolean fromKits;
        Inventory inventory;

        Holder(Kit kit, boolean edit, boolean fromKits) {
            this.kit = kit;
            this.edit = edit;
            this.fromKits = fromKits;
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
            if (!(e.getInventory().getHolder() instanceof Holder holder) || holder.edit) return;
            e.setCancelled(true);
            if (e.getRawSlot() == BACK_SLOT && e.getClickedInventory() == e.getInventory() && e.getWhoClicked() instanceof Player p) {
                if (holder.fromKits) KitsMenu.open(p, holder.kit.getCategory());
                else p.closeInventory();
            }
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
