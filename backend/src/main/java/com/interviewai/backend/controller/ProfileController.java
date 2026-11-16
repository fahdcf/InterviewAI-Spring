package com.interviewai.backend.controller;

import com.interviewai.backend.dto.response.AccountResponse;
import com.interviewai.backend.security.AccountPrincipal;
import com.interviewai.backend.service.ProfileService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
public class ProfileController {

    private final ProfileService profile;

    public ProfileController(ProfileService profile) {
        this.profile = profile;
    }

    @PatchMapping
    public AccountResponse rename(@AuthenticationPrincipal AccountPrincipal account,
            @Valid @RequestBody RenameRequest request) {
        return profile.rename(account.getUserId(), request.username());
    }

    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@AuthenticationPrincipal AccountPrincipal account,
            @Valid @RequestBody PasswordRequest request, HttpServletRequest http) {
        profile.changePassword(account.getUserId(), request.currentPassword(), request.newPassword());
        endSession(http);
    }

    @PostMapping("/account-deletion")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AccountPrincipal account,
            @Valid @RequestBody DeleteRequest request, HttpServletRequest http) {
        profile.deleteAccount(account.getUserId(), request.password());
        endSession(http);
    }

    private void endSession(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    public record RenameRequest(@NotBlank @Size(max = 100) @Pattern(regexp = "[^@]+") String username) {
    }

    public record PasswordRequest(@NotBlank String currentPassword,
                                  @NotBlank @Size(min = 6, max = 72) String newPassword) {
    }

    public record DeleteRequest(@NotBlank String password) {
    }
}
