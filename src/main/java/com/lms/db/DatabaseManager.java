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
                    server_url          TEXT,
                    auth_token          TEXT,
                    last_sync_time      DATETIME,
                    sync_interval_mins  INTEGER DEFAULT 60,
                    is_enabled          INTEGER DEFAULT 0
                )
            """);

            // --- MIGRATIONS ---
            try {
                stmt.execute("ALTER TABLE categories ADD COLUMN is_active INTEGER DEFAULT 1");
            } catch (SQLException ignore) {
                // Column already exists
            }

            try {
                stmt.execute("ALTER TABLE members ADD COLUMN dob DATE");
            } catch (SQLException ignore) {}

            try { stmt.execute("ALTER TABLE users ADD COLUMN email TEXT"); } catch (SQLException ignore) {}
            try { stmt.execute("ALTER TABLE users ADD COLUMN phone TEXT"); } catch (SQLException ignore) {}
            try { stmt.execute("ALTER TABLE users ADD COLUMN dob DATE"); } catch (SQLException ignore) {}
            try { stmt.execute("ALTER TABLE users ADD COLUMN is_active INTEGER DEFAULT 1"); } catch (SQLException ignore) {}
            try { stmt.execute("ALTER TABLE users ADD COLUMN must_change_password INTEGER DEFAULT 0"); } catch (SQLException ignore) {}
        }
    }
}
