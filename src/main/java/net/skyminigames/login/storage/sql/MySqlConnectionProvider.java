package net.skyminigames.login.storage.sql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.skyminigames.login.config.PluginConfig;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * MySQL / MariaDB through a HikariCP pool and the (shaded) MariaDB driver.
 */
public final class MySqlConnectionProvider implements ConnectionProvider {

    private final PluginConfig config;
    private HikariDataSource dataSource;

    public MySqlConnectionProvider(PluginConfig config) {
        this.config = config;
    }

    @Override
    public void init() throws SQLException {
        HikariConfig hikari = new HikariConfig();
        hikari.setPoolName("SkyLogin");
        hikari.setDriverClassName(org.mariadb.jdbc.Driver.class.getName());
        hikari.setJdbcUrl("jdbc:mariadb://" + config.getMysqlHost() + ":" + config.getMysqlPort() + "/" + config.getMysqlDatabase());
        hikari.setUsername(config.getMysqlUsername());
        hikari.setPassword(config.getMysqlPassword());
        hikari.setMaximumPoolSize(config.getMysqlPoolSize());
        hikari.setMinimumIdle(Math.min(2, config.getMysqlPoolSize()));
        hikari.setConnectionTimeout(5000);
        hikari.setMaxLifetime(1_800_000);
        try {
            dataSource = new HikariDataSource(hikari);
        } catch (RuntimeException e) {
            throw new SQLException("Could not connect to MySQL at " + config.getMysqlHost() + ":" + config.getMysqlPort(), e);
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public String autoIncrementPrimaryKey() {
        return "INT NOT NULL AUTO_INCREMENT PRIMARY KEY";
    }

    @Override
    public void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }
}
