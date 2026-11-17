package com.lcarvalho.brokerage.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lcarvalho.brokerage.common.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Every endpoint except the health check needs the X-API-Key header. */
@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-API-Key";

    private final byte[] expectedKey;
    private final ObjectMapper objectMapper;

    public ApiKeyFilter(@Value("${brokerage.api-key}") String apiKey, ObjectMapper objectMapper) {
        this.expectedKey = apiKey.getBytes(StandardCharsets.UTF_8);
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/actuator/health");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String providedKey = request.getHeader(HEADER);
        // Constant-time comparison: the response time does not reveal how much of the key matched
        if (providedKey != null
                && MessageDigest.isEqual(expectedKey, providedKey.getBytes(StandardCharsets.UTF_8))) {
            chain.doFilter(request, response);
            return;
        }
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(),
                ApiError.of("UNAUTHORIZED", "Missing or invalid API key"));
    }
}
