package com.notificationservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "delivery_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryLog {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "event_id", nullable = false)
	private Event event;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "webhook_id", nullable = false)
	private Webhook webhook;

	@Enumerated(EnumType.STRING)
	private NotificationChannel channel;

	@Enumerated(EnumType.STRING)
	@Builder.Default
	private DeliveryStatus status = DeliveryStatus.PENDING;

	@Builder.Default
	private int attemptCount = 0;

	private LocalDateTime lastAttemptedAt;

	private String failureReason;

	private LocalDateTime createdAt;

	@PrePersist
	public void prePersist() {
		this.createdAt = LocalDateTime.now();
	}
}
