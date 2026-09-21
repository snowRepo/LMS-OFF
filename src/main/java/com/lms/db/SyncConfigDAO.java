package com.lms.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SyncConfigDAO {
    private final Connection conn;

    public SyncConfigDAO() {
        this.conn = DatabaseManager.getInstance().getConnection();
    }

    public record SyncConfig(
            String dbType, String host, String port, String dbName, 
            String username, String password, String lastSyncTime) {}

    public SyncConfig getConfig() {
        String sql = "SELECT db_type, server_host, server_port, db_name, db_user, db_pass, last_sync_time FROM sync_config WHERE id = 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new SyncConfig(
                        rs.getString("db_type"),
                        rs.getString("server_host"),
                        rs.getString("server_port"),
                        rs.getString("db_name"),
                        rs.getString("db_user"),
                        rs.getString("db_pass"),
                        rs.getString("last_sync_time")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void saveConfig(String type, String host, String port, String dbName, String user, String pass) {
        String sql = "INSERT OR REPLACE INTO sync_config (id, db_type, server_host, server_port, db_name, db_user, db_pass) " +
                     "VALUES (1, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, type);
            ps.setString(2, host);
            ps.setString(3, port);
            ps.setString(4, dbName);
            ps.setString(5, user);
            ps.setString(6, pass);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save sync config", e);
        }
    }

    public void updateLastSyncTime(String timeStr) {
        String sql = "UPDATE sync_config SET last_sync_time = ? WHERE id = 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, timeStr);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
