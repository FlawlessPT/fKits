package pt.flawless.fkits.menu;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pt.flawless.fapi.items.FItemStack;

import java.util.List;

/** Utilitários partilhados pelos menus. */
final class MenuFrame {
    private MenuFrame() {}

    static String color(String s) {
        return s.replace('&', '§');
    }

    /** Nome do kit no menu: o definido (com &) ou o id. */
    static String kitName(pt.flawless.fkits.model.Kit kit) {
        return kit.getName() != null ? color(kit.getName()) : "§e" + kit.getId();
    }

    static ItemStack item(Material material, String name, List<String> lore) {
        return new FItemStack(material, 1, name).setLore(lore).createItem();
    }

    /** Item só com o material de {@code base}, a lore dada e, se {@code name} não for nulo, esse nome. */
    static ItemStack withLore(ItemStack base, String name, List<String> lore) {
        // só o material: nome, lore e encantamentos do item escolhido não passam para o menu
        ItemStack it = new ItemStack(base.getType());
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            if (name != null) meta.setDisplayName(name);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.values());
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }

    /** Moldura de vidro cinzento; só para menus de 54 slots. */
    static void frame(Inventory inv) {
        ItemStack glass = item(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        int size = inv.getSize();
        for (int i = 0; i < size; i++) {
            if (i < 9 || i >= size - 9 || i % 9 == 0 || i % 9 == 8) inv.setItem(i, glass);
        }
    }
}
