package pt.flawless.fkits.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SqlCooldownRepository implements CooldownRepository {

    @Override
    public Map<String, Long> findAll(Connection c, UUID player) throws SQLException {
        Map<String, Long> out = new HashMap<>();
        try (PreparedStatement ps = c.prepareStatement("SELECT kit, last_claim FROM fkits_cooldowns WHERE player_uuid = ?")) {
            ps.setString(1, player.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.put(rs.getString(1), rs.getLong(2));
            }
        }
        return out;
    }

    @Override
    public void upsert(Connection c, UUID player, String kit, long when) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO fkits_cooldowns (player_uuid, kit, last_claim) VALUES (?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE last_claim = VALUES(last_claim)")) {
            ps.setString(1, player.toString());
            ps.setString(2, kit);
            ps.setLong(3, when);
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(Connection c, UUID player, String kit) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM fkits_cooldowns WHERE player_uuid = ? AND kit = ?")) {
            ps.setString(1, player.toString());
            ps.setString(2, kit);
            ps.executeUpdate();
        }
    }
}
