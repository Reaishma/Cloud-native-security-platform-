
package com.cloudnative.service;

package com.cloudnative.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
public class MonitoringService {
    private static final Logger logger = LoggerFactory.getLogger(MonitoringService.class);
    private static final Logger securityLogger = LoggerFactory.getLogger("SECURITY");
    private static final Logger performanceLogger = LoggerFactory.getLogger("PERFORMANCE");
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public void logSecurityEvent(String eventType, String userId, String details) {
        try {
            Map<String, Object> securityEvent = new HashMap<>();
            securityEvent.put("timestamp", Instant.now().toString());
            securityEvent.put("eventType", eventType);
            securityEvent.put("userId", userId);
            securityEvent.put("details", details);
            securityEvent.put("severity", "HIGH");
            securityEvent.put("source", "cloud-native-app");
            
            MDC.put("eventType", eventType);
            MDC.put("userId", userId);
            
            securityLogger.warn("Security Event: {}", objectMapper.writeValueAsString(securityEvent));
            
            // Trigger alert for critical events
            if (isCriticalSecurityEvent(eventType)) {
                triggerSecurityAlert(securityEvent);
            }
        } catch (Exception e) {
            logger.error("Failed to log security event", e);
        } finally {
            MDC.clear();
        }
    }
    
    public void logPerformanceMetrics(String operation, long duration, boolean success) {
        try {
            Map<String, Object> perfMetrics = new HashMap<>();
            perfMetrics.put("timestamp", Instant.now().toString());
            perfMetrics.put("operation", operation);
            perfMetrics.put("duration_ms", duration);
            perfMetrics.put("success", success);
            perfMetrics.put("source", "cloud-native-app");
            
            MDC.put("operation", operation);
            MDC.put("duration", String.valueOf(duration));
            
            performanceLogger.info("Performance Metric: {}", objectMapper.writeValueAsString(perfMetrics));
            
            // Alert on slow operations
            if (duration > 5000) { // 5 seconds threshold
                triggerPerformanceAlert(perfMetrics);
            }
        } catch (Exception e) {
            logger.error("Failed to log performance metrics", e);
        } finally {
            MDC.clear();
        }
    }
    
    public void logApplicationEvent(String eventType, Map<String, Object> data) {
        try {
            Map<String, Object> appEvent = new HashMap<>();
            appEvent.put("timestamp", Instant.now().toString());
            appEvent.put("eventType", eventType);
            appEvent.put("data", data);
            appEvent.put("source", "cloud-native-app");
            
            logger.info("Application Event: {}", objectMapper.writeValueAsString(appEvent));
        } catch (Exception e) {
            logger.error("Failed to log application event", e);
        }
    }
    
    private boolean isCriticalSecurityEvent(String eventType) {
        return eventType.equals("UNAUTHORIZED_ACCESS") || 
               eventType.equals("SQL_INJECTION_ATTEMPT") ||
               eventType.equals("XSS_ATTEMPT") ||
               eventType.equals("BRUTE_FORCE_ATTACK");
    }
    
    private void triggerSecurityAlert(Map<String, Object> event) {
        logger.error("CRITICAL SECURITY ALERT: {}", event);
        // In production, this would integrate with notification systems
        // like AWS SNS, PagerDuty, or email alerts
    }
    
    private void triggerPerformanceAlert(Map<String, Object> metrics) {
        logger.warn("PERFORMANCE ALERT: {}", metrics);
        // In production, this would trigger performance monitoring alerts
    }
}
