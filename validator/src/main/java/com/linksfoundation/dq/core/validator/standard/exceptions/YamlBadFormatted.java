package com.linksfoundation.dq.core.validator.standard.exceptions;

/**
 * This exception is thrown whenever the format of the configuration file is not recognized.
*/
public class YamlBadFormatted extends RuntimeException {
    public YamlBadFormatted() { super(); }
    public YamlBadFormatted(String message) { super(message); }
}
