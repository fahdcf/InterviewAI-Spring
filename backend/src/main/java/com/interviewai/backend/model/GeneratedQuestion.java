package com.interviewai.backend.model;

import java.util.List;

public class GeneratedQuestion {

    private final String text;
    private final String type;
    private final String answer;
    private final String explanation;
    private final List<String> choices;

    public GeneratedQuestion(String text, String type, String answer, String explanation, List<String> choices) {
        this.text = text;
        this.type = type;
        this.answer = answer;
        this.explanation = explanation;
        this.choices = choices;
    }

    public String getText() { return text; }
    public String getType() { return type; }
    public String getAnswer() { return answer; }
    public String getExplanation() { return explanation; }
    public List<String> getChoices() { return choices; }
}
