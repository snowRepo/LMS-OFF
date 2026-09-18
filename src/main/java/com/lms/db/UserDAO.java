package com.lms.db;

import org.mindrot.jbcrypt.BCrypt;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UserDAO {
    private final Connection conn;

    public UserDAO() {
        this.conn = DatabaseManager.getInstance().getConnection();
    }

    public record Staff(int id, String fullName, String username, String email, String phone, String dob, String role, boolean isActive, String createdAt) {}

    public List<Staff> getUsers(String search, Integer statusFilter, int offset, int limit) {
        List<Staff> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM users WHERE role = 'LIBRARIAN' ");
        
        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (full_name LIKE ? OR username LIKE ? OR email LIKE ?) ");
        }
        if (statusFilter != null) {
            sql.append("AND is_active = ? ");
        }
        
        sql.append("ORDER BY full_name ASC LIMIT ? OFFSET ?");
        
        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int paramIndex = 1;
            if (search != null && !search.trim().isEmpty()) {
                String pattern = "%" + search.trim() + "%";
                ps.setString(paramIndex++, pattern);
                ps.setString(paramIndex++, pattern);
                ps.setString(paramIndex++, pattern);
            }
            if (statusFilter != null) {
                ps.setInt(paramIndex++, statusFilter);
            }
            ps.setInt(paramIndex++, limit);
            ps.setInt(paramIndex, offset);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Staff(
                        rs.getInt("id"),
                        rs.getString("full_name"),
                        rs.getString("username"),
                        rs.getString("email"),
                        rs.getString("phone"),
                        rs.getString("dob"),
                        rs.getString("role"),
                        rs.getInt("is_active") == 1,
                        rs.getString("created_at")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public int getUsersCount(String search, Integer statusFilter) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM users WHERE role = 'LIBRARIAN' ");
        
        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (full_name LIKE ? OR username LIKE ? OR email LIKE ?) ");
        }
        if (statusFilter != null) {
            sql.append("AND is_active = ? ");
        }
        
        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int paramIndex = 1;
            if (search != null && !search.trim().isEmpty()) {
                String pattern = "%" + search.trim() + "%";
                ps.setString(paramIndex++, pattern);
                ps.setString(paramIndex++, pattern);
                ps.setString(paramIndex++, pattern);
            }
            if (statusFilter != null) {
                ps.setInt(paramIndex++, statusFilter);
            }
            
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public String insertUser(String fullName, String username, String email, String phone, String dob, String role) {
        String tempPassword = "Lib-" + UUID.randomUUID().toString().substring(0, 5);
        String passHash = BCrypt.hashpw(tempPassword, BCrypt.gensalt(12));
        String tempPinHash = BCrypt.hashpw("000000", BCrypt.gensalt(12)); // placeholder, changed on first login

        String sql = "INSERT INTO users (full_name, username, email, phone, dob, password_hash, pin_hash, role, must_change_password) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, fullName.trim());
            ps.setString(2, username.trim().toLowerCase());
            ps.setString(3, email != null && !email.isBlank() ? email.trim() : null);
            ps.setString(4, phone != null && !phone.isBlank() ? phone.trim() : null);
            ps.setString(5, dob != null && !dob.isBlank() ? dob : null);
            ps.setString(6, passHash);
            ps.setString(7, tempPinHash);
            ps.setString(8, role);
            if (ps.executeUpdate() > 0) {
                return tempPassword;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String resetPassword(int id) {
        String tempPassword = "Lib-" + UUID.randomUUID().toString().substring(0, 5);
        String passHash = BCrypt.hashpw(tempPassword, BCrypt.gensalt(12));
        
        String sql = "UPDATE users SET password_hash = ?, must_change_password = 1 WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, passHash);
            ps.setInt(2, id);
            if (ps.executeUpdate() > 0) {
                return tempPassword;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean toggleUserStatus(int id, boolean isActive) {
        String sql = "UPDATE users SET is_active = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, isActive ? 1 : 0);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
