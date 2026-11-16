package com.interviewai.backend.dto.response;

import java.util.List;

public class AccountResponse {

    private final int id;
    private final String username;
    private final String email;
    private final List<String> roles;
    private final List<String> permissions;
    private final boolean administrator;

    public AccountResponse(int id, String username, String email, List<String> roles, List<String> permissions,
            boolean administrator) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.roles = roles;
        this.permissions = permissions;
        this.administrator = administrator;
    }

    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public List<String> getRoles() { return roles; }
    public List<String> getPermissions() { return permissions; }
    public boolean isAdministrator() { return administrator; }
}
