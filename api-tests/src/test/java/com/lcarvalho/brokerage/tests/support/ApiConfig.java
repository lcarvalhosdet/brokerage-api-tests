package com.lcarvalho.brokerage.tests.support;

/**
 * Where the API under test is and which key to use. Each value comes from a system
 * property (-DbaseUrl=...), then an environment variable (BASE_URL), then a local default.
 */
public final class ApiConfig {

    private ApiConfig() {
    }

    public static String baseUrl() {
        return setting("baseUrl", "BASE_URL", "http://localhost:8080");
    }

    public static String apiKey() {
        return setting("apiKey", "API_KEY", "local-dev-key");
    }

    private static String setting(String property, String environmentVariable, String fallback) {
        String value = System.getProperty(property);
        if (value == null || value.isBlank()) {
            value = System.getenv(environmentVariable);
        }
        return (value == null || value.isBlank()) ? fallback : value;
    }
}
