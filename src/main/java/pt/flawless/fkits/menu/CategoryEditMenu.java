package pt.flawless.fkits.menu;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import pt.flawless.fapi.enums.FInventorySize;
import pt.flawless.fapi.inventory.ConfirmMenu;
import pt.flawless.fapi.inventory.FInventory;
import pt.flawless.fapi.sounds.FSound;
import pt.flawless.fkits.FKits;
import pt.flawless.fkits.listeners.ChatPrompt;
import pt.flawless.fkits.model.KitCategory;

import java.util.List;

/** Edição de uma categoria: ícone, nome, linhas do menu de kits e apagar. */
public final class CategoryEditMenu {
    private static final int ICON_SLOT = 10;
    private static final int NAME_SLOT = 12;
    private static final int ROWS_SLOT = 14;
    private static final int DELETE_SLOT = 16;
    private static final int BACK_SLOT = 22;
    private static FKits plugin;

    private CategoryEditMenu() {}

    public static void register(FKits p) {
        plugin = p;
        Bukkit.getPluginManager().registerEvents(new Handler(), p);
    }

    /** Thread principal. */
    public static void open(Player player, KitCategory category) {
        Holder holder = new Holder(category);
        Inventory inv = new FInventory(holder, FInventorySize.SIZE_27, "Categoria " + category.getId()).getInventory();
        holder.inventory = inv;
        fill(inv, category);
        player.openInventory(inv);
    }

    private static void fill(Inventory inv, KitCategory c) {
        inv.setItem(ICON_SLOT, MenuFrame.withLore(c.getIcon(), "§eÍcone",
                List.of("§7Item que representa a categoria no menu", "", "§7Clica para escolher um item do teu inventário")));
        inv.setItem(NAME_SLOT, MenuFrame.item(Material.NAME_TAG, "§eNome",
                List.of("§7Atual: §f" + MenuFrame.color(c.getName()), "", "§7Clica e escreve o novo nome no chat (aceita &)")));
        inv.setItem(ROWS_SLOT, MenuFrame.item(Material.LADDER, "§eLinhas do menu de kits",
                List.of("§7Atual: §f" + c.getRows() + " §7(máx. " + c.getRows() * 9 + " kits)", "",
                        "§7Botão esquerdo: mais uma linha", "§7Botão direito: menos uma linha")));
        inv.setItem(DELETE_SLOT, MenuFrame.item(Material.BARRIER, "§cApagar categoria",
                List.of("§7Só é possível se não tiver kits", "", "§7Clica para apagar")));
        inv.setItem(BACK_SLOT, MenuFrame.item(Material.ARROW, "§eVoltar",
                List.of("§7Volta à lista de categorias", "", "§7Clica para voltar")));
    }

    private static void askName(Player p, KitCategory c) {
        ChatPrompt.start(p, new String[] {"§eEscreve no chat o novo nome da categoria §f" + c.getId() + "§e (aceita cores com &)."},
                (player, msg) -> {
                    c.setName(msg);
                    plugin.service().save();
                    player.sendMessage("§aNome da categoria " + c.getId() + " atualizado.");
                    FSound.success(player);
                    open(player, c);
                    return true;
                }, player -> open(player, c));
    }

    private static void changeRows(Player p, KitCategory c, Inventory inv, boolean less) {
        int rows = c.getRows() + (less ? -1 : 1);
        if (rows < 1 || rows > 5) return;
        if (plugin.service().kitsIn(c.getId()).size() > rows * 9) {
            p.sendMessage("§cA categoria tem mais kits do que cabem em " + rows + " linhas.");
            FSound.fail(p);
            return;
        }
        c.setRows(rows);
        plugin.service().save();
        fill(inv, c);
    }

    private static void confirmDelete(Player p, KitCategory c) {
        if (!plugin.service().kitsIn(c.getId()).isEmpty()) {
            p.sendMessage("§cA categoria " + c.getId() + " ainda tem kits; apaga-os ou muda-os de categoria primeiro.");
            FSound.fail(p);
            return;
        }
        ConfirmMenu.open(p, "Apagar " + c.getId(), new ItemStack(c.getIcon().getType()),
                new ConfirmMenu.Button(Material.LIME_CONCRETE, "§aApagar",
                        List.of("§7Apaga a categoria " + c.getId(), "", "§7Clica para confirmar"), player -> {
                    if (plugin.service().category(c.getId()) == null || !plugin.service().kitsIn(c.getId()).isEmpty()) return;
                    plugin.service().deleteCategory(c.getId());
                    player.sendMessage("§aCategoria " + c.getId() + " apagada.");
                    CategoriesAdminMenu.open(player);
                }),
                new ConfirmMenu.Button(Material.RED_CONCRETE, "§cCancelar",
                        List.of("§7Mantém a categoria", "", "§7Clica para cancelar"), player -> open(player, c)));
    }

    private static final class Holder implements InventoryHolder {
        final KitCategory category;
        Inventory inventory;

        Holder(KitCategory category) {
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
            KitCategory c = holder.category;
            switch (e.getRawSlot()) {
                case ICON_SLOT -> IconPickMenu.open(p, "Ícone de " + c.getId(), "a categoria", icon -> {
                    c.setIcon(icon);
                    plugin.service().save();
                }, player -> open(player, c));
                case NAME_SLOT -> askName(p, c);
                case ROWS_SLOT -> changeRows(p, c, e.getInventory(), e.isRightClick());
                case DELETE_SLOT -> confirmDelete(p, c);
                case BACK_SLOT -> CategoriesAdminMenu.open(p);
                default -> { }
            }
        }

        @EventHandler
        public void onDrag(InventoryDragEvent e) {
            if (e.getInventory().getHolder() instanceof Holder) e.setCancelled(true);
        }
    }
}
