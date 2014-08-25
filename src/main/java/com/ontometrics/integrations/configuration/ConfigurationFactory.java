package com.ontometrics.integrations.configuration;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

/**
 * Factory for obtaining a global application config
 */
public class ConfigurationFactory {
    private static PropertiesConfiguration CONFIGURATION;

    /**
     * @return application configuration
     */
    public static Configuration get() {
        if (CONFIGURATION == null) {
            try {
                CONFIGURATION = new PropertiesConfiguration();
                CONFIGURATION.setListDelimiter(';');
                CONFIGURATION.load("application.properties");
            } catch (ConfigurationException e) {
                throw new RuntimeException("Failed to load configuration", e);
            }
        }
        return CONFIGURATION;
    }
}
