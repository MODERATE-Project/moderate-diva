package com.linksfoundation.dq.core.processing.anonymization.standard.exceptions;

/**
 * This exception is thrown whenever a rule read from the configuration file  is not recognized.
*/
public class RuleNotRecognized extends RuntimeException {
    public RuleNotRecognized() { super(); }
    public RuleNotRecognized(String message) { super(message); }
}
