package com.interviewai.backend.service;

import com.interviewai.backend.dto.response.AdminCourse;
import com.interviewai.backend.dto.response.AdminOverview;
import com.interviewai.backend.dto.response.AdminPage;
import com.interviewai.backend.dto.response.AdminUser;
import com.interviewai.backend.model.entity.UserAccount;
import com.interviewai.backend.repository.AccountRepository;
import com.interviewai.backend.repository.ChapterRepository;
import com.interviewai.backend.repository.CourseRepository;
import com.interviewai.backend.repository.DailyCount;
import com.interviewai.backend.repository.FeedbackRepository;
import com.interviewai.backend.repository.GoalRepository;
import com.interviewai.backend.repository.OnboardingRepository;
import com.interviewai.backend.repository.PracticeRepository;
import com.interviewai.backend.repository.ProgressRepository;
import com.interviewai.backend.repository.QuestionRepository;
import com.interviewai.backend.repository.RoleRepository;
import com.interviewai.backend.security.AccountPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

@Service
public class AdminService {

    private static final int PAGE_SIZE = 20;
    private static final DateTimeFormatter DATE_TIME = new DateTimeFormatterBuilder()
            .appendPattern("uuuu-MM-dd HH:mm:ss")
            .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
            .toFormatter();

    private final AccountRepository accounts;
    private final RoleRepository roles;
    private final CourseRepository courses;
    private final ChapterRepository chapters;
    private final QuestionRepository questions;
    private final ProgressRepository progress;
    private final PracticeRepository attempts;
    private final GoalRepository goals;
    private final FeedbackRepository feedback;
    private final OnboardingRepository onboarding;
    private final Clock clock;

    public AdminService(AccountRepository accounts, RoleRepository roles, CourseRepository courses,
            ChapterRepository chapters, QuestionRepository questions, ProgressRepository progress,
            PracticeRepository attempts, GoalRepository goals, FeedbackRepository feedback,
            OnboardingRepository onboarding, Clock clock) {
        this.accounts = accounts;
        this.roles = roles;
        this.courses = courses;
        this.chapters = chapters;
        this.questions = questions;
        this.progress = progress;
        this.attempts = attempts;
        this.goals = goals;
        this.feedback = feedback;
        this.onboarding = onboarding;
        this.clock = clock;
    }

    public AdminPage<AdminUser> users(String search, String status, String role, int page) {
        validatePage(page);
        validateStatus(status, "ACTIVE", "SUSPENDED");
        Page<AccountRepository.UserSummary> result =
                accounts.findAdminUsers(searchPattern(search), status, role, PageRequest.of(page, PAGE_SIZE));
        if (result.isEmpty()) {
            return new AdminPage<>(List.of(), page, (int) result.getTotalElements());
        }
        List<Integer> ids = result.getContent().stream().map(AccountRepository.UserSummary::getId).toList();
        Map<Integer, List<String>> rolesByUser = new LinkedHashMap<>();
        for (AccountRepository.UserRole userRole : accounts.findRolesForUsers(ids)) {
            rolesByUser.computeIfAbsent(userRole.getUserId(), id -> new ArrayList<>()).add(userRole.getRole());
        }
        List<AdminUser> users = result.getContent().stream()
                .map(row -> toAdminUser(row, rolesByUser.getOrDefault(row.getId(), List.of())))
                .toList();
        return new AdminPage<>(users, page, (int) result.getTotalElements());
    }

    public AdminUser user(int id) {
        return accounts.findAdminUser(id)
                .map(summary -> toAdminUser(summary, accounts.findRoles(id)))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User unavailable."));
    }

    public List<String> roles() {
        return roles.findNames();
    }

    @Transactional
    public void editUser(AccountPrincipal actor, int id, String email, String role, Boolean active) {
        UserAccount current = lockManagedUser(actor, id);
        if (actor.getUserId() == id && (Boolean.FALSE.equals(active) || role != null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Change your own access from another administrator account.");
        }
        if (role != null && (!isSuperAdmin(actor) && !"CANDIDATE".equals(role) || !roles.findNames().contains(role))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot assign this role.");
        }
        if (email != null) {
            current.setEmail(email.isBlank() ? null : email);
        }
        if (role != null && !accounts.findRoles(id).equals(List.of(role))) {
            current.assignRole(roles.findByName(role).orElseThrow(() -> new IllegalArgumentException("Unknown role.")));
        }
        if (active != null && active != current.isActive()) {
            current.changeActive(active);
        }
    }

    @Transactional
    public void reset(AccountPrincipal actor, int id, boolean allProgress) {
        lockManagedUser(actor, id);
        progress.resetXp(id);
        if (allProgress) {
            chapters.resetProgress(id);
            questions.resetProgress(id);
            courses.reopenCompleted(id);
            attempts.deleteForUser(id);
            goals.resetProgress(id);
        }
    }

    public AdminPage<AdminCourse> courses(String search, String status, int page) {
        validatePage(page);
        validateStatus(status, "ACTIVE", "ARCHIVED", "COMPLETED");
        Page<CourseRepository.CourseDetails> result =
                courses.findAdminCourses(searchPattern(search), status, PageRequest.of(page, PAGE_SIZE));
        return new AdminPage<>(result.stream().map(AdminService::toAdminCourse).toList(),
                page, (int) result.getTotalElements());
    }

    public AdminCourse course(int id) {
        return courses.findAdminCourse(id).map(AdminService::toAdminCourse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course unavailable."));
    }

    @Transactional
    public void changeCourse(int id, String status) {
        if (status.isBlank()) {
            throw new IllegalArgumentException("A course status is required.");
        }
        validateStatus(status, "ACTIVE", "ARCHIVED", "COMPLETED");
        AdminCourse current = course(id);
        accounts.lockById(current.userId());
        courses.setCourseStatus(id, status);
    }

    @Transactional
    public void deleteCourse(int id) {
        AdminCourse current = course(id);
        accounts.lockById(current.userId());
        feedback.deleteForCourse(id);
        courses.deleteCourse(id);
    }

    @Transactional(readOnly = true)
    public AdminOverview overview() {
        LocalDate today = LocalDate.now(clock);
        LocalDateTime daily = utcStart(today);
        LocalDateTime weekly = utcStart(today.minusDays(6));
        LocalDateTime monthly = utcStart(today.withDayOfMonth(1));
        return new AdminOverview((int) accounts.count(), accounts.countByActiveTrue(),
                accounts.countByCreatedAtGreaterThanEqual(daily), accounts.countByCreatedAtGreaterThanEqual(weekly),
                accounts.countByCreatedAtGreaterThanEqual(monthly), (int) courses.count(),
                courses.countByStatus("COMPLETED"), chapters.countByStatus("COMPLETED"), (int) questions.count(),
                questions.countByStatusIn(List.of("COMPLETED", "INCORRECT")), progress.averageXp(),
                categories(onboarding.countInterviewTypes()), categories(onboarding.countLanguages()),
                dailyCounts(accounts.countNewAccounts(weekly)), dailyCounts(progress.countDailyUsage(weekly)));
    }

    private UserAccount lockManagedUser(AccountPrincipal actor, int id) {
        UserAccount target = accounts.lockById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User unavailable."));
        if (!isSuperAdmin(actor) && accounts.findRoles(id).stream().anyMatch(role -> !"CANDIDATE".equals(role))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot manage this staff account.");
        }
        return target;
    }

    private static AdminUser toAdminUser(AccountRepository.UserSummary row, List<String> roles) {
        return new AdminUser(row.getId(), row.getUsername(), row.getEmail(), Boolean.TRUE.equals(row.getActive()),
                row.getXp(), row.getCourseCount(), formatDateTime(row.getCreatedAt()), roles);
    }

    private static AdminCourse toAdminCourse(CourseRepository.CourseDetails row) {
        return new AdminCourse(row.getId(), row.getUserId(), row.getOwner(), row.getTitle(), row.getStatus(),
                row.getChapterCount(), row.getCompleted(), row.getQuestionCount(), formatDateTime(row.getCreatedAt()));
    }

    private static String formatDateTime(LocalDateTime value) {
        return value == null ? null : DATE_TIME.format(value);
    }

    private static Map<String, Long> categories(List<OnboardingRepository.CategoryCount> rows) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (OnboardingRepository.CategoryCount row : rows) {
            counts.put(row.getLabel(), row.getTotal());
        }
        return counts;
    }

    private static Map<String, Long> dailyCounts(List<DailyCount> rows) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (DailyCount row : rows) {
            counts.put(row.getDay().toString(), row.getTotal());
        }
        return counts;
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
