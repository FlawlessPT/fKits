package pt.flawless.fkits;

import org.bukkit.plugin.java.JavaPlugin;
import pt.flawless.fapi.database.DatabaseService;
import pt.flawless.fapi.database.Databases;
import pt.flawless.fapi.enums.EMessageTypeEnum;
import pt.flawless.fapi.logs.FConsoleLogger;
import pt.flawless.fapi.reload.Reloads;
import pt.flawless.fkits.config.KitsStore;
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
        enabledOk = true;
        consoleLogger.sendEnablePluginMessage();
    }

    public KitService service() {
        return service;
    }

    @Override
    public void onDisable() {
        if (enabledOk) consoleLogger.sendDisablePluginMessage();
    }
}
