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
import pt.flawless.fkits.listeners.ChatPrompt;
import pt.flawless.fapi.sounds.FSound;
import pt.flawless.fkits.model.Kit;
import pt.flawless.fkits.model.KitCategory;
import pt.flawless.fkits.service.KitService;

import java.util.ArrayList;
import java.util.List;

public final class EditKitMenu {
    private static final int ICON_SLOT = 9;
    private static final int ITEMS_SLOT = 11;
    private static final int COOLDOWN_SLOT = 13;
    private static final int CATEGORY_SLOT = 15;
    private static final int POSITION_SLOT = 17;
    private static FKits plugin;

    private EditKitMenu() {}

    public static void register(FKits p) {
        plugin = p;
        Bukkit.getPluginManager().registerEvents(new Handler(), p);
    }

    /** Thread principal. */
    public static void open(Player player, Kit kit) {
        Holder holder = new Holder(kit);
        Inventory inv = new FInventory(holder, FInventorySize.SIZE_27, "Editar " + kit.getId()).getInventory();
        holder.inventory = inv;
        fill(inv, kit);
        player.openInventory(inv);
    }

    private static void fill(Inventory inv, Kit kit) {
        ItemStack icon = kit.getIcon();
        inv.setItem(ICON_SLOT, MenuFrame.withLore(icon, "§eItem no menu",
                List.of("§7Item que representa o kit no menu", "", "§7Clica para escolher o item")));
        inv.setItem(ITEMS_SLOT, MenuFrame.item(Material.CHEST, "§eItens do kit",
                List.of("§7Itens: §f" + kit.getItems().stream().filter(java.util.Objects::nonNull).count(), "", "§7Clica para editar os itens")));
        inv.setItem(COOLDOWN_SLOT, MenuFrame.item(Material.CLOCK, "§eCooldown",
                List.of("§7Atual: §f" + delayText(kit), "", "§7Clica e escreve o novo cooldown no chat")));
        inv.setItem(POSITION_SLOT, MenuFrame.item(Material.COMPASS, "§ePosição",
                List.of("§7Atual: §f" + positionText(plugin.service(), kit), "", "§7Clica para escolher a posição no menu")));
        KitCategory cat = plugin.service().category(kit.getCategory());
        ItemStack catIcon = cat != null ? cat.getIcon() : new ItemStack(Material.BOOKSHELF);
        inv.setItem(CATEGORY_SLOT, MenuFrame.withLore(catIcon, "§eCategoria",
                List.of("§7Atual: §f" + (cat != null ? MenuFrame.color(cat.getName()) : kit.getCategory()), "",
                        "§7Botão esquerdo: próxima categoria", "§7Botão direito: categoria anterior")));
    }

    static String positionText(KitService service, Kit kit) {
        for (var e : service.layout(kit.getCategory()).entrySet()) {
            if (e.getValue() == kit) return "coluna " + (e.getKey() % 9 + 1) + ", linha " + (e.getKey() / 9 + 1);
        }
        return "sem posição";
    }

    private static void askCooldown(Player p, Kit kit) {
        ChatPrompt.start(p, new String[] {
                "§eEscreve no chat o cooldown do kit §f" + kit.getId() + "§e (atual: §f" + delayText(kit) + "§e).",
                "§7Exemplos: §f1d§7, §f12h§7, §f1d 12h 30m§7, §f90s§7, §f0 §7(sem delay)."},
                (player, msg) -> {
                    long seconds = KitService.parseDuration(msg);
                    if (seconds < 0) {
                        player.sendMessage("§cCooldown inválido. Usa por exemplo §f1d 12h 30m §c(d, h, m, s), ou §f0 §cpara não ter delay.");
                        FSound.fail(player);
                        return false;
                    }
                    kit.setDelaySeconds(seconds);
                    plugin.service().save();
                    player.sendMessage("§aCooldown do kit §f" + kit.getId() + " §adefinido para §f" + delayText(kit) + "§a.");
                    FSound.success(player);
                    open(player, kit);
                    return true;
                }, player -> open(player, kit));
    }

    public static String delayText(Kit kit) {
        return kit.getDelaySeconds() > 0 ? KitService.formatDuration(kit.getDelaySeconds() * 1000) : "Sem delay";
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
            Kit kit = holder.kit;
            switch (e.getRawSlot()) {
                case ICON_SLOT -> IconPickMenu.open(p, kit);
                case ITEMS_SLOT -> KitItemsMenu.openEdit(p, kit);
                case COOLDOWN_SLOT -> askCooldown(p, kit);
                case POSITION_SLOT -> PositionMenu.open(p, kit);
                case CATEGORY_SLOT -> {
                    List<KitCategory> cats = new ArrayList<>(plugin.service().categories());
                    if (cats.isEmpty()) return;
                    int idx = 0;
                    for (int i = 0; i < cats.size(); i++) if (cats.get(i).getId().equals(kit.getCategory())) idx = i;
                    int next = Math.floorMod(idx + (e.isRightClick() ? -1 : 1), cats.size());
                    kit.setCategory(cats.get(next).getId());
                    plugin.service().save();
                    fill(e.getInventory(), kit);
                }
                default -> { }
            }
        }

        @EventHandler
        public void onDrag(InventoryDragEvent e) {
            if (e.getInventory().getHolder() instanceof Holder) e.setCancelled(true);
        }
    }
}
