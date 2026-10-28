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
import pt.flawless.fapi.enums.FInventorySize;
import pt.flawless.fapi.inventory.FInventory;
import pt.flawless.fkits.FKits;
import pt.flawless.fkits.model.Kit;

import java.util.List;

public final class CooldownMenu {
    private static final int VALUE_SLOT = 13;
    private static final int BACK_SLOT = 22;
    // slot -> segundos a somar
    private static final long[][] STEPS = {{10, -86400}, {11, -3600}, {12, -60}, {14, 60}, {15, 3600}, {16, 86400}};
    private static final String[] LABELS = {"1 dia", "1 hora", "1 minuto"};
    private static FKits plugin;

    private CooldownMenu() {}

    public static void register(FKits p) {
        plugin = p;
        Bukkit.getPluginManager().registerEvents(new Handler(), p);
    }

    /** Thread principal. */
    public static void open(Player player, Kit kit) {
        Holder holder = new Holder(kit);
        Inventory inv = new FInventory(holder, FInventorySize.SIZE_27, "Cooldown " + kit.getId()).getInventory();
        holder.inventory = inv;
        for (long[] s : STEPS) {
            boolean minus = s[1] < 0;
            String label = LABELS[(int) (minus ? 2 - (s[0] - 10) : s[0] - 14)];
            inv.setItem((int) s[0], MenuFrame.item(minus ? Material.RED_CONCRETE : Material.LIME_CONCRETE, (minus ? "§c-" : "§a+") + label,
                    List.of("§7Altera o cooldown do kit", "", "§7Clica para " + (minus ? "diminuir" : "aumentar"))));
        }
        inv.setItem(BACK_SLOT, MenuFrame.item(Material.ARROW, "§eVoltar",
                List.of("§7Volta à edição do kit", "", "§7Clica para voltar")));
        refresh(inv, kit);
        player.openInventory(inv);
    }

    private static void refresh(Inventory inv, Kit kit) {
        inv.setItem(VALUE_SLOT, MenuFrame.item(Material.CLOCK, "§e" + EditKitMenu.delayText(kit),
                List.of("§7Cooldown atual do kit", "", "§7Usa os botões para alterar")));
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
            if (slot == BACK_SLOT) {
                EditKitMenu.open(p, holder.kit);
                return;
            }
            for (long[] s : STEPS) {
                if (s[0] != slot) continue;
                holder.kit.setDelaySeconds(Math.max(0, holder.kit.getDelaySeconds() + s[1]));
                plugin.service().save();
                refresh(e.getInventory(), holder.kit);
                return;
            }
        }

        @EventHandler
        public void onDrag(InventoryDragEvent e) {
            if (e.getInventory().getHolder() instanceof Holder) e.setCancelled(true);
        }
    }
}
