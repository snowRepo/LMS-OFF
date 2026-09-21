package com.lms.util;

import com.lms.db.DatabaseManager;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

public class ReportExporter {

    private static String getBooksSql() {
        return "SELECT b.id as 'Book ID', b.title as 'Title', b.author as 'Author', b.isbn as 'ISBN', " +
               "c.name as 'Category', b.total_copies as 'Total Copies', b.available_copies as 'Available Copies', " +
               "b.published_year as 'Published Year', b.created_at as 'Date Added' " +
               "FROM books b LEFT JOIN categories c ON b.category_id = c.id ORDER BY b.title ASC";
    }

    private static String getMembersSql() {
        return "SELECT member_code as 'Member Code', full_name as 'Full Name', email as 'Email', " +
               "phone as 'Phone', address as 'Address', dob as 'Date of Birth', joined_date as 'Date Joined', " +
               "CASE WHEN is_active = 1 THEN 'Active' ELSE 'Inactive' END as 'Status' " +
               "FROM members ORDER BY full_name ASC";
    }

    private static String getCirculationSql(String startDate, String endDate) {
        StringBuilder sql = new StringBuilder(
            "SELECT c.id as 'Record ID', b.title as 'Book Title', m.full_name as 'Member Name', " +
            "c.borrowed_at as 'Date Borrowed', c.due_date as 'Due Date', c.returned_at as 'Date Returned', " +
            "c.status as 'Status' " +
            "FROM borrow_records c " +
            "JOIN books b ON c.book_id = b.id " +
            "JOIN members m ON c.member_id = m.id " +
            "WHERE 1=1 "
        );
        if (startDate != null && !startDate.isEmpty()) sql.append("AND date(c.borrowed_at) >= date(?) ");
        if (endDate != null && !endDate.isEmpty()) sql.append("AND date(c.borrowed_at) <= date(?) ");
        sql.append("ORDER BY c.borrowed_at DESC");
        return sql.toString();
    }

    private static String getAttendanceSql(String startDate, String endDate) {
        StringBuilder sql = new StringBuilder(
            "SELECT a.id as 'Record ID', m.member_code as 'Member Code', m.full_name as 'Member Name', " +
            "a.date as 'Date', a.check_in as 'Check In Time', a.check_out as 'Check Out Time' " +
            "FROM attendance a " +
            "JOIN members m ON a.member_id = m.id " +
            "WHERE 1=1 "
        );
        if (startDate != null && !startDate.isEmpty()) sql.append("AND date(a.date) >= date(?) ");
        if (endDate != null && !endDate.isEmpty()) sql.append("AND date(a.date) <= date(?) ");
        sql.append("ORDER BY a.check_in DESC");
        return sql.toString();
    }

    private static String getActivityLogsSql(String startDate, String endDate) {
        StringBuilder sql = new StringBuilder(
            "SELECT id as 'Log ID', username as 'User', action_type as 'Action Type', " +
            "description as 'Description', created_at as 'Timestamp' " +
            "FROM activity_logs " +
            "WHERE 1=1 "
        );
        if (startDate != null && !startDate.isEmpty()) sql.append("AND date(created_at) >= date(?) ");
        if (endDate != null && !endDate.isEmpty()) sql.append("AND date(created_at) <= date(?) ");
        sql.append("ORDER BY created_at DESC");
        return sql.toString();
    }

    public static boolean exportBooksReport(File file) {
        try (Workbook workbook = new XSSFWorkbook()) {
            addSheetToWorkbook(workbook, getBooksSql(), null, null, "Books Report");
            return saveWorkbook(workbook, file);
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    public static boolean exportMembersReport(File file) {
        try (Workbook workbook = new XSSFWorkbook()) {
            addSheetToWorkbook(workbook, getMembersSql(), null, null, "Members Report");
            return saveWorkbook(workbook, file);
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    public static boolean exportCirculationReport(File file, String startDate, String endDate) {
        try (Workbook workbook = new XSSFWorkbook()) {
            addSheetToWorkbook(workbook, getCirculationSql(startDate, endDate), startDate, endDate, "Circulation Report");
            return saveWorkbook(workbook, file);
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    public static boolean exportAttendanceReport(File file, String startDate, String endDate) {
        try (Workbook workbook = new XSSFWorkbook()) {
            addSheetToWorkbook(workbook, getAttendanceSql(startDate, endDate), startDate, endDate, "Attendance Report");
            return saveWorkbook(workbook, file);
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    public static boolean exportActivityLogsReport(File file, String startDate, String endDate) {
        try (Workbook workbook = new XSSFWorkbook()) {
            addSheetToWorkbook(workbook, getActivityLogsSql(startDate, endDate), startDate, endDate, "Activity Logs");
            return saveWorkbook(workbook, file);
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    public static boolean exportAllDataReport(File file, String startDate, String endDate) {
        try (Workbook workbook = new XSSFWorkbook()) {
            addSheetToWorkbook(workbook, getBooksSql(), null, null, "Books");
            addSheetToWorkbook(workbook, getMembersSql(), null, null, "Members");
            addSheetToWorkbook(workbook, getCirculationSql(startDate, endDate), startDate, endDate, "Circulation");
            addSheetToWorkbook(workbook, getAttendanceSql(startDate, endDate), startDate, endDate, "Attendance");
            addSheetToWorkbook(workbook, getActivityLogsSql(startDate, endDate), startDate, endDate, "Activity Logs");
            return saveWorkbook(workbook, file);
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    private static void addSheetToWorkbook(Workbook workbook, String sql, String startDate, String endDate, String sheetName) throws SQLException {
        Connection conn = DatabaseManager.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
             
            int paramIndex = 1;
            if (startDate != null && !startDate.isEmpty()) ps.setString(paramIndex++, startDate);
            if (endDate != null && !endDate.isEmpty()) ps.setString(paramIndex++, endDate);
            
            try (ResultSet rs = ps.executeQuery()) {
                Sheet sheet = workbook.createSheet(sheetName);
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();
                
                CellStyle headerStyle = workbook.createCellStyle();
                Font headerFont = workbook.createFont();
                headerFont.setBold(true);
                headerStyle.setFont(headerFont);
                headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
                headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                headerStyle.setBorderBottom(BorderStyle.THIN);
                headerStyle.setBorderTop(BorderStyle.THIN);
                headerStyle.setBorderLeft(BorderStyle.THIN);
                headerStyle.setBorderRight(BorderStyle.THIN);
                
                CellStyle dataStyle = workbook.createCellStyle();
                dataStyle.setBorderBottom(BorderStyle.THIN);
                dataStyle.setBorderTop(BorderStyle.THIN);
                dataStyle.setBorderLeft(BorderStyle.THIN);
                dataStyle.setBorderRight(BorderStyle.THIN);
                
                Row headerRow = sheet.createRow(0);
                for (int i = 1; i <= columnCount; i++) {
                    Cell cell = headerRow.createCell(i - 1);
                    cell.setCellValue(metaData.getColumnLabel(i));
                    cell.setCellStyle(headerStyle);
                }
                
                int rowIndex = 1;
                while (rs.next()) {
                    Row row = sheet.createRow(rowIndex++);
                    for (int i = 1; i <= columnCount; i++) {
                        Cell cell = row.createCell(i - 1);
                        Object value = rs.getObject(i);
                        if (value != null) {
                            if (value instanceof Number) {
                                cell.setCellValue(((Number) value).doubleValue());
                            } else {
                                cell.setCellValue(value.toString());
                            }
                        } else {
                            cell.setCellValue("");
                        }
                        cell.setCellStyle(dataStyle);
                    }
                }
                
                for (int i = 0; i < columnCount; i++) {
                    sheet.autoSizeColumn(i);
                }
            }
        }
    }

    private static boolean saveWorkbook(Workbook workbook, File file) {
        try (FileOutputStream fileOut = new FileOutputStream(file)) {
            workbook.write(fileOut);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
