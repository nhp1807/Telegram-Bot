package org.example.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "sent_messages")
public class SentMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "message", nullable = false)
    String message;

    @Column(name = "sent_at", nullable = false)
    Long sentAt;

    @ManyToOne
    @JoinColumn(name = "service_id", nullable = false)
    Service service;

    public SentMessage() {
    }

    public SentMessage(String message, Long sentAt, Service service) {
        this.message = message;
        this.sentAt = sentAt;
        this.service = service;
    }
}
