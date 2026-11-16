package com.interviewai.backend.security;

import com.interviewai.backend.model.Account;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.List;

public class AccountPrincipal extends User {

    private final int userId;
    private final int sessionVersion;

    public AccountPrincipal(Account account, List<GrantedAuthority> authorities) {
        super(account.getUsername(), account.getPasswordHash() == null ? "" : account.getPasswordHash(),
                account.isActive(), true, true, true, authorities);
        this.userId = account.getId();
        this.sessionVersion = account.getSessionVersion();
    }

    public int getUserId() {
        return userId;
    }

    public int getSessionVersion() {
        return sessionVersion;
    }
}
