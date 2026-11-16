package com.interviewai.util;

import java.io.InputStream;
import java.util.Locale;
import java.util.Properties;

public class ConfigLoader {
    private static Properties props = new Properties();
    
    static {
        try (InputStream in = ConfigLoader.class.getResourceAsStream("/config/config.properties")) {
            if (in != null) props.load(in);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /** Environment variables override the non-secret classpath defaults. */
    public static String get(String key) {
        String environmentName = key.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "_");
        String environmentValue = System.getenv(environmentName);
        return environmentValue != null ? environmentValue : props.getProperty(key);
    }
}
