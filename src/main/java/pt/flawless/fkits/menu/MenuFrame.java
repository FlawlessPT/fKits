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

    static ItemStack item(Material material, String name, List<String> lore) {
        return new FItemStack(material, 1, name).setLore(lore).createItem();
    }

    /** Clone de {@code base} com lore substituída e, se {@code name} não for nulo, novo nome. */
    static ItemStack withLore(ItemStack base, String name, List<String> lore) {
        ItemStack it = base.clone();
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            if (name != null) meta.setDisplayName(name);
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
