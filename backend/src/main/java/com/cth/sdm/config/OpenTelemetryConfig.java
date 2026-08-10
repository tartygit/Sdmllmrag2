package com.cth.sdm.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.logging.Logger;

@Configuration
public class OpenTelemetryConfig {

    private static final Logger logger = Logger.getLogger(OpenTelemetryConfig.class.getName());

    @Bean
    public OpenTelemetryTraceService traceService() {
        return new OpenTelemetryTraceService();
    }

    public static class OpenTelemetryTraceService {

        public void startSpan(String spanName) {
            // Log distributed instrumentation metrics trace spans
            logger.info("[OpenTelemetry Trace] Starting trace span: " + spanName);
        }

        public void endSpan(String spanName) {
            logger.info("[OpenTelemetry Trace] Closing trace span: " + spanName);
        }

        public void logEvent(String spanName, String eventDescription) {
            logger.info("[OpenTelemetry Trace] span: " + spanName + " | Event: " + eventDescription);
        }
    }
}
