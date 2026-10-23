package net.skyminigames.login.storage.sql;

import lombok.RequiredArgsConstructor;
import net.skyminigames.login.model.Account;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

/**
 * Blocking SQL access to the accounts table. Only called from the storage thread.
 */
@RequiredArgsConstructor
public final class SqlAccountRepository {

    private final ConnectionProvider provider;
    private final String table;

    public void createTable() throws SQLException {
        try (Connection connection = provider.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + table + " ("
                    + "id " + provider.autoIncrementPrimaryKey() + ", "
                    + "name VARCHAR(16) NOT NULL UNIQUE, "
                    + "realname VARCHAR(16) NOT NULL, "
                    + "password VARCHAR(255) NOT NULL, "
                    + "reg_ip VARCHAR(45), "
                    + "last_ip VARCHAR(45), "
                    + "reg_date BIGINT NOT NULL, "
                    + "last_login BIGINT NOT NULL DEFAULT 0, "
                    + "last_seen BIGINT NOT NULL DEFAULT 0)");
        }
    }

    public Optional<Account> find(String name) throws SQLException {
        String sql = "SELECT name, realname, password, reg_ip, last_ip, reg_date, last_login, last_seen FROM " + table + " WHERE name = ?";
        try (Connection connection = provider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return Optional.empty();
                }
                return Optional.of(Account.builder()
                        .name(result.getString("name"))
                        .realName(result.getString("realname"))
                        .passwordHash(result.getString("password"))
                        .registrationIp(result.getString("reg_ip"))
                        .lastIp(result.getString("last_ip"))
                        .registrationDate(result.getLong("reg_date"))
                        .lastLogin(result.getLong("last_login"))
                        .lastSeen(result.getLong("last_seen"))
                        .build());
            }
        }
    }

    public void insert(Account account) throws SQLException {
        String sql = "INSERT INTO " + table + " (name, realname, password, reg_ip, last_ip, reg_date, last_login, last_seen) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = provider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, account.getName());
            statement.setString(2, account.getRealName());
            statement.setString(3, account.getPasswordHash());
            statement.setString(4, account.getRegistrationIp());
            statement.setString(5, account.getLastIp());
            statement.setLong(6, account.getRegistrationDate());
            statement.setLong(7, account.getLastLogin());
            statement.setLong(8, account.getLastSeen());
            statement.executeUpdate();
        }
    }

    public void updateLogin(String name, String ip, long time) throws SQLException {
        String sql = "UPDATE " + table + " SET last_ip = ?, last_login = ? WHERE name = ?";
        try (Connection connection = provider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, ip);
            statement.setLong(2, time);
            statement.setString(3, name);
            statement.executeUpdate();
        }
    }

    public void updateLastSeen(String name, long time) throws SQLException {
        String sql = "UPDATE " + table + " SET last_seen = ? WHERE name = ?";
        try (Connection connection = provider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, time);
            statement.setString(2, name);
            statement.executeUpdate();
        }
    }

    public int countByRegistrationIp(String ip) throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + table + " WHERE reg_ip = ?";
        try (Connection connection = provider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, ip);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getInt(1) : 0;
            }
        }
    }
}
