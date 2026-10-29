package pt.flawless.fkits.model;

import org.bukkit.inventory.ItemStack;

public class KitCategory {
    private final String id;
    private String name;
    private ItemStack icon;
    private int rows = 4;

    public KitCategory(String id, String name, ItemStack icon) {
        this.id = id;
        this.name = name;
        this.icon = icon;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public ItemStack getIcon() { return icon; }
    public void setIcon(ItemStack icon) { this.icon = icon; }
    public int getRows() { return rows; }
    public void setRows(int rows) { this.rows = Math.max(1, Math.min(5, rows)); }
}
