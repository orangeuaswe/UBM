package com.UBM.BMW.domain;

import java.util.Arrays;
public enum QuestionState
{
    NOT_ASKED("not_asked"),
    ASKED("asked"),
    CONFIRMED_WITH_EVIDENCE("confirmed_with_evidence"),
    NOT_DONE("not_done"),
    UNKNOWN("unknown");
    private final String value;
    QuestionState(String value)
    {
        this.value = value;
    }
    public String value()
    {
        return value;
    }
    public static QuestionState fromValue(String value)
    {
        return Arrays.stream(values())
                .filter(s -> s.value.equalsIgnoreCase(value)||s.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown question state: "+value));
    }
}
