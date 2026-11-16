package com.interviewai.desktop.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AccountSummary {

    private int id;
    private String username;
    private boolean administrator;
    private List<String> roles = List.of();
    private List<String> permissions = List.of();

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public boolean isAdministrator() { return administrator; }
    public void setAdministrator(boolean administrator) { this.administrator = administrator; }

    public void setRoles(List<String> roles) { this.roles = roles; }
    public void setPermissions(List<String> permissions) { this.permissions = permissions; }

    public boolean isSuperAdmin() { return roles.contains("SUPER_ADMIN"); }
    public boolean canManageUsers() { return isSuperAdmin() || permissions.contains("manage_users"); }
    public boolean canManageCourses() { return isSuperAdmin() || permissions.contains("manage_courses"); }
    public boolean canViewAnalytics() { return isSuperAdmin() || permissions.contains("view_analytics"); }
}
