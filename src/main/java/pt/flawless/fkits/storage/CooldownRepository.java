package pt.flawless.fkits.storage;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

/** Síncrono: chamar dentro do {@code supplyAsync}/{@code runAsync} do DatabaseService, com a sua ligação. */
public interface CooldownRepository {
    Map<String, Long> findAll(Connection c, UUID player) throws SQLException;

    void upsert(Connection c, UUID player, String kit, long when) throws SQLException;

    void delete(Connection c, UUID player, String kit) throws SQLException;
}
