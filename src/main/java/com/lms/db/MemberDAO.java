package com.lms.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MemberDAO {
    private final Connection conn;

    public MemberDAO() {
        this.conn = DatabaseManager.getInstance().getConnection();
    }

    public record Member(int id, String memberCode, String fullName, String email, String phone, String address, boolean isActive, String joinedDate, String dob) {}

    public List<Member> getMembers(String search, Integer statusFilter, int offset, int limit) {
        List<Member> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM members WHERE 1=1 ");
        
        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (full_name LIKE ? OR email LIKE ? OR phone LIKE ? OR member_code LIKE ?) ");
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
                ps.setString(paramIndex++, pattern);
            }
            if (statusFilter != null) {
                ps.setInt(paramIndex++, statusFilter);
            }
            ps.setInt(paramIndex++, limit);
            ps.setInt(paramIndex, offset);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Member(
                        rs.getInt("id"),
                        rs.getString("member_code"),
                        rs.getString("full_name"),
                        rs.getString("email"),
                        rs.getString("phone"),
                        rs.getString("address"),
                        rs.getInt("is_active") == 1,
                        rs.getString("joined_date"),
                        rs.getString("dob")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public int getMembersCount(String search, Integer statusFilter) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM members WHERE 1=1 ");
        
        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (full_name LIKE ? OR email LIKE ? OR phone LIKE ? OR member_code LIKE ?) ");
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

    public boolean insertMember(String fullName, String email, String phone, String address, boolean isActive, String dob) {
        String memberCode = "MEM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String sql = "INSERT INTO members (member_code, full_name, email, phone, address, is_active, dob) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, memberCode);
            ps.setString(2, fullName.trim());
            ps.setString(3, email != null && !email.isBlank() ? email.trim() : null);
            ps.setString(4, phone != null && !phone.isBlank() ? phone.trim() : null);
            ps.setString(5, address != null && !address.isBlank() ? address.trim() : null);
            ps.setInt(6, isActive ? 1 : 0);
            ps.setString(7, dob != null && !dob.isBlank() ? dob : null);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateMember(int id, String fullName, String email, String phone, String address, boolean isActive, String dob) {
        String sql = "UPDATE members SET full_name=?, email=?, phone=?, address=?, is_active=?, dob=? WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, fullName.trim());
            ps.setString(2, email != null && !email.isBlank() ? email.trim() : null);
            ps.setString(3, phone != null && !phone.isBlank() ? phone.trim() : null);
            ps.setString(4, address != null && !address.isBlank() ? address.trim() : null);
            ps.setInt(5, isActive ? 1 : 0);
            ps.setString(6, dob != null && !dob.isBlank() ? dob : null);
            ps.setInt(7, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean toggleMemberStatus(int id, boolean isActive) {
        String sql = "UPDATE members SET is_active=? WHERE id=?";
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
