package com.lms.auth;

import com.lms.db.DatabaseManager;
import com.lms.model.User;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.Optional;

/**
 * Handles all authentication-related database operations.
 */
public class AuthService {

    private final Connection conn;

    public AuthService() {
        this.conn = DatabaseManager.getInstance().getConnection();
    }

    /**
     * Returns the total number of system users (staff accounts).
     * Used to determine whether to show Register or Login on first launch.
     */
    public int countUsers() {
        try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM users")) {
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to count users", e);
        }
    }

    /**
     * Registers a new system user.
     *
     * @param fullName display name
     * @param username unique login username
     * @param password plain-text password (will be hashed)
     * @param pin      6-digit PIN (will be hashed)
     * @param role     "ADMIN" or "LIBRARIAN"
     * @return true if registration succeeded
     */
    public boolean register(String fullName, String username, String password, String pin, String role) {
        String passHash = BCrypt.hashpw(password, BCrypt.gensalt(12));
        String pinHash  = BCrypt.hashpw(pin, BCrypt.gensalt(12));
        String sql  = "INSERT INTO users (full_name, username, password_hash, pin_hash, role) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, fullName.trim());
            ps.setString(2, username.trim().toLowerCase());
            ps.setString(3, passHash);
            ps.setString(4, pinHash);
            ps.setString(5, role);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            if (e.getMessage().contains("UNIQUE")) {
                return false; // duplicate username
            }
            throw new RuntimeException("Registration failed: " + e.getMessage(), e);
        }
    }

    /**
     * Authenticates a user by username and password.
     */
    public Optional<User> login(String username, String password) {
        String sql = "SELECT id, full_name, username, password_hash, role, must_change_password, is_active FROM users WHERE username = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username.trim().toLowerCase());
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                if (rs.getInt("is_active") == 0) {
                    return Optional.empty(); // Account deactivated
                }
                String hash = rs.getString("password_hash");
                if (BCrypt.checkpw(password, hash)) {
                    User user = new User(
                            rs.getInt("id"),
                            rs.getString("full_name"),
                            rs.getString("username"),
                            rs.getString("role"),
                            rs.getInt("must_change_password") == 1
                    );
                    return Optional.of(user);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Login failed", e);
        }
        return Optional.empty();
    }

    /**
     * Checks if a user with the given username exists.
     */
    public boolean checkUsernameExists(String username) {
        String sql = "SELECT 1 FROM users WHERE username = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username.trim().toLowerCase());
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check username", e);
        }
    }

    /**
     * Verifies the 6-digit PIN for a specific user.
     */
    public boolean verifyPin(String username, String pin) {
        String sql = "SELECT pin_hash FROM users WHERE username = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username.trim().toLowerCase());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String hash = rs.getString("pin_hash");
                return BCrypt.checkpw(pin, hash);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to verify PIN", e);
        }
        return false;
    }

    /**
     * Updates the password for a given user.
     */
    public void updatePassword(String username, String newPassword) {
        String passHash = BCrypt.hashpw(newPassword, BCrypt.gensalt(12));
        String sql = "UPDATE users SET password_hash = ? WHERE username = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, passHash);
            ps.setString(2, username.trim().toLowerCase());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update password", e);
        }
    }

    /**
     * Force changes password and PIN on first login, and clears the must_change flag.
     */
    public void forceChangePasswordAndPin(String username, String newPassword, String newPin) {
        String passHash = BCrypt.hashpw(newPassword, BCrypt.gensalt(12));
        String pinHash  = BCrypt.hashpw(newPin, BCrypt.gensalt(12));
        String sql = "UPDATE users SET password_hash = ?, pin_hash = ?, must_change_password = 0 WHERE username = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, passHash);
            ps.setString(2, pinHash);
            ps.setString(3, username.trim().toLowerCase());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update password and PIN", e);
        }
    }

    /**
     * Fetches the first admin user in the system (useful for auto-login after initial setup).
     */
    public Optional<User> getFirstAdmin() {
        String sql = "SELECT id, full_name, username, role, must_change_password FROM users WHERE role = 'ADMIN' LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                User user = new User(
                        rs.getInt("id"),
                        rs.getString("full_name"),
                        rs.getString("username"),
                        rs.getString("role"),
                        rs.getInt("must_change_password") == 1
                );
                return Optional.of(user);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch admin", e);
        }
        return Optional.empty();
    }
}
