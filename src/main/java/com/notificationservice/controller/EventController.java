package com.notificationservice.controller; import com.notificationservice.model.Event; import com.notificationservice.model.Webhook; import com.notificationservice.repository.WebhookRepository; import com.notificationservice.service.EventService;
import com.notificationservice.service.WebhookService;

import lombok.RequiredArgsConstructor; import org.springframework.http.HttpStatus; import org.springframework.http.ResponseEntity; import org.springframework.web.bind.annotation.*; import java.util.Map; import java.util.Optional; import java.util.UUID;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final WebhookService webhookService;
    private final EventService eventService;

    @PostMapping
    public ResponseEntity<?> receiveEvent(
            @RequestBody Event event,
            @RequestHeader("X-Secret-Key") String secretKey) {

        Optional<Webhook> webhook = webhookService.findBySecretKey(secretKey);

        if (webhook.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid secret key"));
        }

        event.setSourceApp(webhook.get().getAppName());

        Event saved = eventService.processEvent(event);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
                "eventId", saved.getId(),
                "status", "QUEUED",
                "message", "Event received and queued for delivery"
        ));
    }
}