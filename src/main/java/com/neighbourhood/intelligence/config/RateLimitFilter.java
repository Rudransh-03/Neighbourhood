package com.neighbourhood.intelligence.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.function.Supplier;

@Component
@Slf4j
public class RateLimitFilter implements Filter {

    private final AppProperties appProperties;
    private final ProxyManager<String> proxyManager;

    public RateLimitFilter(AppProperties appProperties, RedisClient redisClient) {
        this.appProperties = appProperties;
        StatefulRedisConnection<String, byte[]> connection =
                redisClient.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));
        this.proxyManager = LettuceBasedProxyManager.builderFor(connection)
                .withExpirationStrategy(
                        ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(
                                Duration.ofMinutes(2)))
                .build();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest  httpRequest  = (HttpServletRequest)  request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String uri      = httpRequest.getRequestURI();
        String method   = httpRequest.getMethod();
        String clientIp = resolveClientIp(httpRequest);

        if ("POST".equals(method) && uri.contains("/api/locality/search")) {
            if (!tryConsume("search:" + clientIp,
                    appProperties.getRateLimit().getSearchRequestsPerMinute())) {
                log.warn("Search rate limit exceeded for IP: {}", clientIp);
                writeRateLimitResponse(httpResponse);
                return;
            }
        } else if ("POST".equals(method) && uri.contains("/api/review")) {
            if (!tryConsume("review:" + clientIp,
                    appProperties.getRateLimit().getReviewRequestsPerMinute())) {
                log.warn("Review rate limit exceeded for IP: {}", clientIp);
                writeRateLimitResponse(httpResponse);
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private boolean tryConsume(String bucketKey, int requestsPerMinute) {
        Supplier<BucketConfiguration> configSupplier = () -> BucketConfiguration.builder()
                .addLimit(Bandwidth.classic(
                        requestsPerMinute,
                        Refill.greedy(requestsPerMinute, Duration.ofMinutes(1))))
                .build();

        return proxyManager.builder()
                .build(bucketKey, configSupplier)
                .tryConsume(1);
    }

    private void writeRateLimitResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.getWriter().write(
                "{\"success\":false,\"message\":\"Too many requests. Please try again later.\"}");
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}