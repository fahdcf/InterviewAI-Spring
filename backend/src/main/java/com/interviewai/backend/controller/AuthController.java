package com.interviewai.backend.controller;

import com.interviewai.backend.dto.request.LoginRequest;
import com.interviewai.backend.dto.request.RegisterRequest;
import com.interviewai.backend.dto.response.AccountResponse;
import com.interviewai.backend.security.AccountPrincipal;
import com.interviewai.backend.service.AccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class AuthController {

    private final AccountService accounts;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository sessions;
    private final CsrfTokenRepository csrfTokens;

    public AuthController(AccountService accounts, AuthenticationManager authenticationManager,
            SecurityContextRepository sessions, CsrfTokenRepository csrfTokens) {
        this.accounts = accounts;
        this.authenticationManager = authenticationManager;
        this.sessions = sessions;
        this.csrfTokens = csrfTokens;
    }

    @GetMapping("/auth/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("headerName", token.getHeaderName(), "token", token.getToken());
    }

    @PostMapping("/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse register(@Valid @RequestBody RegisterRequest request) {
        return accounts.register(request);
    }

    @PostMapping("/auth/login")
    public AccountResponse login(@Valid @RequestBody LoginRequest login,
            HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                        login.getIdentifier().strip(), login.getPassword()));
        AccountPrincipal principal = (AccountPrincipal) authentication.getPrincipal();
        int userId = principal.getUserId();
        accounts.upgradeLegacyPassword(userId, login.getPassword());
        AccountResponse account = accounts.getActiveAccount(userId);

        // Rotate the anonymous session ID and use a fresh CSRF token after login.
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }
        csrfTokens.saveToken(null, request, response);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        sessions.saveContext(context, request, response);
        return account;
    }

    @GetMapping("/me")
    public AccountResponse currentAccount(@AuthenticationPrincipal AccountPrincipal principal) {
        return accounts.getActiveAccount(principal.getUserId());
    }
}
