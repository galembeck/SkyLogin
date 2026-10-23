package net.skyminigames.login.storage;

import lombok.Getter;
import net.skyminigames.login.config.PluginConfig;
import net.skyminigames.login.model.Account;
import net.skyminigames.login.storage.sql.ConnectionProvider;
import net.skyminigames.login.storage.sql.MySqlConnectionProvider;
import net.skyminigames.login.storage.sql.SqlAccountRepository;
import net.skyminigames.login.storage.sql.SqliteConnectionProvider;
import net.skyminigames.login.util.NamedThreadFactory;

import java.io.File;
import java.sql.SQLException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Asynchronous access to accounts. Every query runs on dedicated storage threads, never on the main thread.
 */
public final class AccountStorage {

    @Getter
    private final StorageType type;
    private final ConnectionProvider provider;
    private final SqlAccountRepository repository;
    private final ExecutorService executor;
    @Getter
    private volatile boolean ready;

    public AccountStorage(File dataFolder, PluginConfig config) {
        this.type = config.getStorageType();
        if (type == StorageType.MYSQL) {
            this.provider = new MySqlConnectionProvider(config);
            this.executor = Executors.newFixedThreadPool(4, new NamedThreadFactory("SkyLogin-Storage"));
        } else {
            this.provider = new SqliteConnectionProvider(new File(dataFolder, "accounts.db"));
            // SQLite allows one writer at a time: a single thread avoids "database is locked" errors
            this.executor = Executors.newSingleThreadExecutor(new NamedThreadFactory("SkyLogin-Storage"));
        }
        this.repository = new SqlAccountRepository(provider, config.getTable());
    }

    public void init() throws SQLException {
        provider.init();
        repository.createTable();
        ready = true;
    }

    public CompletableFuture<Optional<Account>> find(String name) {
        return supply(() -> repository.find(name));
    }

    public CompletableFuture<Void> insert(Account account) {
        return supply(() -> {
            repository.insert(account);
            return null;
        });
    }

    public CompletableFuture<Void> updateLogin(String name, String ip, long time) {
        return supply(() -> {
            repository.updateLogin(name, ip, time);
            return null;
        });
    }

    public CompletableFuture<Void> updateLastSeen(String name, long time) {
        return supply(() -> {
            repository.updateLastSeen(name, time);
            return null;
        });
    }

    public CompletableFuture<Integer> countByRegistrationIp(String ip) {
        return supply(() -> repository.countByRegistrationIp(ip));
    }

    /**
     * Waits for pending queries (up to 5 seconds), then closes the connections.
     */
    public void close() {
        ready = false;
        executor.shutdown();
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        provider.close();
    }

    private <T> CompletableFuture<T> supply(SqlSupplier<T> supplier) {
        CompletableFuture<T> future = new CompletableFuture<>();
        try {
            executor.execute(() -> {
                try {
                    future.complete(supplier.get());
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
        } catch (RejectedExecutionException e) {
            future.completeExceptionally(new SQLException("Storage is closed", e));
        }
        return future;
    }

    @FunctionalInterface
    private interface SqlSupplier<T> {
        T get() throws SQLException;
    }
}
