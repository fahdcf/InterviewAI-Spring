package com.interviewai.backend.service;

import com.interviewai.backend.dto.response.AccountResponse;
import com.interviewai.backend.model.Account;
import com.interviewai.backend.repository.AccountRepository;
import com.interviewai.backend.repository.CourseRepository;
import com.interviewai.backend.repository.FeedbackRepository;
import com.interviewai.backend.repository.ProgressRepository;
import com.interviewai.backend.security.AccountPasswordEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProfileService {

    private final AccountRepository repository;
    private final AccountService accounts;
    private final AccountPasswordEncoder passwords;
    private final ProgressRepository progress;
    private final FeedbackRepository feedback;
    private final CourseRepository courses;

    public ProfileService(AccountRepository repository, AccountService accounts, AccountPasswordEncoder passwords,
            ProgressRepository progress, FeedbackRepository feedback, CourseRepository courses) {
        this.repository = repository;
        this.accounts = accounts;
        this.passwords = passwords;
        this.progress = progress;
        this.feedback = feedback;
        this.courses = courses;
    }

    @Transactional
    public AccountResponse rename(int userId, String username) {
        accounts.lockActiveAccount(userId);
        repository.updateUsername(userId, username.strip());
        return accounts.getActiveAccount(userId);
    }

    @Transactional
    public void changePassword(int userId, String oldPassword, String newPassword) {
        verifyPassword(userId, oldPassword);
        if (!passwords.fitsBcrypt(newPassword)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use a shorter password.");
        }
        repository.changePassword(userId, passwords.encode(newPassword));
    }

    @Transactional
    public void deleteAccount(int userId, String password) {
        verifyPassword(userId, password);
        if (accounts.getActiveAccount(userId).isAdministrator()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Administrator account deletion is unavailable.");
        }
        progress.deleteForUser(userId);
        feedback.deleteByUserOrOwnedCourse(userId);
        courses.deleteByOwner(userId);
        repository.deleteById(userId);
    }

    private void verifyPassword(int userId, String password) {
        accounts.lockActiveAccount(userId);
        Account account = repository.findAccount(userId).orElseThrow();
        if (!passwords.matches(password, account.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The current password is incorrect.");
        }
    }
}
