package pt.flawless.fkits;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;
import pt.flawless.fkits.command.CreateKitCommand;
import pt.flawless.fkits.command.DelKitCommand;
import pt.flawless.fkits.command.EditKitCommand;
import pt.flawless.fkits.command.KitCategoryCommand;
import pt.flawless.fkits.command.KitCommand;
import pt.flawless.fkits.command.ResetKitDelayCommand;
import pt.flawless.fkits.command.ViewKitCommand;
import pt.flawless.fapi.database.DatabaseService;
import pt.flawless.fapi.database.Databases;
import pt.flawless.fapi.enums.EMessageTypeEnum;
import pt.flawless.fapi.logs.FConsoleLogger;
import pt.flawless.fapi.reload.Reloads;
import pt.flawless.fkits.config.KitsStore;
import pt.flawless.fkits.menu.CategoriesMenu;
import pt.flawless.fkits.listeners.CooldownPrompt;
import pt.flawless.fkits.menu.EditKitMenu;
import pt.flawless.fkits.menu.IconPickMenu;
import pt.flawless.fkits.menu.KitItemsMenu;
import pt.flawless.fkits.menu.KitsMenu;
import pt.flawless.fkits.menu.PositionMenu;
import pt.flawless.fkits.service.KitService;
import pt.flawless.fkits.storage.SqlCooldownRepository;

import java.sql.SQLException;
import java.util.List;

public class FKits extends JavaPlugin {
    private final FConsoleLogger consoleLogger = new FConsoleLogger(this.getName());
    private boolean enabledOk = false;
    private KitService service;

    @Override
    public void onEnable() {
        try {
            DatabaseService database = Databases.get();
            database.migrate(this, List.of("db/fkits/V001__create_cooldowns.sql"));
            service = new KitService(this, new KitsStore(this), database, new SqlCooldownRepository());
            Reloads.register(this, service::reload);
        } catch (SQLException | RuntimeException e) {
            // inclui DatabaseUnavailableException
            consoleLogger.sendMessage("não foi possível iniciar os kits: " + e.getMessage(), EMessageTypeEnum.ERROR);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        CategoriesMenu.register(this);
        KitsMenu.register(this);
        KitItemsMenu.register(this);
        EditKitMenu.register(this);
        IconPickMenu.register(this);
        CooldownPrompt.register(this);
        PositionMenu.register(this);
        register("kit", new KitCommand(this));
        register("criarkit", new CreateKitCommand(this));
        register("editarkit", new EditKitCommand(this));
        register("delkit", new DelKitCommand(this));
        register("verkit", new ViewKitCommand(this));
        register("resetkitdelay", new ResetKitDelayCommand(this));
        register("kitcategoria", new KitCategoryCommand(this));
        enabledOk = true;
        consoleLogger.sendEnablePluginMessage();
    }

    private <T extends CommandExecutor & TabCompleter> void register(String name, T handler) {
        PluginCommand command = getCommand(name);
        command.setExecutor(handler);
        command.setTabCompleter(handler);
    }

    public KitService service() {
        return service;
    }

    @Override
    public void onDisable() {
        if (enabledOk) consoleLogger.sendDisablePluginMessage();
    }
}
