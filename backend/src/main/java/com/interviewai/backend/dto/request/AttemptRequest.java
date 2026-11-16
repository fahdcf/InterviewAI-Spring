package com.interviewai.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public class AttemptRequest {

    @NotNull
    private UUID requestId;

    @NotNull
    @Pattern(regexp = "LESSON|REVIEW")
    private String mode;

    @Size(max = 4000)
    private String answer;

    private boolean skip;

    public UUID getRequestId() { return requestId; }
    public void setRequestId(UUID requestId) { this.requestId = requestId; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public boolean isSkip() { return skip; }
    public void setSkip(boolean skip) { this.skip = skip; }
}
