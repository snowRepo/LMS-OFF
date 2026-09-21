package com.lms.db;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Manages the local SQLite database connection.
 * Database file is stored in a platform-appropriate location:
 *  - macOS  : ~/Library/Application Support/LibraryMS/lms.db
 *  - Windows: %APPDATA%\LibraryMS\lms.db
 *  - Linux  : ~/.local/share/LibraryMS/lms.db
 */
public class DatabaseManager {

    private static DatabaseManager instance;
    private Connection connection;

    private DatabaseManager() throws SQLException {
        String dbPath = resolveDbPath();
        File dbFile = new File(dbPath);
        dbFile.getParentFile().mkdirs(); // ensure directories exist
        connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        connection.createStatement().execute("PRAGMA foreign_keys = ON");
        initSchema();
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            try {
                instance = new DatabaseManager();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to initialize database: " + e.getMessage(), e);
            }
        }
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }

    // -------------------------------------------------------------------------
    // Platform-aware DB path
    // -------------------------------------------------------------------------

    private String resolveDbPath() {
        String os = System.getProperty("os.name").toLowerCase();
        String home = System.getProperty("user.home");
        String separator = File.separator;

        if (os.contains("mac")) {
            return home + separator + "Library" + separator
                    + "Application Support" + separator + "LibraryMS" + separator + "lms.db";
        } else if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            if (appData == null) appData = home;
            return appData + separator + "LibraryMS" + separator + "lms.db";
        } else {
            // Linux / other Unix
            return home + separator + ".local" + separator
                    + "share" + separator + "LibraryMS" + separator + "lms.db";
        }
    }

    // -------------------------------------------------------------------------
    // Schema initialization (CREATE TABLE IF NOT EXISTS)
    // -------------------------------------------------------------------------

    private void initSchema() throws SQLException {
        try (Statement stmt = connection.createStatement()) {

            // System users (staff)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id            INTEGER PRIMARY KEY AUTOINCREMENT,
                    full_name     TEXT    NOT NULL,
                    username      TEXT    UNIQUE NOT NULL,
                    email         TEXT,
                    phone         TEXT,
                    dob           DATE,
                    password_hash TEXT    NOT NULL,
                    pin_hash      TEXT    NOT NULL,
                    role          TEXT    NOT NULL CHECK(role IN ('ADMIN','LIBRARIAN')),
                    is_active     INTEGER DEFAULT 1,
                    must_change_password INTEGER DEFAULT 0,
                    created_at    DATETIME DEFAULT CURRENT_TIMESTAMP
                )
            """);

            // Library members (patrons)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS members (
                    id          INTEGER PRIMARY KEY AUTOINCREMENT,
                    member_code TEXT    UNIQUE NOT NULL,
                    full_name   TEXT    NOT NULL,
                    email       TEXT,
                    phone       TEXT,
                    address     TEXT,
                    dob         DATE,
                    is_active   INTEGER DEFAULT 1,
                    joined_date DATE    DEFAULT (date('now')),
                    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP
                )
            """);

            // Book categories
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS categories (
                    id          INTEGER PRIMARY KEY AUTOINCREMENT,
                    name        TEXT UNIQUE NOT NULL,
                    description TEXT,
                    is_active   INTEGER DEFAULT 1,
                    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP
                )
            """);

            // Books
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS books (
                    id               INTEGER PRIMARY KEY AUTOINCREMENT,
                    title            TEXT NOT NULL,
                    author           TEXT NOT NULL,
                    isbn             TEXT UNIQUE,
                    category_id      INTEGER,
                    total_copies     INTEGER DEFAULT 1,
                    available_copies INTEGER DEFAULT 1,
                    published_year   INTEGER,
                    description      TEXT,
                    created_at       DATETIME DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (category_id) REFERENCES categories(id)
                )
            """);

            // Borrow / return records
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS borrow_records (
                    id          INTEGER PRIMARY KEY AUTOINCREMENT,
                    book_id     INTEGER NOT NULL,
                    member_id   INTEGER NOT NULL,
                    borrowed_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    due_date    DATE     NOT NULL,
                    returned_at DATETIME,
                    status      TEXT DEFAULT 'BORROWED'
                                CHECK(status IN ('BORROWED','RETURNED','OVERDUE')),
                    FOREIGN KEY (book_id)   REFERENCES books(id),
                    FOREIGN KEY (member_id) REFERENCES members(id)
                )
            """);

            // Daily attendance
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS attendance (
                    id         INTEGER PRIMARY KEY AUTOINCREMENT,
                    member_id  INTEGER NOT NULL,
                    date       DATE    DEFAULT (date('now')),
                    check_in   DATETIME DEFAULT CURRENT_TIMESTAMP,
                    check_out  DATETIME,
                    FOREIGN KEY (member_id) REFERENCES members(id)
                )
            """);

            // Application settings (sync configuration)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS sync_config (
                    id                  INTEGER PRIMARY KEY CHECK (id = 1), -- Single row
                    db_type             TEXT,
                    server_host         TEXT,
                    server_port         TEXT,
                    db_name             TEXT,
                    db_user             TEXT,
                    db_pass             TEXT,
                    last_sync_time      DATETIME,
                    sync_interval_mins  INTEGER DEFAULT 60,
                    is_enabled          INTEGER DEFAULT 0
                )
            """);

            // Activity Logs
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS activity_logs (
                    id          INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id     INTEGER,
                    username    TEXT,
                    action_type TEXT NOT NULL,
                    description TEXT NOT NULL,
                    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
                    updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (user_id) REFERENCES users(id)
                )
            """);

            // Sync Log for offline changes
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS sync_log (
                    id          INTEGER PRIMARY KEY AUTOINCREMENT,
                    table_name  TEXT NOT NULL,
                    record_id   TEXT NOT NULL,
                    operation   TEXT NOT NULL,
                    synced      INTEGER DEFAULT 0,
                    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP
                )
            """);

            // --- MIGRATIONS ---
            try { stmt.execute("ALTER TABLE categories ADD COLUMN is_active INTEGER DEFAULT 1"); } catch (SQLException ignore) {}
            try { stmt.execute("ALTER TABLE members ADD COLUMN dob DATE"); } catch (SQLException ignore) {}
            try { stmt.execute("ALTER TABLE users ADD COLUMN email TEXT"); } catch (SQLException ignore) {}
            try { stmt.execute("ALTER TABLE users ADD COLUMN phone TEXT"); } catch (SQLException ignore) {}
            try { stmt.execute("ALTER TABLE users ADD COLUMN dob DATE"); } catch (SQLException ignore) {}
            try { stmt.execute("ALTER TABLE users ADD COLUMN is_active INTEGER DEFAULT 1"); } catch (SQLException ignore) {}
            try { stmt.execute("ALTER TABLE users ADD COLUMN must_change_password INTEGER DEFAULT 0"); } catch (SQLException ignore) {}
            
            try { 
                stmt.execute("ALTER TABLE users ADD COLUMN last_password_change DATETIME"); 
                stmt.execute("UPDATE users SET last_password_change = CURRENT_TIMESTAMP WHERE last_password_change IS NULL");
            } catch (SQLException ignore) {}
            
            // Add updated_at columns
            String[] tables = {"users", "members", "categories", "books", "borrow_records", "attendance"};
            for (String table : tables) {
                try { 
                    stmt.execute("ALTER TABLE " + table + " ADD COLUMN updated_at DATETIME"); 
                    stmt.execute("UPDATE " + table + " SET updated_at = CURRENT_TIMESTAMP WHERE updated_at IS NULL");
                } catch (SQLException ignore) {}
            }

            // --- TRIGGERS FOR SYNC LOG & UPDATED_AT ---
            for (String table : tables) {
                // Update updated_at on UPDATE
                stmt.execute(String.format("""
                    CREATE TRIGGER IF NOT EXISTS trg_%s_updated_at 
                    AFTER UPDATE ON %s
                    BEGIN
                        UPDATE %s SET updated_at = CURRENT_TIMESTAMP WHERE id = NEW.id;
                    END;
                """, table, table, table));

                // Log INSERT to sync_log
                stmt.execute(String.format("""
                    CREATE TRIGGER IF NOT EXISTS trg_%s_sync_insert 
                    AFTER INSERT ON %s
                    BEGIN
                        INSERT INTO sync_log (table_name, record_id, operation) VALUES ('%s', NEW.id, 'INSERT');
                    END;
                """, table, table, table));

                // Log UPDATE to sync_log
                stmt.execute(String.format("""
                    CREATE TRIGGER IF NOT EXISTS trg_%s_sync_update 
                    AFTER UPDATE ON %s
                    BEGIN
                        INSERT INTO sync_log (table_name, record_id, operation) VALUES ('%s', NEW.id, 'UPDATE');
                    END;
                """, table, table, table));

                // Log DELETE to sync_log
                stmt.execute(String.format("""
                    CREATE TRIGGER IF NOT EXISTS trg_%s_sync_delete 
                    AFTER DELETE ON %s
                    BEGIN
                        INSERT INTO sync_log (table_name, record_id, operation) VALUES ('%s', OLD.id, 'DELETE');
                    END;
                """, table, table, table));
            }
        }
    }

    // -------------------------------------------------------------------------
    // Data Management
    // -------------------------------------------------------------------------

    public void wipeLocalKeepAdmin() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            connection.setAutoCommit(false);
            stmt.execute("DELETE FROM sync_log");
            stmt.execute("DELETE FROM activity_logs");
            stmt.execute("DELETE FROM attendance");
            stmt.execute("DELETE FROM borrow_records");
            stmt.execute("DELETE FROM books");
            stmt.execute("DELETE FROM categories");
            stmt.execute("DELETE FROM members");
            stmt.execute("DELETE FROM users WHERE role != 'ADMIN'");
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    public void wipeEverything() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            connection.setAutoCommit(false);
            stmt.execute("DELETE FROM sync_log");
            stmt.execute("DELETE FROM activity_logs");
            stmt.execute("DELETE FROM attendance");
            stmt.execute("DELETE FROM borrow_records");
            stmt.execute("DELETE FROM books");
            stmt.execute("DELETE FROM categories");
            stmt.execute("DELETE FROM members");
            stmt.execute("DELETE FROM users");
            stmt.execute("DELETE FROM sync_config");
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }
}
