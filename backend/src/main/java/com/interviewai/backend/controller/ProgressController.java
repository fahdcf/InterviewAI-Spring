package com.interviewai.backend.controller;

import com.interviewai.backend.dto.request.GoalRequest;
import com.interviewai.backend.dto.response.GoalResponse;
import com.interviewai.backend.dto.response.LeaderboardEntry;
import com.interviewai.backend.dto.response.ProgressSummary;
import com.interviewai.backend.dto.response.QuestResponse;
import com.interviewai.backend.repository.QuestRepository.QuestClaim;
import com.interviewai.backend.security.AccountPrincipal;
import com.interviewai.backend.service.AccountService;
import com.interviewai.backend.service.ProgressService;
import com.interviewai.backend.service.QuestService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class ProgressController {

    private final AccountService accounts;
    private final ProgressService progress;
    private final QuestService quests;

    public ProgressController(AccountService accounts, ProgressService progress, QuestService quests) {
        this.accounts = accounts;
        this.progress = progress;
        this.quests = quests;
    }

    @GetMapping("/progress")
    public ProgressSummary progress(@AuthenticationPrincipal AccountPrincipal account) {
        return progress.summary(activeUserId(account));
    }

    @GetMapping("/leaderboard")
    public List<LeaderboardEntry> leaderboard(@AuthenticationPrincipal AccountPrincipal account) {
        activeUserId(account);
        return progress.leaderboard();
    }

    @GetMapping("/goals")
    public List<GoalResponse> goals(@AuthenticationPrincipal AccountPrincipal account) {
        return progress.goals(activeUserId(account));
    }

    @PostMapping("/goals")
    @ResponseStatus(HttpStatus.CREATED)
    public void addGoal(@AuthenticationPrincipal AccountPrincipal account, @Valid @RequestBody GoalRequest request) {
        progress.addGoal(activeUserId(account), request);
    }

    @PatchMapping("/goals/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateGoal(@AuthenticationPrincipal AccountPrincipal account, @PathVariable int id,
            @Valid @RequestBody GoalRequest request) {
        progress.updateGoal(activeUserId(account), id, request);
    }

    @DeleteMapping("/goals/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeGoal(@AuthenticationPrincipal AccountPrincipal account, @PathVariable int id) {
        progress.removeGoal(activeUserId(account), id);
    }

    @GetMapping("/quests")
    public List<QuestResponse> quests(@AuthenticationPrincipal AccountPrincipal account) {
        return quests.list(activeUserId(account));
    }

    @PostMapping("/quests/{type}/claims")
    public QuestClaim claim(@AuthenticationPrincipal AccountPrincipal account, @PathVariable String type,
            @Valid @RequestBody QuestClaimRequest request) {
        return quests.claim(activeUserId(account), type, request.requestId().toString());
    }

    private int activeUserId(AccountPrincipal account) {
        accounts.getActiveAccount(account.getUserId());
        return account.getUserId();
    }

    public record QuestClaimRequest(@NotNull UUID requestId) {
    }
}
