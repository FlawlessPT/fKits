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
import pt.flawless.fkits.model.Kit;

import java.util.List;
import java.util.Map;

/** Mostra a grelha da categoria; clicar num slot livre move o kit para lá. */
public final class PositionMenu {
    private static FKits plugin;

    private PositionMenu() {}

    public static void register(FKits p) {
        plugin = p;
        Bukkit.getPluginManager().registerEvents(new Handler(), p);
    }

    /** Thread principal. */
    public static void open(Player player, Kit kit) {
        Holder holder = new Holder(kit);
        Inventory inv = new FInventory(holder, FInventorySize.values()[plugin.service().category(kit.getCategory()).getRows() - 1], "Posição " + kit.getId()).getInventory();
        holder.inventory = inv;
        for (Map.Entry<Integer, Kit> e : plugin.service().layout(kit.getCategory()).entrySet()) {
            Kit other = e.getValue();
            boolean self = other == kit;
            inv.setItem(e.getKey(), MenuFrame.withLore(other.getIcon(), MenuFrame.kitName(other),
                    List.of(self ? "§aPosição atual deste kit" : "§cPosição ocupada")));
        }
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
            if (e.getClickedInventory() != e.getInventory() || !(e.getWhoClicked() instanceof Player p)) return;
            int slot = e.getRawSlot();
            Kit kit = holder.kit;
            if (plugin.service().layout(kit.getCategory()).containsKey(slot)) return;
            kit.setPosition(slot % 9 + 1, slot / 9 + 1);
            plugin.service().save();
            EditKitMenu.open(p, kit);
        }

        @EventHandler
        public void onDrag(InventoryDragEvent e) {
            if (e.getInventory().getHolder() instanceof Holder) e.setCancelled(true);
        }
    }
}
