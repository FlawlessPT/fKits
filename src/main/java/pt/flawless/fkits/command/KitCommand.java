package pt.flawless.fkits.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import pt.flawless.fkits.FKits;
import pt.flawless.fkits.menu.CategoriesMenu;
import pt.flawless.fkits.model.Kit;
import pt.flawless.fkits.service.KitService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class KitCommand implements CommandExecutor, TabCompleter {
    private final FKits plugin;

    public KitCommand(FKits plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            Player p = CommandUtil.requirePlayer(sender);
            if (p != null) CategoriesMenu.open(p);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "help" -> help(sender);
            case "reload" -> {
                if (!CommandUtil.requireAdmin(sender)) return true;
                plugin.service().reload();
                sender.sendMessage("§aKits recarregados.");
            }
            case "tempo", "time" -> tempo(sender, args);
            default -> give(sender, args);
        }
        return true;
    }

    private void help(CommandSender s) {
        boolean admin = s.hasPermission(CommandUtil.ADMIN);
        s.sendMessage("§e/kit §7- Abre o menu de kits");
        s.sendMessage(admin ? "§e/kit <kit> [jogador] §7- Recebe um kit ou dá-o a um jogador"
                : "§e/kit <kit> §7- Recebe um kit");
        s.sendMessage(admin ? "§e/kit tempo [kit] [jogador] §7- Mostra o delay restante dos kits"
                : "§e/kit tempo [kit] §7- Mostra o delay restante dos kits");
        if (admin) s.sendMessage("§e/kit reload §7- Recarrega o kits.yml");
        s.sendMessage("§e/kit help §7- Mostra esta lista");
    }

    private void give(CommandSender sender, String[] args) {
        if (args.length > 2) {
            sender.sendMessage(sender.hasPermission(CommandUtil.ADMIN) ? "§cUso: /kit <kit> [jogador]" : "§cUso: /kit <kit>");
            return;
        }
        Kit kit = CommandUtil.kitOrMessage(plugin, sender, args[0]);
        if (kit == null) return;
        if (args.length == 1) {
            Player p = CommandUtil.requirePlayer(sender);
            if (p == null) return;
            if (!plugin.service().canUse(p, kit)) {
                p.sendMessage("§cNão tens permissão para este kit.");
                return;
            }
            CommandUtil.claim(plugin, p, p, kit, false);
            return;
        }
        if (!CommandUtil.requireAdmin(sender)) return;
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage("§cO jogador " + args[1] + " não está online.");
            return;
        }
        CommandUtil.claim(plugin, sender, target, kit, true);
    }

    private void tempo(CommandSender sender, String[] args) {
        if (args.length > 3) {
            sender.sendMessage(sender.hasPermission(CommandUtil.ADMIN) ? "§cUso: /kit tempo [kit] [jogador]" : "§cUso: /kit tempo [kit]");
            return;
        }
        Player who;
        if (args.length == 3) {
            if (!CommandUtil.requireAdmin(sender)) return;
            who = Bukkit.getPlayerExact(args[2]);
            if (who == null) {
                sender.sendMessage("§cO jogador " + args[2] + " não está online.");
                return;
            }
        } else {
            who = CommandUtil.requirePlayer(sender);
            if (who == null) return;
        }
        Kit single = null;
        if (args.length >= 2) {
            single = CommandUtil.kitOrMessage(plugin, sender, args[1]);
            if (single == null) return;
        }
        final Kit only = single;
        KitService service = plugin.service();
        service.lastClaims(who.getUniqueId()).whenComplete((claims, err) -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (err != null) {
                sender.sendMessage("§cOcorreu um erro ao ler os delays.");
                return;
            }
            List<Kit> kits = new ArrayList<>();
            if (only != null) kits.add(only);
            else for (Kit k : service.kits()) if (service.canUse(who, k)) kits.add(k);
            if (kits.isEmpty()) {
                sender.sendMessage("§cNão há kits disponíveis.");
                return;
            }
            for (Kit k : kits) sender.sendMessage("§e" + k.getId() + " §7- " + state(service, who, k, claims));
        }));
    }

    private static String state(KitService service, Player who, Kit kit, Map<String, Long> claims) {
        Long last = claims.get(kit.getId());
        if (last == null || kit.getDelaySeconds() <= 0 || service.hasBypass(who)) return "§adisponível";
        long remaining = last + kit.getDelaySeconds() * 1000 - System.currentTimeMillis();
        return remaining > 0 ? "§cdisponível em " + KitService.formatDuration(remaining) : "§adisponível";
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        boolean admin = sender.hasPermission(CommandUtil.ADMIN);
        if (args.length == 1) {
            List<String> opts = new ArrayList<>(plugin.service().kits().stream().map(Kit::getId).toList());
            opts.add("tempo");
            opts.add("help");
            if (admin) opts.add("reload");
            return CommandUtil.filter(opts, args[0]);
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        boolean tempo = sub.equals("tempo") || sub.equals("time");
        if (args.length == 2) {
            if (tempo) return CommandUtil.kitIds(plugin, args[1]);
            if (admin && !sub.equals("help") && !sub.equals("reload")) return CommandUtil.onlinePlayers(args[1]);
        }
        if (args.length == 3 && tempo && admin) return CommandUtil.onlinePlayers(args[2]);
        return List.of();
    }
}
