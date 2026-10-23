package net.skyminigames.login.storage;

import java.util.Locale;

public enum StorageType {
    SQLITE,
    MYSQL;

    public static StorageType parse(String value) {
        if (value == null) {
            return SQLITE;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return SQLITE;
        }
    }
}
