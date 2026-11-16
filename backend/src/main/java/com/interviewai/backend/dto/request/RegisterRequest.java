package com.interviewai.backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

    @NotBlank(message = "Enter a username.")
    @Size(max = 100, message = "Use at most 100 characters for the username.")
    @Pattern(regexp = "[^@]+", message = "Use a username without an @ sign.")
    private String username;

    @Email(message = "Enter a valid email address.")
    @Size(max = 255, message = "Use at most 255 characters for the email.")
    private String email;

    @NotBlank(message = "Enter a password.")
    @Size(max = 72, message = "Use a shorter password.")
    private String password;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
