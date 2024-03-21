package com.linksfoundation.dq.core.processing.anonymization.standard.exceptions;

/**
 * This exception is thrown whenever the format of a rule read from the configuration file is not recognized.
*/
public class RuleBadFormatted extends RuntimeException {
    public RuleBadFormatted() { super(); }
    public RuleBadFormatted(String message) { super(message); }
}
