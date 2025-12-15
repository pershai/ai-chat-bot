package com.example.aichatbot.enums;

public enum RagStateName {
    RETRIEVE("retrieve"),
    GRADE("grade"),
    GENERATE("generate"),
    CLARIFY("clarify");

    private final String value;

    RagStateName(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}