package pt.flawless.fkits.command;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pt.flawless.fapi.inventory.ConfirmMenu;
import pt.flawless.fkits.FKits;
import pt.flawless.fkits.model.KitCategory;
import pt.flawless.fkits.service.KitService;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class KitCategoryCommand implements CommandExecutor, TabCompleter {
    private static final int MAX_CATEGORIES = 9;
    private static final String GENERAL_USAGE = "§cUso: /kitcategoria <criar|editar|apagar|lista|help>";
    private final FKits plugin;

    public KitCategoryCommand(FKits plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!CommandUtil.requireAdmin(sender)) return true;
        if (args.length == 0) {
            sender.sendMessage(GENERAL_USAGE);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "criar", "create" -> create(sender, args);
            case "editar", "edit" -> edit(sender, args);
            case "apagar", "delete" -> delete(sender, args);
            case "lista", "list" -> list(sender);
            case "help" -> help(sender);
            default -> sender.sendMessage(GENERAL_USAGE);
        }
        return true;
    }

    private void help(CommandSender s) {
        s.sendMessage("§e/kitcategoria criar <id> [nome...] §7- Cria uma categoria");
        s.sendMessage("§e/kitcategoria editar <id> nome <texto...> §7- Muda o nome da categoria");
        s.sendMessage("§e/kitcategoria editar <id> icone §7- Usa o item na mão como ícone");
        s.sendMessage("§e/kitcategoria editar <id> linhas <1-6> §7- Muda as linhas do menu de kits");
        s.sendMessage("§e/kitcategoria apagar <id> §7- Apaga uma categoria sem kits");
        s.sendMessage("§e/kitcategoria lista §7- Lista as categorias");
        s.sendMessage("§e/kitcategoria help §7- Mostra esta lista");
    }

    private void create(CommandSender s, String[] args) {
        if (args.length < 2) {
            s.sendMessage("§cUso: /kitcategoria criar <id> [nome...]");
            return;
        }
        KitService service = plugin.service();
        String id = args[1].toLowerCase(Locale.ROOT);
        if (!CommandUtil.validId(s, id)) return;
        if (service.category(id) != null) {
            s.sendMessage("§cJá existe uma categoria com o id " + id + ".");
            return;
        }
        if (service.categories().size() >= MAX_CATEGORIES) {
            s.sendMessage("§cJá existe o máximo de " + MAX_CATEGORIES + " categorias.");
            return;
        }
        String name = args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : id;
        service.createCategory(id, name);
        s.sendMessage("§aCategoria " + id + " criada.");
    }

    private void edit(CommandSender s, String[] args) {
        if (args.length < 3) {
            s.sendMessage("§cUso: /kitcategoria editar <id> <nome|icone|linhas> ...");
            return;
        }
        KitCategory c = plugin.service().category(args[1].toLowerCase(Locale.ROOT));
        if (c == null) {
            s.sendMessage("§cA categoria " + args[1] + " não existe.");
            return;
        }
        String field = args[2].toLowerCase(Locale.ROOT);
        switch (field) {
            case "nome", "name" -> {
                if (args.length < 4) {
                    s.sendMessage("§cUso: /kitcategoria editar <id> nome <texto...>");
                    return;
                }
                c.setName(String.join(" ", Arrays.copyOfRange(args, 3, args.length)));
                plugin.service().save();
                s.sendMessage("§aNome da categoria " + c.getId() + " atualizado.");
            }
            case "linhas", "rows" -> {
                if (args.length != 4) {
                    s.sendMessage("§cUso: /kitcategoria editar <id> linhas <1-6>");
                    return;
                }
                int rows;
                try {
                    rows = Integer.parseInt(args[3]);
                } catch (NumberFormatException ex) {
                    rows = 0;
                }
                if (rows < 1 || rows > 6) {
                    s.sendMessage("§cAs linhas vão de 1 a 6.");
                    return;
                }
                if (plugin.service().kitsIn(c.getId()).size() > rows * 9) {
                    s.sendMessage("§cA categoria tem mais kits do que cabem em " + rows + " linhas.");
                    return;
                }
                c.setRows(rows);
                plugin.service().save();
                s.sendMessage("§aA categoria " + c.getId() + " passou a ter " + rows + " linhas.");
            }
            case "icone", "icon" -> {
                if (args.length != 3) {
                    s.sendMessage("§cUso: /kitcategoria editar <id> icone");
                    return;
                }
                Player p = CommandUtil.requirePlayer(s);
                if (p == null) return;
                ItemStack hand = p.getInventory().getItemInMainHand();
                if (hand.getType() == Material.AIR) {
                    p.sendMessage("§cSegura o item que queres como ícone.");
                    return;
                }
                ItemStack icon = hand.clone();
                icon.setAmount(1);
                c.setIcon(icon);
                plugin.service().save();
                p.sendMessage("§aÍcone da categoria " + c.getId() + " atualizado.");
            }
            default -> s.sendMessage("§cUso: /kitcategoria editar <id> <nome|icone|linhas> ...");
        }
    }

    private void delete(CommandSender s, String[] args) {
        if (args.length != 2) {
            s.sendMessage("§cUso: /kitcategoria apagar <id>");
            return;
        }
        Player p = CommandUtil.requirePlayer(s);
        if (p == null) return;
        KitService service = plugin.service();
        KitCategory c = service.category(args[1].toLowerCase(Locale.ROOT));
        if (c == null) {
            p.sendMessage("§cA categoria " + args[1] + " não existe.");
            return;
        }
        if (!service.kitsIn(c.getId()).isEmpty()) {
            p.sendMessage("§cA categoria " + c.getId() + " ainda tem kits; apaga-os ou muda-os de categoria primeiro.");
            return;
        }
        ConfirmMenu.open(p, "Apagar " + c.getId(), c.getIcon(),
                new ConfirmMenu.Button(Material.LIME_CONCRETE, "§aApagar",
                        List.of("§7Apaga a categoria " + c.getId(), "", "§7Clica para confirmar"), player -> {
                    if (service.category(c.getId()) == null || !service.kitsIn(c.getId()).isEmpty()) return;
                    service.deleteCategory(c.getId());
                    player.sendMessage("§aCategoria " + c.getId() + " apagada.");
                }),
                new ConfirmMenu.Button(Material.RED_CONCRETE, "§cCancelar",
                        List.of("§7Mantém a categoria", "", "§7Clica para cancelar"), null));
    }

    private void list(CommandSender s) {
        if (plugin.service().categories().isEmpty()) {
            s.sendMessage("§cNão há categorias.");
            return;
        }
        for (KitCategory c : plugin.service().categories()) {
            s.sendMessage("§e" + c.getId() + " §7- " + c.getName().replace('&', '§')
                    + " §7(" + plugin.service().kitsIn(c.getId()).size() + " kits)");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission(CommandUtil.ADMIN)) return List.of();
        if (args.length == 1) return CommandUtil.filter(List.of("criar", "editar", "apagar", "lista", "help"), args[0]);
        String sub = args[0].toLowerCase(Locale.ROOT);
        boolean edit = sub.equals("editar") || sub.equals("edit");
        if (args.length == 2 && (edit || sub.equals("apagar") || sub.equals("delete"))) return CommandUtil.categoryIds(plugin, args[1]);
        if (args.length == 3 && edit) return CommandUtil.filter(List.of("nome", "icone", "linhas"), args[2]);
        return List.of();
    }
}
