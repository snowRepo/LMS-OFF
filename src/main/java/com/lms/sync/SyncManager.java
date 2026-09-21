package com.lms.sync;

import com.lms.db.DatabaseManager;
import com.lms.db.SyncConfigDAO;
import javafx.application.Platform;

import java.sql.*;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

public class SyncManager {

    private final SyncConfigDAO configDao;

    public SyncManager() {
        this.configDao = new SyncConfigDAO();
    }

    public boolean testConnection(String type, String host, String port, String dbName, String user, String pass) {
        String url = buildJdbcUrl(type, host, port, dbName);
        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public Connection getRemoteConnection() throws SQLException {
        SyncConfigDAO.SyncConfig config = configDao.getConfig();
        if (config == null || config.dbType() == null || config.host() == null) return null;
        
        String url = buildJdbcUrl(config.dbType(), config.host(), config.port(), config.dbName());
        return DriverManager.getConnection(url, config.username(), config.password());
    }

    private String buildJdbcUrl(String type, String host, String port, String dbName) {
        if ("MySQL".equalsIgnoreCase(type) || "MariaDB".equalsIgnoreCase(type)) {
            return String.format("jdbc:mysql://%s:%s/%s?useSSL=false&allowPublicKeyRetrieval=true", host, port, dbName);
        } else {
            // PostgreSQL
            return String.format("jdbc:postgresql://%s:%s/%s", host, port, dbName);
        }
    }

    public void initRemoteSchema(Connection cloud, String dbType) throws SQLException {
        boolean isPostgres = "PostgreSQL".equalsIgnoreCase(dbType);
        String autoInc = isPostgres ? "SERIAL PRIMARY KEY" : "INTEGER PRIMARY KEY AUTO_INCREMENT";
        String datetime = isPostgres ? "TIMESTAMP" : "DATETIME";
        String integer = "INTEGER";
        String text = isPostgres ? "TEXT" : "VARCHAR(255)";
        
        try (Statement stmt = cloud.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS users (id " + integer + " PRIMARY KEY, full_name " + text + ", username " + text + ", email " + text + ", phone " + text + ", dob " + text + ", password_hash " + text + ", pin_hash " + text + ", role " + text + ", is_active " + integer + ", must_change_password " + integer + ", created_at " + datetime + ", updated_at " + datetime + ")");
            stmt.execute("CREATE TABLE IF NOT EXISTS members (id " + integer + " PRIMARY KEY, member_code " + text + ", full_name " + text + ", email " + text + ", phone " + text + ", address " + text + ", dob " + text + ", is_active " + integer + ", joined_date " + text + ", created_at " + datetime + ", updated_at " + datetime + ")");
            stmt.execute("CREATE TABLE IF NOT EXISTS categories (id " + integer + " PRIMARY KEY, name " + text + ", description " + text + ", is_active " + integer + ", created_at " + datetime + ", updated_at " + datetime + ")");
            stmt.execute("CREATE TABLE IF NOT EXISTS books (id " + integer + " PRIMARY KEY, title " + text + ", author " + text + ", isbn " + text + ", category_id " + integer + ", total_copies " + integer + ", available_copies " + integer + ", published_year " + integer + ", description " + text + ", created_at " + datetime + ", updated_at " + datetime + ")");
            stmt.execute("CREATE TABLE IF NOT EXISTS borrow_records (id " + integer + " PRIMARY KEY, book_id " + integer + ", member_id " + integer + ", borrowed_at " + datetime + ", due_date " + text + ", returned_at " + datetime + ", status " + text + ", updated_at " + datetime + ")");
            stmt.execute("CREATE TABLE IF NOT EXISTS attendance (id " + integer + " PRIMARY KEY, member_id " + integer + ", date " + text + ", check_in " + datetime + ", check_out " + datetime + ", updated_at " + datetime + ")");
        }
    }

    public void sync(Runnable onSuccess, Runnable onFailure) {
        new Thread(() -> {
            try {
                Connection cloud = getRemoteConnection();
                if (cloud == null) {
                    if (onFailure != null) Platform.runLater(onFailure);
                    return;
                }
                
                Connection local = DatabaseManager.getInstance().getConnection();
                SyncConfigDAO.SyncConfig config = configDao.getConfig();

                cloud.setAutoCommit(false);
                try {
                    initRemoteSchema(cloud, config.dbType());
                    pushLocalChanges(local, cloud);
                    pullRemoteChanges(local, cloud, config.lastSyncTime());
                    
                    cloud.commit();
                    
                    String now = Instant.now().toString().replace("T", " ").split("\\.")[0];
                    configDao.updateLastSyncTime(now);
                    
                    if (onSuccess != null) Platform.runLater(onSuccess);
                } catch (SQLException e) {
                    cloud.rollback();
                    e.printStackTrace();
                    if (onFailure != null) Platform.runLater(onFailure);
                } finally {
                    cloud.setAutoCommit(true);
                    cloud.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (onFailure != null) Platform.runLater(onFailure);
            }
        }).start();
    }

    private void pushLocalChanges(Connection local, Connection cloud) throws SQLException {
        String sql = "SELECT id, table_name, record_id, operation FROM sync_log WHERE synced = 0 ORDER BY id ASC";
        try (Statement st = local.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            
            while (rs.next()) {
                long logId = rs.getLong("id");
                String table = rs.getString("table_name");
                String recordId = rs.getString("record_id");
                String op = rs.getString("operation");

                if ("DELETE".equals(op)) {
                    deleteOnCloud(cloud, table, recordId);
                } else {
                    upsertToCloud(local, cloud, table, recordId);
                }
                
                markSyncLogDone(local, logId);
            }
        }
    }

    private void upsertToCloud(Connection local, Connection cloud, String table, String recordId) throws SQLException {
        String selectLocal = "SELECT * FROM " + table + " WHERE id = ?";
        try (PreparedStatement psLocal = local.prepareStatement(selectLocal)) {
            psLocal.setString(1, recordId);
            try (ResultSet rsLocal = psLocal.executeQuery()) {
                if (!rsLocal.next()) return;

                ResultSetMetaData meta = rsLocal.getMetaData();
                int cols = meta.getColumnCount();

                // Check if exists remotely
                boolean existsRemotely = false;
                try (PreparedStatement check = cloud.prepareStatement("SELECT 1 FROM " + table + " WHERE id = ?")) {
                    check.setString(1, recordId);
                    try (ResultSet rsCheck = check.executeQuery()) {
                        existsRemotely = rsCheck.next();
                    }
                }

                if (existsRemotely) {
                    // UPDATE
                    StringBuilder updateStr = new StringBuilder("UPDATE " + table + " SET ");
                    for (int i = 1; i <= cols; i++) {
                        String col = meta.getColumnName(i);
                        if (!"id".equalsIgnoreCase(col)) {
                            updateStr.append(col).append(" = ?");
                            if (i < cols) updateStr.append(", ");
                        }
                    }
                    // Handle trailing comma if id was last (rare)
                    if (updateStr.toString().endsWith(", ")) {
                        updateStr.setLength(updateStr.length() - 2);
                    }
                    updateStr.append(" WHERE id = ?");

                    try (PreparedStatement psUpdate = cloud.prepareStatement(updateStr.toString())) {
                        int paramIdx = 1;
                        for (int i = 1; i <= cols; i++) {
                            if (!"id".equalsIgnoreCase(meta.getColumnName(i))) {
                                psUpdate.setObject(paramIdx++, rsLocal.getObject(i));
                            }
                        }
                        psUpdate.setString(paramIdx, recordId);
                        psUpdate.executeUpdate();
                    }
                } else {
                    // INSERT
                    StringBuilder colsStr = new StringBuilder();
                    StringBuilder valsStr = new StringBuilder();
                    for (int i = 1; i <= cols; i++) {
                        colsStr.append(meta.getColumnName(i));
                        valsStr.append("?");
                        if (i < cols) {
                            colsStr.append(", ");
                            valsStr.append(", ");
                        }
                    }
                    String insertStr = "INSERT INTO " + table + " (" + colsStr + ") VALUES (" + valsStr + ")";
                    try (PreparedStatement psInsert = cloud.prepareStatement(insertStr)) {
                        for (int i = 1; i <= cols; i++) {
                            psInsert.setObject(i, rsLocal.getObject(i));
                        }
                        psInsert.executeUpdate();
                    }
                }
            }
        }
    }

    private void deleteOnCloud(Connection cloud, String table, String recordId) throws SQLException {
        try (PreparedStatement ps = cloud.prepareStatement("DELETE FROM " + table + " WHERE id = ?")) {
            ps.setString(1, recordId);
            ps.executeUpdate();
        }
    }

    private void markSyncLogDone(Connection local, long logId) throws SQLException {
        try (PreparedStatement ps = local.prepareStatement("UPDATE sync_log SET synced = 1 WHERE id = ?")) {
            ps.setLong(1, logId);
            ps.executeUpdate();
        }
    }

    private void pullRemoteChanges(Connection local, Connection cloud, String lastSyncTime) throws SQLException {
        if (lastSyncTime == null) lastSyncTime = "1970-01-01 00:00:00";
        String[] tables = {"users", "members", "categories", "books", "borrow_records", "attendance"};
        
        for (String table : tables) {
            String query = "SELECT * FROM " + table + " WHERE updated_at > '" + lastSyncTime + "'";
            try (Statement st = cloud.createStatement();
                 ResultSet rs = st.executeQuery(query)) {
                
                ResultSetMetaData meta = rs.getMetaData();
                int cols = meta.getColumnCount();

                while (rs.next()) {
                    StringBuilder colsStr = new StringBuilder();
                    StringBuilder valsStr = new StringBuilder();
                    Object[] values = new Object[cols];

                    for (int i = 1; i <= cols; i++) {
                        values[i - 1] = rs.getObject(i);
                        colsStr.append(meta.getColumnName(i));
                        valsStr.append("?");
                        if (i < cols) {
                            colsStr.append(", ");
                            valsStr.append(", ");
                        }
                    }

                    String upsert = String.format("INSERT OR REPLACE INTO %s (%s) VALUES (%s)", table, colsStr, valsStr);
                    try (PreparedStatement ps = local.prepareStatement(upsert)) {
                        for (int i = 0; i < values.length; i++) ps.setObject(i + 1, values[i]);
                        ps.executeUpdate();
                    }
                }
            }
        }
    }
}
