package net.skyminigames.login.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * A registered account. {@link #name} is the lower-case key; {@link #realName} keeps the original capitalisation.
 */
@Getter
@Setter
@Builder
public final class Account {
    private final String name;
    private final String realName;
    private String passwordHash;
    private final String registrationIp;
    private String lastIp;
    private final long registrationDate;
    private long lastLogin;
    private long lastSeen;
}
