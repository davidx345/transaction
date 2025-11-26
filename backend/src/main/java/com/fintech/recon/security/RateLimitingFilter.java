package com.fintech.recon.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting filter using Bucket4j token bucket algorithm
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {

    private final SecurityProperties securityProperties;
    
    // In-memory bucket cache (use Redis in production for distributed rate limiting)
    private final Map<String, Bucket> bucketCache = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        
        if (!securityProperties.getRateLimiting().isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientId = resolveClientId(request);
        String path = request.getServletPath();
        
        Bucket bucket = resolveBucket(clientId, path);
        
        if (bucket.tryConsume(1)) {
            // Add rate limit headers
            response.setHeader("X-Rate-Limit-Remaining", String.valueOf(bucket.getAvailableTokens()));
            filterChain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded for client: {} on path: {}", clientId, path);
            
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.setHeader("X-Rate-Limit-Remaining", "0");
            response.setHeader("Retry-After", "60");
            response.getWriter().write("{\"error\": \"Rate limit exceeded. Please try again later.\", \"retryAfter\": 60}");
        }
    }

    /**
     * Resolve client identifier for rate limiting
     * Priority: API Key > User ID > IP Address
     */
    private String resolveClientId(HttpServletRequest request) {
        // Check for API key
        String apiKey = request.getHeader("X-API-Key");
        if (apiKey != null && !apiKey.isEmpty()) {
            return "api:" + apiKey;
        }
        
        // Check for authenticated user
        if (request.getUserPrincipal() != null) {
            return "user:" + request.getUserPrincipal().getName();
        }
        
        // Fall back to IP address
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isEmpty()) {
            return "ip:" + forwardedFor.split(",")[0].trim();
        }
        
        return "ip:" + request.getRemoteAddr();
    }

    /**
     * Get or create rate limiting bucket for client
     */
    private Bucket resolveBucket(String clientId, String path) {
        String bucketKey = clientId + ":" + categorizePath(path);
        
        return bucketCache.computeIfAbsent(bucketKey, key -> createBucket(path));
    }

    /**
     * Create bucket based on endpoint category
     */
    private Bucket createBucket(String path) {
        int requestsPerMinute;
        
        if (path.startsWith("/api/auth")) {
            requestsPerMinute = securityProperties.getRateLimiting().getAuthRequestsPerMinute();
        } else if (path.startsWith("/api/ingest") || path.contains("/upload")) {
            requestsPerMinute = securityProperties.getRateLimiting().getUploadRequestsPerMinute();
        } else if (path.startsWith("/api/reports") || path.contains("/export")) {
            requestsPerMinute = securityProperties.getRateLimiting().getReportRequestsPerMinute();
        } else {
            requestsPerMinute = securityProperties.getRateLimiting().getRequestsPerMinute();
        }
        
        Bandwidth limit = Bandwidth.classic(requestsPerMinute, 
                Refill.greedy(requestsPerMinute, Duration.ofMinutes(1)));
        
        return Bucket.builder().addLimit(limit).build();
    }

    /**
     * Categorize path for bucket grouping
     */
    private String categorizePath(String path) {
        if (path.startsWith("/api/auth")) {
            return "auth";
        } else if (path.startsWith("/api/ingest") || path.contains("/upload")) {
            return "upload";
        } else if (path.startsWith("/api/reports") || path.contains("/export")) {
            return "reports";
        } else {
            return "general";
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        // Skip rate limiting for health checks and static resources
        return path.equals("/api/health") || 
               path.startsWith("/actuator/health") ||
               path.startsWith("/static/");
    }
}
