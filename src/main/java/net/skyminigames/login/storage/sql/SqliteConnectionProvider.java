package net.skyminigames.login.storage.sql;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * SQLite through the driver bundled with Spigot 1.8.8. Opening a connection is cheap (it's a local file),
 * and all access goes through a single storage thread, so there are no write conflicts.
 */
public final class SqliteConnectionProvider implements ConnectionProvider {

    private final File file;
    private final String url;

    public SqliteConnectionProvider(File file) {
        this.file = file;
        this.url = "jdbc:sqlite:" + file.getAbsolutePath();
    }

    @Override
    public void init() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new SQLException("The SQLite driver is not available on this server", e);
        }
        File folder = file.getParentFile();
        if (folder != null && !folder.exists() && !folder.mkdirs()) {
            throw new SQLException("Could not create folder " + folder);
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url);
    }

    @Override
    public String autoIncrementPrimaryKey() {
        return "INTEGER PRIMARY KEY AUTOINCREMENT";
    }

    @Override
    public void close() {
    }
}
