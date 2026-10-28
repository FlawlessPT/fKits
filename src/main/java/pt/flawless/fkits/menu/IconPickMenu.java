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

import java.util.List;

public final class IconPickMenu {
    private static final int INFO_SLOT = 4;
    private static final int BACK_SLOT = 8;
    private static FKits plugin;

    private IconPickMenu() {}

    public static void register(FKits p) {
        plugin = p;
        Bukkit.getPluginManager().registerEvents(new Handler(), p);
    }

    /** Thread principal. */
    public static void open(Player player, Kit kit) {
        Holder holder = new Holder(kit);
        Inventory inv = new FInventory(holder, FInventorySize.SIZE_9, "Ícone de " + kit.getId()).getInventory();
        holder.inventory = inv;
        inv.setItem(INFO_SLOT, MenuFrame.item(Material.PAPER, "§eClica num item do teu inventário",
                List.of("§7O item escolhido passa a ser o ícone do kit", "§7no menu (é copiado, não o perdes)")));
        inv.setItem(BACK_SLOT, MenuFrame.item(Material.ARROW, "§eVoltar",
                List.of("§7Volta à edição do kit", "", "§7Clica para voltar")));
        player.openInventory(inv);
    }

    private static final class Holder implements InventoryHolder {
        final Kit kit;
        Inventory inventory;

        Holder(Kit kit) {
            this.kit = kit;
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
            if (!(e.getWhoClicked() instanceof Player p) || e.getClickedInventory() == null) return;
            if (e.getClickedInventory() == e.getInventory()) {
                if (e.getRawSlot() == BACK_SLOT) EditKitMenu.open(p, holder.kit);
                return;
            }
            ItemStack clicked = e.getCurrentItem();
            if (clicked == null || clicked.getType().isAir()) return;
            ItemStack icon = clicked.clone();
            icon.setAmount(1);
            holder.kit.setIcon(icon);
            plugin.service().save();
            EditKitMenu.open(p, holder.kit);
        }

        @EventHandler
        public void onDrag(InventoryDragEvent e) {
            if (e.getInventory().getHolder() instanceof Holder) e.setCancelled(true);
        }
    }
}
