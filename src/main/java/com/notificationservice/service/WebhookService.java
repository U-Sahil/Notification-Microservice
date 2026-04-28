package com.notificationservice.service;

import com.notificationservice.model.Webhook;
import com.notificationservice.repository.WebhookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class WebhookService {

	private final WebhookRepository webhookRepository;

	public WebhookService(WebhookRepository webhookRepository) {
	    this.webhookRepository = webhookRepository;
	}

    public Webhook register(Webhook webhook) {
        log.info("Registering webhook for app: {} | Event: {} | Channel: {}",
                webhook.getAppName(), webhook.getEventType(), webhook.getChannel());
        return webhookRepository.save(webhook);
    }

    public Webhook getById(UUID id) {
        return webhookRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Webhook not found: " + id));
    }

    public List<Webhook> getByApp(String appName) {
        return webhookRepository.findByAppName(appName);
    }

    public Webhook update(UUID id, Webhook updatedData) {
        Webhook existing = getById(id);
        existing.setTarget(updatedData.getTarget());
        existing.setChannel(updatedData.getChannel());
        existing.setEventType(updatedData.getEventType());
        return webhookRepository.save(existing);
    }

    public void deactivate(UUID id) {
        Webhook webhook = getById(id);
        webhook.setActive(false);
        webhookRepository.save(webhook);
        log.info("Deactivated webhook: {}", id);
    }

    public List<Webhook> findActiveByEventType(String eventType) {
        return webhookRepository.findByEventTypeAndActiveTrue(eventType);
    }

	public Optional<Webhook> findBySecretKey(String secretKey) {
		// TODO Auto-generated method stub
		return null;
	}
}
