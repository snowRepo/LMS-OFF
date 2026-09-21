package com.lms.db;

import com.lms.auth.SessionManager;
import com.lms.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ActivityLogDAO {

    public record ActivityLog(
            int id,
            Integer userId,
            String username,
            String actionType,
            String description,
            String createdAt
    ) {}

    /**
     * Logs an action with the currently authenticated user.
     */
    public static void log(String actionType, String description) {
        User user = SessionManager.getCurrentUser();
        Integer userId = (user != null) ? user.id() : null;
        String username = (user != null) ? user.username() : "SYSTEM";
        
        String sql = "INSERT INTO activity_logs (user_id, username, action_type, description) VALUES (?, ?, ?, ?)";
        Connection conn = DatabaseManager.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            
            if (userId != null) {
                ps.setInt(1, userId);
            } else {
                ps.setNull(1, java.sql.Types.INTEGER);
            }
            ps.setString(2, username);
            ps.setString(3, actionType);
            ps.setString(4, description);
            
            ps.executeUpdate();
            
        } catch (SQLException e) {
            System.err.println("Failed to write to activity log: " + e.getMessage());
        }
    }

    /**
     * Specifically for logging events where a user is explicitly provided (e.g., login).
     */
    public static void log(User user, String actionType, String description) {
        Integer userId = (user != null) ? user.id() : null;
        String username = (user != null) ? user.username() : "SYSTEM";
        
        String sql = "INSERT INTO activity_logs (user_id, username, action_type, description) VALUES (?, ?, ?, ?)";
        Connection conn = DatabaseManager.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            
            if (userId != null) {
                ps.setInt(1, userId);
            } else {
                ps.setNull(1, java.sql.Types.INTEGER);
            }
            ps.setString(2, username);
            ps.setString(3, actionType);
            ps.setString(4, description);
            
            ps.executeUpdate();
            
        } catch (SQLException e) {
            System.err.println("Failed to write to activity log: " + e.getMessage());
        }
    }

    public List<ActivityLog> getLogs(String startDate, String endDate, int limit, int offset) {
        List<ActivityLog> logs = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM activity_logs WHERE 1=1");
        
        if (startDate != null) {
            sql.append(" AND date(created_at) >= date(?)");
        }
        if (endDate != null) {
            sql.append(" AND date(created_at) <= date(?)");
        }
        
        sql.append(" ORDER BY created_at DESC LIMIT ? OFFSET ?");
        
        Connection conn = DatabaseManager.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int paramIndex = 1;
            if (startDate != null) {
                ps.setString(paramIndex++, startDate);
            }
            if (endDate != null) {
                ps.setString(paramIndex++, endDate);
            }
            ps.setInt(paramIndex++, limit);
            ps.setInt(paramIndex, offset);
            
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                logs.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return logs;
    }
    
    public List<ActivityLog> searchLogs(String query, String startDate, String endDate, int limit, int offset) {
        List<ActivityLog> logs = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM activity_logs WHERE (username LIKE ? OR action_type LIKE ? OR description LIKE ?)");
        
        if (startDate != null) {
            sql.append(" AND date(created_at) >= date(?)");
        }
        if (endDate != null) {
            sql.append(" AND date(created_at) <= date(?)");
        }
        
        sql.append(" ORDER BY created_at DESC LIMIT ? OFFSET ?");
        
        Connection conn = DatabaseManager.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            String likeParam = "%" + query + "%";
            ps.setString(1, likeParam);
            ps.setString(2, likeParam);
            ps.setString(3, likeParam);
            
            int paramIndex = 4;
            if (startDate != null) {
                ps.setString(paramIndex++, startDate);
            }
            if (endDate != null) {
                ps.setString(paramIndex++, endDate);
            }
            ps.setInt(paramIndex++, limit);
            ps.setInt(paramIndex, offset);
            
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                logs.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return logs;
    }
    
    public int countLogs(String query, String startDate, String endDate) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM activity_logs WHERE 1=1");
        boolean hasQuery = query != null && !query.trim().isEmpty();
        if (hasQuery) {
            sql.append(" AND (username LIKE ? OR action_type LIKE ? OR description LIKE ?)");
        }
        
        if (startDate != null) {
            sql.append(" AND date(created_at) >= date(?)");
        }
        if (endDate != null) {
            sql.append(" AND date(created_at) <= date(?)");
        }
        
        Connection conn = DatabaseManager.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int paramIndex = 1;
            if (hasQuery) {
                String likeParam = "%" + query + "%";
                ps.setString(paramIndex++, likeParam);
                ps.setString(paramIndex++, likeParam);
                ps.setString(paramIndex++, likeParam);
            }
            if (startDate != null) {
                ps.setString(paramIndex++, startDate);
            }
            if (endDate != null) {
                ps.setString(paramIndex++, endDate);
            }
            
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private ActivityLog mapRow(ResultSet rs) throws SQLException {
        Integer userId = rs.getObject("user_id") != null ? rs.getInt("user_id") : null;
        return new ActivityLog(
                rs.getInt("id"),
                userId,
                rs.getString("username"),
                rs.getString("action_type"),
                rs.getString("description"),
                rs.getString("created_at")
        );
    }
}
