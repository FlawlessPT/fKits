package pt.flawless.fkits.command;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pt.flawless.fapi.messages.Messages;
import pt.flawless.fkits.FKits;
import pt.flawless.fkits.config.KitsStore;
import pt.flawless.fkits.model.Kit;
import pt.flawless.fkits.model.KitCategory;
import pt.flawless.fkits.service.ClaimResult;
import pt.flawless.fkits.service.KitService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

final class CommandUtil {
    static final String ADMIN = "fkits.admin";

    private CommandUtil() {}

    /** Envia a recusa de permissão e devolve false se o remetente não for admin. */
    static boolean requireAdmin(CommandSender s) {
        if (s.hasPermission(ADMIN)) return true;
        Messages.noPermission(s);
        return false;
    }

    static Player requirePlayer(CommandSender s) {
        if (s instanceof Player p) return p;
        s.sendMessage("§cEste comando só pode ser usado por jogadores.");
        return null;
    }

    static boolean validId(CommandSender s, String id) {
        if (KitsStore.ID.matcher(id).matches()) return true;
        s.sendMessage("§cId inválido: usa 1 a 16 caracteres entre a-z, 0-9, _ e -.");
        return false;
    }

    static Kit kitOrMessage(FKits plugin, CommandSender s, String id) {
        Kit kit = plugin.service().kit(id.toLowerCase(Locale.ROOT));
        if (kit == null) s.sendMessage("§cO kit " + id + " não existe.");
        return kit;
    }

    static List<String> filter(Collection<String> options, String prefix) {
        String p = prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String o : options) if (o.toLowerCase(Locale.ROOT).startsWith(p)) out.add(o);
        return out;
    }

    static List<String> kitIds(FKits plugin, String prefix) {
        return filter(plugin.service().kits().stream().map(Kit::getId).toList(), prefix);
    }

    static List<String> categoryIds(FKits plugin, String prefix) {
        return filter(plugin.service().categories().stream().map(KitCategory::getId).toList(), prefix);
    }

    static List<String> onlinePlayers(String prefix) {
        return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), prefix);
    }

    /** O futuro do serviço completa na thread principal, mas o runTask garante-o também em erros. */
    static void claim(FKits plugin, CommandSender sender, Player target, Kit kit, boolean force) {
        plugin.service().claim(target, kit, force).whenComplete((r, err) ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (err != null) {
                        sender.sendMessage("§cOcorreu um erro ao dar o kit.");
                        return;
                    }
                    send(sender, target, kit, r);
                }));
    }

    private static void send(CommandSender sender, Player target, Kit kit, ClaimResult r) {
        boolean self = sender == target;
        switch (r.status()) {
            case OK -> {
                if (self) {
                    sender.sendMessage("§aRecebeste o kit " + kit.getId() + ".");
                } else {
                    sender.sendMessage("§aDeste o kit " + kit.getId() + " a " + target.getName() + ".");
                    target.sendMessage("§aRecebeste o kit " + kit.getId() + ".");
                }
            }
            case COOLDOWN -> sender.sendMessage("§cPodes receber o kit " + kit.getId() + " daqui a "
                    + KitService.formatDuration(r.remainingMillis()) + ".");
            case FULL -> sender.sendMessage(self ? "§cNão tens espaço no inventário para o kit."
                    : "§c" + target.getName() + " não tem espaço no inventário para o kit.");
            case BUSY -> { }
        }
    }
}
