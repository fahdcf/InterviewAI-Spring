package com.interviewai.backend.controller;

import com.interviewai.backend.dto.request.NavigationRequest;
import com.interviewai.backend.dto.response.ChapterSummary;
import com.interviewai.backend.dto.response.CourseSummary;
import com.interviewai.backend.dto.response.DashboardResponse;
import com.interviewai.backend.security.AccountPrincipal;
import com.interviewai.backend.service.AccountService;
import com.interviewai.backend.service.DashboardService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class DashboardController {

    private final DashboardService dashboard;
    private final AccountService accounts;

    public DashboardController(DashboardService dashboard, AccountService accounts) {
        this.dashboard = dashboard;
        this.accounts = accounts;
    }

    @GetMapping("/dashboard")
    public DashboardResponse dashboard(@AuthenticationPrincipal AccountPrincipal account) {
        return dashboard.load(activeUserId(account));
    }

    @GetMapping("/courses")
    public List<CourseSummary> courses(@AuthenticationPrincipal AccountPrincipal account) {
        return dashboard.listCourses(activeUserId(account));
    }

    @GetMapping("/courses/{id}")
    public CourseSummary course(@AuthenticationPrincipal AccountPrincipal account, @PathVariable int id) {
        return dashboard.getCourse(activeUserId(account), id);
    }

    @GetMapping("/courses/{id}/chapters")
    public List<ChapterSummary> chapters(@AuthenticationPrincipal AccountPrincipal account, @PathVariable int id) {
        return dashboard.listChapters(activeUserId(account), id);
    }

    @PutMapping("/me/navigation")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void saveNavigation(@AuthenticationPrincipal AccountPrincipal account,
            @Valid @RequestBody NavigationRequest request) {
        dashboard.saveLocation(activeUserId(account), request);
    }

    private int activeUserId(AccountPrincipal account) {
        accounts.getActiveAccount(account.getUserId());
        return account.getUserId();
    }
}
