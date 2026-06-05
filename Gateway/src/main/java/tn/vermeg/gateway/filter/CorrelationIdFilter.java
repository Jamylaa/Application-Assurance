package tn.vermeg.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(CorrelationIdFilter.class);
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String CORRELATION_ID_MDC_KEY = "correlationId";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        final String correlationId;
        String headerValue = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID_HEADER);
        
        if (headerValue == null || headerValue.isBlank()) {
            correlationId = UUID.randomUUID().toString();
            logger.debug("Generated new correlation ID: {}", correlationId);
        } else {
            correlationId = headerValue;
            logger.debug("Using existing correlation ID: {}", correlationId);
        }

        // Add to MDC for logging
        MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
        
        // Add to response headers
        exchange.getResponse().getHeaders().add(CORRELATION_ID_HEADER, correlationId);
        
        // Add to request attributes for downstream services
        exchange.getAttributes().put(CORRELATION_ID_MDC_KEY, correlationId);

        logger.info("Incoming request: {} {} - Correlation ID: {}", 
            exchange.getRequest().getMethod(), 
            exchange.getRequest().getPath(),
            correlationId);

        final String finalCorrelationId = correlationId;
        return chain.filter(exchange).doFinally(signalType -> {
            logger.info("Request completed: {} {} - Correlation ID: {} - Status: {}", 
                exchange.getRequest().getMethod(), 
                exchange.getRequest().getPath(),
                finalCorrelationId,
                exchange.getResponse().getStatusCode());
            MDC.remove(CORRELATION_ID_MDC_KEY);
        });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
