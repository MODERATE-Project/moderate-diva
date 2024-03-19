package com.linksfoundation.dq.core.processing.anonymization.standard.exceptions;

public class RuleNotRecognized extends RuntimeException {
    public RuleNotRecognized() { super(); }
    public RuleNotRecognized(String message) { super(message); }
}
