package com.notificationservice.service;

import com.notificationservice.config.RabbitMQConfig;
import com.notificationservice.model.*;
import com.notificationservice.repository.DeliveryLogRepository;
import com.notificationservice.repository.EventRepository;
import com.notificationservice.repository.WebhookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {

    private final WebhookRepository webhookRepository;
    private final EventRepository eventRepository;
    private final DeliveryLogRepository deliveryLogRepository;
    private final RabbitTemplate rabbitTemplate;
    private final RateLimiterService rateLimiterService;

    public Event processEvent(Event event) {

        // 1. Rate limit check
        if (!rateLimiterService.isAllowed(event.getSourceApp())) {
            throw new RuntimeException("Rate limit exceeded for app: " + event.getSourceApp());
        }

        // 2. Save event
        Event savedEvent = eventRepository.save(event);

        log.info("Event received | ID: {} | Type: {} | App: {}",
                savedEvent.getId(), savedEvent.getEventType(), savedEvent.getSourceApp());

        // 3. Find matching webhooks
        List<Webhook> matchingWebhooks =
                webhookRepository.findByEventTypeAndActiveTrue(event.getEventType());

        if (matchingWebhooks.isEmpty()) {
            log.warn("No active webhooks found for event type: {}", event.getEventType());
            return savedEvent;
        }

        // 4. Fan-out
        for (Webhook webhook : matchingWebhooks) {

            DeliveryLog deliveryLog = DeliveryLog.builder()
                    .event(savedEvent)
                    .webhook(webhook)
                    .channel(webhook.getChannel())
                    .status(DeliveryStatus.PENDING)
                    .build();

            deliveryLogRepository.save(deliveryLog);

            NotificationMessage message = NotificationMessage.builder()
                    .eventId(savedEvent.getId())
                    .webhookId(webhook.getId())
                    .eventType(savedEvent.getEventType())
                    .target(webhook.getTarget())
                    .channel(webhook.getChannel())
                    .payload(savedEvent.getPayload())
                    .appName(savedEvent.getSourceApp())
                    .build();

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE,
                    webhook.getChannel().name(),
                    message
            );

            log.info("Queued notification | WebhookId: {} | Channel: {} | Target: {}",
                    webhook.getId(), webhook.getChannel(), webhook.getTarget());
        }

        log.info("Event {} fanned out to {} webhook(s)",
                savedEvent.getId(), matchingWebhooks.size());

        return savedEvent;
    }

    public Event getById(UUID id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found: " + id));
    }
}