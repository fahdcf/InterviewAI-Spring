package com.interviewai.backend.security;

import com.interviewai.backend.model.Account;
import com.interviewai.backend.repository.AccountRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AccountDetailsService implements UserDetailsService {

    private final AccountRepository accounts;

    public AccountDetailsService(AccountRepository accounts) {
        this.accounts = accounts;
    }

    @Override
    public AccountPrincipal loadUserByUsername(String identifier) {
        Account account = accounts.findByLogin(identifier)
                .orElseThrow(() -> new UsernameNotFoundException("Account not found."));

        List<GrantedAuthority> authorities = new ArrayList<>();
        for (String role : accounts.findRoles(account.getId())) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        }
        for (String permission : accounts.findPermissions(account.getId())) {
            authorities.add(new SimpleGrantedAuthority(permission));
        }
        return new AccountPrincipal(account, authorities);
    }
}
