package com.lms.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BookDAO {

    private final Connection conn;

    public BookDAO() {
        this.conn = DatabaseManager.getInstance().getConnection();
    }

    public record Category(int id, String name) {
        @Override
        public String toString() {
            return name; // Useful for ComboBox display
        }
    }

    public record Book(int id, String title, String author, String isbn, String categoryName, int totalCopies, int availableCopies, Integer publishedYear, String description) {}

    public List<Category> getCategories() {
        List<Category> list = new ArrayList<>();
        try {
            ResultSet rs = conn.createStatement().executeQuery("SELECT id, name FROM categories ORDER BY name ASC");
            while (rs.next()) {
                list.add(new Category(rs.getInt("id"), rs.getString("name")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public int getBooksCount(String search, Integer categoryId) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM books b ");
        if (categoryId != null) {
            sql.append("WHERE b.category_id = ? ");
        }
        if (search != null && !search.trim().isEmpty()) {
            sql.append(categoryId != null ? "AND " : "WHERE ");
            sql.append("(b.title LIKE ? OR b.author LIKE ? OR b.isbn LIKE ?) ");
        }

        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int paramIndex = 1;
            if (categoryId != null) {
                ps.setInt(paramIndex++, categoryId);
            }
            if (search != null && !search.trim().isEmpty()) {
                String like = "%" + search.trim() + "%";
                ps.setString(paramIndex++, like);
                ps.setString(paramIndex++, like);
                ps.setString(paramIndex++, like);
            }
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public List<Book> getBooks(String search, Integer categoryId, int offset, int limit) {
        List<Book> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
            SELECT b.id, b.title, b.author, b.isbn, c.name as category_name, b.total_copies, b.available_copies, b.published_year, b.description
            FROM books b
            LEFT JOIN categories c ON b.category_id = c.id
        """);

        if (categoryId != null) {
            sql.append(" WHERE b.category_id = ? ");
        }
        if (search != null && !search.trim().isEmpty()) {
            sql.append(categoryId != null ? " AND " : " WHERE ");
            sql.append(" (b.title LIKE ? OR b.author LIKE ? OR b.isbn LIKE ?) ");
        }
        
        sql.append(" ORDER BY b.title ASC LIMIT ? OFFSET ?");

        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int paramIndex = 1;
            if (categoryId != null) {
                ps.setInt(paramIndex++, categoryId);
            }
            if (search != null && !search.trim().isEmpty()) {
                String like = "%" + search.trim() + "%";
                ps.setString(paramIndex++, like);
                ps.setString(paramIndex++, like);
                ps.setString(paramIndex++, like);
            }
            ps.setInt(paramIndex++, limit);
            ps.setInt(paramIndex, offset);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Book(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getString("isbn"),
                        rs.getString("category_name") != null ? rs.getString("category_name") : "Uncategorized",
                        rs.getInt("total_copies"),
                        rs.getInt("available_copies"),
                        (Integer) rs.getObject("published_year"),
                        rs.getString("description")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean insertBook(String title, String author, String isbn, Integer categoryId, int copies, Integer publishedYear, String description) {
        String sql = "INSERT INTO books (title, author, isbn, category_id, total_copies, available_copies, published_year, description) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, title.trim());
            ps.setString(2, author.trim());
            ps.setString(3, isbn != null && !isbn.isBlank() ? isbn.trim() : null);
            if (categoryId != null) ps.setInt(4, categoryId); else ps.setNull(4, java.sql.Types.INTEGER);
            ps.setInt(5, copies);
            ps.setInt(6, copies); // initial available == total
            if (publishedYear != null) ps.setInt(7, publishedYear); else ps.setNull(7, java.sql.Types.INTEGER);
            ps.setString(8, description != null && !description.isBlank() ? description.trim() : null);
            if (ps.executeUpdate() > 0) {
                ActivityLogDAO.log("ADD_BOOK", "Added book: " + title.trim());
                return true;
            }
            return false;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateBook(int id, String title, String author, String isbn, Integer categoryId, int copies, Integer publishedYear, String description) {
        // Adjust available_copies based on the change in total_copies to preserve borrowed count,
        // and use MIN() to automatically repair any corrupted records where available > total.
        String sql = "UPDATE books SET available_copies = MIN(available_copies + (? - total_copies), ?), title=?, author=?, isbn=?, category_id=?, total_copies=?, published_year=?, description=? WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, copies);
            ps.setInt(2, copies);
            ps.setString(3, title.trim());
            ps.setString(4, author.trim());
            ps.setString(5, isbn != null && !isbn.isBlank() ? isbn.trim() : null);
            if (categoryId != null) ps.setInt(6, categoryId); else ps.setNull(6, java.sql.Types.INTEGER);
            ps.setInt(7, copies);
            if (publishedYear != null) ps.setInt(8, publishedYear); else ps.setNull(8, java.sql.Types.INTEGER);
            ps.setString(9, description != null && !description.isBlank() ? description.trim() : null);
            ps.setInt(10, id);
            if (ps.executeUpdate() > 0) {
                ActivityLogDAO.log("UPDATE_BOOK", "Updated book: " + title.trim());
                return true;
            }
            return false;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteBook(int id) {
        // Warning: This deletes the book unconditionally. 
        // In a real system, you might want to prevent deletion if there are active borrow_records.
        String sql = "DELETE FROM books WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            if (ps.executeUpdate() > 0) {
                ActivityLogDAO.log("DELETE_BOOK", "Deleted book ID: " + id);
                return true;
            }
            return false;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
