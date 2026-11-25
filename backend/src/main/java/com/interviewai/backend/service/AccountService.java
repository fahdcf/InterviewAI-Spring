package com.interviewai.backend.service;

import com.interviewai.backend.dto.request.RegisterRequest;
import com.interviewai.backend.dto.response.AccountResponse;
import com.interviewai.backend.model.Account;
import com.interviewai.backend.model.entity.Role;
import com.interviewai.backend.model.entity.UserAccount;
import com.interviewai.backend.repository.AccountRepository;
import com.interviewai.backend.repository.RoleRepository;
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
    private final RoleRepository roles;

    public AccountService(AccountRepository accounts, AccountPasswordEncoder passwordEncoder, RoleRepository roles) {
        this.accounts = accounts;
        this.passwordEncoder = passwordEncoder;
        this.roles = roles;
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
        Role role = roles.findByName("CANDIDATE")
                .orElseThrow(() -> new IllegalStateException("The candidate role is missing."));
        UserAccount account = new UserAccount(username, email, hash);
        account.addRole(role);
        return getActiveAccount(accounts.saveAndFlush(account).getId());
    }

    public AccountResponse getActiveAccount(int userId) {
        Account account = findActiveAccount(userId);
        List<String> roles = accounts.findRoles(userId);
        List<String> permissions = accounts.findPermissions(userId);
        boolean administrator = roles.contains("SUPER_ADMIN")
                || permissions.contains("manage_users") || permissions.contains("manage_courses")
                || permissions.contains("view_analytics");
        return new AccountResponse(account.getId(), account.getUsername(), account.getEmail(), roles, permissions,
                administrator);
    }

    public void lockActiveAccount(int userId) {
        if (accounts.lockById(userId).filter(UserAccount::isActive).isEmpty()) {
            throw new BadCredentialsException("Sign in again.");
        }
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
        return accounts.findAccount(userId).filter(Account::isActive)
                .orElseThrow(() -> new BadCredentialsException("Sign in again."));
    }
}
