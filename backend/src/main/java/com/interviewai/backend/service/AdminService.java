package com.interviewai.backend.service;

import com.interviewai.backend.dto.response.AdminCourse;
import com.interviewai.backend.dto.response.AdminOverview;
import com.interviewai.backend.dto.response.AdminPage;
import com.interviewai.backend.dto.response.AdminUser;
import com.interviewai.backend.repository.AdminRepository;
import com.interviewai.backend.security.AccountPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;

@Service
public class AdminService {

    private final AdminRepository admin;
    private final Clock clock;

    public AdminService(AdminRepository admin, Clock clock) {
        this.admin = admin;
        this.clock = clock;
    }

    public AdminPage<AdminUser> users(String search, String status, String role, int page) {
        validatePage(page);
        validateStatus(status, "ACTIVE", "SUSPENDED");
        return admin.users(searchPattern(search), status, role, page);
    }

    public AdminUser user(int id) {
        return admin.user(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User unavailable."));
    }

    public List<String> roles() {
        return admin.roles();
    }

    @Transactional
    public void editUser(AccountPrincipal actor, int id, String email, String role, Boolean active) {
        AdminUser current = lockManagedUser(actor, id);
        if (actor.getUserId() == id && (Boolean.FALSE.equals(active) || role != null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Change your own access from another administrator account.");
        }
        if (role != null && (!isSuperAdmin(actor) && !"CANDIDATE".equals(role) || !admin.roles().contains(role))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot assign this role.");
        }
        if (email != null) {
            admin.setEmail(id, email.isBlank() ? null : email);
        }
        if (role != null && !current.roles().equals(List.of(role))) {
            admin.assignRole(id, role);
        }
        if (active != null && active != current.active()) {
            admin.setActive(id, active);
        }
    }

    @Transactional
    public void reset(AccountPrincipal actor, int id, boolean allProgress) {
        lockManagedUser(actor, id);
        if (allProgress) {
            admin.resetProgress(id);
        } else {
            admin.resetXp(id);
        }
    }

    public AdminPage<AdminCourse> courses(String search, String status, int page) {
        validatePage(page);
        validateStatus(status, "ACTIVE", "ARCHIVED", "COMPLETED");
        return admin.courses(searchPattern(search), status, page);
    }

    public AdminCourse course(int id) {
        return admin.course(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course unavailable."));
    }

    @Transactional
    public void changeCourse(int id, String status) {
        if (status.isBlank()) {
            throw new IllegalArgumentException("A course status is required.");
        }
        validateStatus(status, "ACTIVE", "ARCHIVED", "COMPLETED");
        AdminCourse current = course(id);
        admin.lockUser(current.userId());
        admin.setCourseStatus(id, status);
    }

    @Transactional
    public void deleteCourse(int id) {
        AdminCourse current = course(id);
        admin.lockUser(current.userId());
        admin.deleteCourse(id);
    }

    @Transactional(readOnly = true)
    public AdminOverview overview() {
        LocalDate today = LocalDate.now(clock);
        LocalDateTime daily = utcStart(today);
        LocalDateTime weekly = utcStart(today.minusDays(6));
        LocalDateTime monthly = utcStart(today.withDayOfMonth(1));
        return admin.overview(daily, weekly, monthly);
    }

    private AdminUser lockManagedUser(AccountPrincipal actor, int id) {
        if (!admin.lockUser(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User unavailable.");
        }
        AdminUser target = user(id);
        if (!isSuperAdmin(actor) && target.roles().stream().anyMatch(role -> !"CANDIDATE".equals(role))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot manage this staff account.");
        }
        return target;
    }

    private boolean isSuperAdmin(AccountPrincipal actor) {
        return actor.getAuthorities().stream().anyMatch(authority -> "ROLE_SUPER_ADMIN".equals(authority.getAuthority()));
    }

    private void validateStatus(String status, String... allowed) {
        if (!status.isBlank() && !List.of(allowed).contains(status)) {
            throw new IllegalArgumentException("Unknown status filter.");
        }
    }

    private void validatePage(int page) {
        if (page < 0 || page > 100_000) {
            throw new IllegalArgumentException("Invalid page.");
        }
    }

    private String searchPattern(String search) {
        return "%" + search.strip().toLowerCase(Locale.ROOT) + "%";
    }

    private LocalDateTime utcStart(LocalDate date) {
        return LocalDateTime.ofInstant(date.atStartOfDay(clock.getZone()).toInstant(), ZoneOffset.UTC);
    }
}
