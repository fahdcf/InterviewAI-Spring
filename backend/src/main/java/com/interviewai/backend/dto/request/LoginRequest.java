package com.interviewai.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class LoginRequest {

    @NotBlank(message = "Enter your username or email.")
    @Size(max = 255, message = "Enter a valid username or email.")
    private String identifier;

    @NotBlank(message = "Enter your password.")
    private String password;

    public String getIdentifier() { return identifier; }
    public void setIdentifier(String identifier) { this.identifier = identifier; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
