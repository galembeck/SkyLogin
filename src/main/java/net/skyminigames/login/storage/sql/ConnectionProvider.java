package net.skyminigames.login.storage.sql;

import java.sql.Connection;
import java.sql.SQLException;

public interface ConnectionProvider {

    void init() throws SQLException;

    /**
     * A connection the caller must close when done.
     */
    Connection getConnection() throws SQLException;

    /**
     * Column definition for an auto-increment primary key in this database's dialect.
     */
    String autoIncrementPrimaryKey();

    void close();
}
