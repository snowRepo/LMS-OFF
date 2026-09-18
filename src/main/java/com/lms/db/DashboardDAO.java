package com.lms.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DashboardDAO {

    private final Connection conn;

    public DashboardDAO() {
        this.conn = DatabaseManager.getInstance().getConnection();
    }

    public record DashboardMetrics(int totalBooks, int totalMembers, int borrowedBooks, int overdueBooks) {}

    public record ActiveBorrowing(String memberName, String bookTitle, String borrowDate, String dueDate, String status) {}

    public DashboardMetrics getMetrics() {
        int books = 0, members = 0, borrowed = 0, overdue = 0;
        try {
            var rs1 = conn.createStatement().executeQuery("SELECT COUNT(*) FROM books");
            if (rs1.next()) books = rs1.getInt(1);

            var rs2 = conn.createStatement().executeQuery("SELECT COUNT(*) FROM members WHERE is_active = 1");
            if (rs2.next()) members = rs2.getInt(1);

            var rs3 = conn.createStatement().executeQuery("SELECT COUNT(*) FROM borrow_records WHERE status = 'BORROWED'");
            if (rs3.next()) borrowed = rs3.getInt(1);

            // Dynamically check overdue based on current date
            String today = LocalDate.now().toString();
            var rs4 = conn.prepareStatement("SELECT COUNT(*) FROM borrow_records WHERE status = 'BORROWED' AND due_date < ?");
            rs4.setString(1, today);
            var rs4Result = rs4.executeQuery();
            if (rs4Result.next()) overdue = rs4Result.getInt(1);
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new DashboardMetrics(books, members, borrowed, overdue);
    }

    public int getActiveBorrowingsCount() {
        try {
            var rs = conn.createStatement().executeQuery("SELECT COUNT(*) FROM borrow_records WHERE status != 'RETURNED'");
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public List<ActiveBorrowing> getActiveBorrowings(int offset, int limit) {
        List<ActiveBorrowing> list = new ArrayList<>();
        String sql = """
            SELECT m.full_name, b.title, r.borrowed_at, r.due_date, r.status
            FROM borrow_records r
            JOIN members m ON r.member_id = m.id
            JOIN books b ON r.book_id = b.id
            WHERE r.status != 'RETURNED'
            ORDER BY r.due_date ASC
            LIMIT ? OFFSET ?
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ps.setInt(2, offset);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                // Dynamically flag overdue items that haven't had their status updated in DB
                String status = rs.getString("status");
                String dueDateStr = rs.getString("due_date");
                if ("BORROWED".equals(status)) {
                    LocalDate dueDate = LocalDate.parse(dueDateStr);
                    if (dueDate.isBefore(LocalDate.now())) {
                        status = "OVERDUE";
                    }
                }
                
                list.add(new ActiveBorrowing(
                        rs.getString("full_name"),
                        rs.getString("title"),
                        rs.getString("borrowed_at").split(" ")[0], // just the date part
                        dueDateStr,
                        status
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}
