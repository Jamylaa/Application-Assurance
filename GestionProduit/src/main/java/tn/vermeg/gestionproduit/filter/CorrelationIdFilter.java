package tn.vermeg.gestionproduit.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(CorrelationIdFilter.class);
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String CORRELATION_ID_MDC_KEY = "correlationId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        String correlationId = httpRequest.getHeader(CORRELATION_ID_HEADER);
        
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
            logger.debug("Generated new correlation ID: {}", correlationId);
        } else {
            logger.debug("Using existing correlation ID: {}", correlationId);
        }

        // Add to MDC for logging
        MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
        
        // Add to response headers
        httpResponse.setHeader(CORRELATION_ID_HEADER, correlationId);

        logger.info("Incoming request: {} {} - Correlation ID: {}", 
            httpRequest.getMethod(), 
            httpRequest.getRequestURI(),
            correlationId);

        try {
            chain.doFilter(request, response);
        } finally {
            logger.info("Request completed: {} {} - Correlation ID: {} - Status: {}", 
                httpRequest.getMethod(), 
                httpRequest.getRequestURI(),
                correlationId,
                httpResponse.getStatus());
            MDC.remove(CORRELATION_ID_MDC_KEY);
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);
    }

    @Override
    public void destroy() {
        Filter.super.destroy();
    }
}
