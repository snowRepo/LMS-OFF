package com.lms.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CirculationDAO {
    private final Connection conn;

    public CirculationDAO() {
        this.conn = DatabaseManager.getInstance().getConnection();
    }

    public record CirculationRecord(int id, int bookId, int memberId, String bookTitle, String memberName, String borrowedAt, String dueDate, String returnedAt, String status) {}

    private void updateOverdueRecords() {
        String sql = "UPDATE borrow_records SET status = 'OVERDUE' WHERE status = 'BORROWED' AND due_date < date('now', 'localtime')";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<CirculationRecord> getCirculationRecords(String search, String statusFilter, int offset, int limit) {
        updateOverdueRecords();
        
        List<CirculationRecord> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT c.id, c.book_id, c.member_id, b.title, m.full_name, c.borrowed_at, c.due_date, c.returned_at, c.status " +
            "FROM borrow_records c " +
            "JOIN books b ON c.book_id = b.id " +
            "JOIN members m ON c.member_id = m.id " +
            "WHERE 1=1 "
        );
        
        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (b.title LIKE ? OR m.full_name LIKE ?) ");
        }
        if (statusFilter != null && !statusFilter.equals("All")) {
            sql.append("AND c.status = ? ");
        }
        
        sql.append("ORDER BY c.borrowed_at DESC LIMIT ? OFFSET ?");
        
        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int paramIndex = 1;
            if (search != null && !search.trim().isEmpty()) {
                String pattern = "%" + search.trim() + "%";
                ps.setString(paramIndex++, pattern);
                ps.setString(paramIndex++, pattern);
            }
            if (statusFilter != null && !statusFilter.equals("All")) {
                ps.setString(paramIndex++, statusFilter.toUpperCase());
            }
            ps.setInt(paramIndex++, limit);
            ps.setInt(paramIndex, offset);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new CirculationRecord(
                        rs.getInt("id"),
                        rs.getInt("book_id"),
                        rs.getInt("member_id"),
                        rs.getString("title"),
                        rs.getString("full_name"),
                        rs.getString("borrowed_at"),
                        rs.getString("due_date"),
                        rs.getString("returned_at"),
                        rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public int getCirculationCount(String search, String statusFilter) {
        StringBuilder sql = new StringBuilder(
            "SELECT COUNT(*) FROM borrow_records c " +
            "JOIN books b ON c.book_id = b.id " +
            "JOIN members m ON c.member_id = m.id " +
            "WHERE 1=1 "
        );
        
        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (b.title LIKE ? OR m.full_name LIKE ?) ");
        }
        if (statusFilter != null && !statusFilter.equals("All")) {
            sql.append("AND c.status = ? ");
        }
        
        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int paramIndex = 1;
            if (search != null && !search.trim().isEmpty()) {
                String pattern = "%" + search.trim() + "%";
                ps.setString(paramIndex++, pattern);
                ps.setString(paramIndex++, pattern);
            }
            if (statusFilter != null && !statusFilter.equals("All")) {
                ps.setString(paramIndex++, statusFilter.toUpperCase());
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

    public boolean issueBook(int bookId, int memberId, LocalDate dueDate) {
        try {
            conn.setAutoCommit(false);
            
            // Check if available
            String checkSql = "SELECT available_copies FROM books WHERE id = ?";
            try (PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
                checkPs.setInt(1, bookId);
                ResultSet rs = checkPs.executeQuery();
                if (rs.next() && rs.getInt("available_copies") <= 0) {
                    conn.rollback();
                    return false; // No copies available
                }
            }

            // Decrement
            String decSql = "UPDATE books SET available_copies = available_copies - 1 WHERE id = ?";
            try (PreparedStatement decPs = conn.prepareStatement(decSql)) {
                decPs.setInt(1, bookId);
                decPs.executeUpdate();
            }

            // Insert
            String insSql = "INSERT INTO borrow_records (book_id, member_id, due_date, status) VALUES (?, ?, ?, 'BORROWED')";
            try (PreparedStatement insPs = conn.prepareStatement(insSql)) {
                insPs.setInt(1, bookId);
                insPs.setInt(2, memberId);
                insPs.setString(3, dueDate.toString());
                insPs.executeUpdate();
            }

            conn.commit();
            ActivityLogDAO.log("BORROW_BOOK", "Issued book ID: " + bookId + " to member ID: " + memberId);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            try {
                conn.rollback();
            } catch (SQLException ex) {}
            return false;
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException ex) {}
        }
    }

    public boolean returnBook(int borrowId, int bookId) {
        try {
            conn.setAutoCommit(false);
            
            // Increment
            String incSql = "UPDATE books SET available_copies = MIN(available_copies + 1, total_copies) WHERE id = ?";
            try (PreparedStatement incPs = conn.prepareStatement(incSql)) {
                incPs.setInt(1, bookId);
                incPs.executeUpdate();
            }

            // Update record
            String upSql = "UPDATE borrow_records SET returned_at = datetime('now', 'localtime'), status = 'RETURNED' WHERE id = ?";
            try (PreparedStatement upPs = conn.prepareStatement(upSql)) {
                upPs.setInt(1, borrowId);
                upPs.executeUpdate();
            }

            conn.commit();
            ActivityLogDAO.log("RETURN_BOOK", "Returned book ID: " + bookId + " for record ID: " + borrowId);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            try {
                conn.rollback();
            } catch (SQLException ex) {}
            return false;
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException ex) {}
        }
    }
}
