package com.cth.sdm.infrastructure.messaging;

import com.cth.sdm.application.dto.DocumentIngestedEvent;
import com.cth.sdm.infrastructure.config.RabbitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RabbitEventDispatcher {

    private final RabbitTemplate rabbitTemplate;

    public void dispatchDocumentIngestedEvent(DocumentIngestedEvent event) {
        try {
            log.info("Dispatching DocumentIngestedEvent to RabbitMQ: docId={}", event.getDocumentId());
            rabbitTemplate.convertAndSend(
                    RabbitConfig.EXCHANGE_NAME,
                    RabbitConfig.ROUTING_KEY,
                    event
            );
            log.info("Successfully dispatched DocumentIngestedEvent to RabbitMQ.");
        } catch (Exception e) {
            log.error("Failed to dispatch DocumentIngestedEvent to RabbitMQ: {}", e.getMessage());
            // Safe fallback logging, we don't block pipeline if RabbitMQ fails or is unreachable
        }
    }
}
