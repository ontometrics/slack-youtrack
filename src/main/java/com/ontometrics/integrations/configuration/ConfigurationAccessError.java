package com.ontometrics.integrations.configuration;

/**
 * ConfigurationAccessError.java
 */
public class ConfigurationAccessError extends RuntimeException{
    public ConfigurationAccessError(String message, Throwable cause) {
        super(message, cause);
    }
}
