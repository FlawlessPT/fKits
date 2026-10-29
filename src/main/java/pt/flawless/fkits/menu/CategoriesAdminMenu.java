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
import pt.flawless.fapi.sounds.FSound;
import pt.flawless.fkits.FKits;
import pt.flawless.fkits.config.KitsStore;
import pt.flawless.fkits.listeners.ChatPrompt;
import pt.flawless.fkits.model.KitCategory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Lista as categorias para o admin: clicar edita, o botão de baixo cria uma nova. */
public final class CategoriesAdminMenu {
    private static final int MAX_CATEGORIES = 9;
    private static final int CREATE_SLOT = 22;
    private static FKits plugin;

    private CategoriesAdminMenu() {}

    public static void register(FKits p) {
        plugin = p;
        Bukkit.getPluginManager().registerEvents(new Handler(), p);
    }

    /** Thread principal. */
    public static void open(Player player) {
        Holder holder = new Holder();
        Inventory inv = new FInventory(holder, FInventorySize.SIZE_27, "Categorias de kits").getInventory();
        holder.inventory = inv;
        List<KitCategory> cats = new ArrayList<>(plugin.service().categories());
        int n = Math.min(cats.size(), MAX_CATEGORIES);
        int start = 9 + (9 - n) / 2;
        for (int i = 0; i < n; i++) {
            KitCategory c = cats.get(i);
            int slot = n == 3 ? 10 + i * 3 : start + i;
            inv.setItem(slot, MenuFrame.withLore(c.getIcon(), MenuFrame.color(c.getName()),
                    List.of("§7Id: §f" + c.getId(), "§7Kits: §f" + plugin.service().kitsIn(c.getId()).size(),
                            "§7Linhas: §f" + c.getRows(), "", "§7Clica para editar a categoria")));
            holder.slots.put(slot, c.getId());
        }
        inv.setItem(CREATE_SLOT, MenuFrame.item(Material.EMERALD, "§aCriar categoria",
                List.of("§7Máximo de " + MAX_CATEGORIES + " categorias", "", "§7Clica e escreve o id no chat")));
        player.openInventory(inv);
    }

    private static void askCreate(Player p) {
        if (plugin.service().categories().size() >= MAX_CATEGORIES) {
            p.sendMessage("§cJá existe o máximo de " + MAX_CATEGORIES + " categorias.");
            FSound.fail(p);
            return;
        }
        ChatPrompt.start(p, new String[] {"§eEscreve no chat o id da nova categoria (1 a 16 caracteres entre a-z, 0-9, _ e -)."},
                (player, msg) -> {
                    String id = msg.toLowerCase(Locale.ROOT);
                    if (!KitsStore.ID.matcher(id).matches()) {
                        player.sendMessage("§cId inválido: usa 1 a 16 caracteres entre a-z, 0-9, _ e -.");
                        FSound.fail(player);
                        return false;
                    }
                    if (plugin.service().category(id) != null) {
                        player.sendMessage("§cJá existe uma categoria com o id " + id + ".");
                        FSound.fail(player);
                        return false;
                    }
                    KitCategory c = plugin.service().createCategory(id, id);
                    player.sendMessage("§aCategoria " + id + " criada.");
                    FSound.success(player);
                    CategoryEditMenu.open(player, c);
                    return true;
                }, CategoriesAdminMenu::open);
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
            if (e.getRawSlot() == CREATE_SLOT) {
                askCreate(p);
                return;
            }
            KitCategory c = plugin.service().category(holder.slots.getOrDefault(e.getRawSlot(), ""));
            if (c != null) CategoryEditMenu.open(p, c);
        }

        @EventHandler
        public void onDrag(InventoryDragEvent e) {
            if (e.getInventory().getHolder() instanceof Holder) e.setCancelled(true);
        }
    }
}
