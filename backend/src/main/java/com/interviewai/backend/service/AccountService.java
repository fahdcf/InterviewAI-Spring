package com.interviewai.backend.service;

import com.interviewai.backend.dto.request.RegisterRequest;
import com.interviewai.backend.dto.response.AccountResponse;
import com.interviewai.backend.model.Account;
import com.interviewai.backend.repository.AccountRepository;
import com.interviewai.backend.security.AccountPasswordEncoder;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AccountService {

    private final AccountRepository accounts;
    private final AccountPasswordEncoder passwordEncoder;

    public AccountService(AccountRepository accounts, AccountPasswordEncoder passwordEncoder) {
        this.accounts = accounts;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AccountResponse register(RegisterRequest request) {
        String username = request.getUsername().strip();
        String email = request.getEmail();
        if (email != null && email.isBlank()) {
            email = null;
        }
        if (!passwordEncoder.fitsBcrypt(request.getPassword())) {
            throw new IllegalArgumentException("Use a shorter password.");
        }
        if (email != null && accounts.findByLogin(email).isPresent()) {
            throw new DuplicateKeyException("Email already in use.");
        }
        String hash = passwordEncoder.encode(request.getPassword());
        int id = accounts.insertCandidate(username, email, hash);
        return getActiveAccount(id);
    }

    public AccountResponse getActiveAccount(int userId) {
        Account account = findActiveAccount(userId);
        List<String> roles = accounts.findRoles(userId);
        boolean administrator = roles.contains("SUPER_ADMIN")
                || accounts.findPermissions(userId).contains("manage_users");
        return new AccountResponse(account.getId(), account.getUsername(), account.getEmail(), roles, administrator);
    }

    public void upgradeLegacyPassword(int userId, String password) {
        Account account = findActiveAccount(userId);
        String oldHash = account.getPasswordHash();
        if (passwordEncoder.isLegacyHash(oldHash) && passwordEncoder.fitsBcrypt(password)
                && passwordEncoder.matches(password, oldHash)) {
            accounts.upgradePasswordHash(userId, oldHash, passwordEncoder.encode(password));
        }
    }

    private Account findActiveAccount(int userId) {
        return accounts.findById(userId).filter(Account::isActive)
                .orElseThrow(() -> new BadCredentialsException("Sign in again."));
    }
}
