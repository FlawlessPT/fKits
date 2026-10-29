package pt.flawless.fkits.model;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class Kit {
    private final String id;
    private String category;
    private long delaySeconds;
    /** Posição no menu, 1-based; 0 = sem posição (ocupa o primeiro slot livre). */
    private int column;
    private int row;
    private ItemStack icon;
    private List<ItemStack> items = new ArrayList<>();

    public Kit(String id, String category, long delaySeconds, ItemStack icon) {
        this.id = id;
        this.category = category;
        this.delaySeconds = delaySeconds;
        this.icon = icon;
    }

    public String getId() { return id; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public long getDelaySeconds() { return delaySeconds; }
    public void setDelaySeconds(long delaySeconds) { this.delaySeconds = delaySeconds; }
    public int getColumn() { return column; }
    public int getRow() { return row; }
    public void setPosition(int column, int row) { this.column = column; this.row = row; }
    public ItemStack getIcon() { return icon; }
    public void setIcon(ItemStack icon) { this.icon = icon; }
    public List<ItemStack> getItems() { return items; }
    public void setItems(List<ItemStack> items) { this.items = items; }
}
