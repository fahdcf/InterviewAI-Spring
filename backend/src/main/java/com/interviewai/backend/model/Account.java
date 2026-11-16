package com.interviewai.backend.model;

public class Account {

    private final int id;
    private final String username;
    private final String email;
    private final String passwordHash;
    private final boolean active;

    public Account(int id, String username, String email, String passwordHash, boolean active) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.active = active;
    }

    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public boolean isActive() { return active; }
}
