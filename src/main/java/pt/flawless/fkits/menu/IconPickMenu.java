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
import java.util.function.Consumer;

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
        open(player, "Ícone de " + kit.getId(), "o kit", icon -> {
            kit.setIcon(icon);
            plugin.service().save();
        }, p -> EditKitMenu.open(p, kit));
    }

    /** Thread principal. {@code onPick} corre ao escolher (o menu de destino abre-o ele); {@code onBack} no botão de voltar. */
    public static void open(Player player, String title, String target, Consumer<ItemStack> onPick, Consumer<Player> onBack) {
        Holder holder = new Holder(onPick, onBack);
        Inventory inv = new FInventory(holder, FInventorySize.SIZE_9, title).getInventory();
        holder.inventory = inv;
        inv.setItem(INFO_SLOT, MenuFrame.item(Material.PAPER, "§eClica num item do teu inventário",
                List.of("§7O item escolhido passa a ser o ícone de " + target, "§7no menu (é copiado, não o perdes)")));
        inv.setItem(BACK_SLOT, MenuFrame.item(Material.ARROW, "§eVoltar",
                List.of("§7Volta atrás", "", "§7Clica para voltar")));
        player.openInventory(inv);
    }

    private static final class Holder implements InventoryHolder {
        final Consumer<ItemStack> onPick;
        final Consumer<Player> onBack;
        Inventory inventory;

        Holder(Consumer<ItemStack> onPick, Consumer<Player> onBack) {
            this.onPick = onPick;
            this.onBack = onBack;
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
                if (e.getRawSlot() == BACK_SLOT) holder.onBack.accept(p);
                return;
            }
            ItemStack clicked = e.getCurrentItem();
            if (clicked == null || clicked.getType().isAir()) return;
            ItemStack icon = clicked.clone();
            icon.setAmount(1);
            holder.onPick.accept(icon);
            holder.onBack.accept(p);
        }

        @EventHandler
        public void onDrag(InventoryDragEvent e) {
            if (e.getInventory().getHolder() instanceof Holder) e.setCancelled(true);
        }
    }
}
