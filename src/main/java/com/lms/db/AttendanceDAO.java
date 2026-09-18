package com.lms.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AttendanceDAO {
    private final Connection conn;

    public AttendanceDAO() {
        this.conn = DatabaseManager.getInstance().getConnection();
    }

    public record AttendanceRecord(int id, int memberId, String memberName, String date, String checkIn, String checkOut) {}

    public List<AttendanceRecord> getAttendance(String searchName, LocalDate dateFilter, int offset, int limit) {
        List<AttendanceRecord> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT a.id, a.member_id, m.full_name, a.date, a.check_in, a.check_out " +
            "FROM attendance a " +
            "JOIN members m ON a.member_id = m.id " +
            "WHERE 1=1 "
        );
        
        if (searchName != null && !searchName.trim().isEmpty()) {
            sql.append("AND m.full_name LIKE ? ");
        }
        if (dateFilter != null) {
            sql.append("AND a.date = ? ");
        }
        
        sql.append("ORDER BY a.check_in DESC LIMIT ? OFFSET ?");
        
        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int paramIndex = 1;
            if (searchName != null && !searchName.trim().isEmpty()) {
                ps.setString(paramIndex++, "%" + searchName.trim() + "%");
            }
            if (dateFilter != null) {
                ps.setString(paramIndex++, dateFilter.toString());
            }
            ps.setInt(paramIndex++, limit);
            ps.setInt(paramIndex, offset);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new AttendanceRecord(
                        rs.getInt("id"),
                        rs.getInt("member_id"),
                        rs.getString("full_name"),
                        rs.getString("date"),
                        rs.getString("check_in"),
                        rs.getString("check_out")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public int getAttendanceCount(String searchName, LocalDate dateFilter) {
        StringBuilder sql = new StringBuilder(
            "SELECT COUNT(*) FROM attendance a " +
            "JOIN members m ON a.member_id = m.id " +
            "WHERE 1=1 "
        );
        
        if (searchName != null && !searchName.trim().isEmpty()) {
            sql.append("AND m.full_name LIKE ? ");
        }
        if (dateFilter != null) {
            sql.append("AND a.date = ? ");
        }
        
        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int paramIndex = 1;
            if (searchName != null && !searchName.trim().isEmpty()) {
                ps.setString(paramIndex++, "%" + searchName.trim() + "%");
            }
            if (dateFilter != null) {
                ps.setString(paramIndex++, dateFilter.toString());
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

    public boolean checkIn(int memberId) {
        String sql = "INSERT INTO attendance (member_id, date, check_in) VALUES (?, date('now', 'localtime'), datetime('now', 'localtime'))";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, memberId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean checkOut(int attendanceId) {
        String sql = "UPDATE attendance SET check_out = datetime('now', 'localtime') WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, attendanceId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
