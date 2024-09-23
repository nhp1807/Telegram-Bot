package org.example.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Setter
@Getter
@Entity
@Table(name = "sent_messages")
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class SentMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "message", nullable = false)
    String message;

    @Column(name = "sent_at", nullable = false)
    Long sentAt;

    @Column(name = "is_read", nullable = false)
    Boolean isRead;

    @ManyToOne
    @JoinColumn(name = "service_id", nullable = false)
    Service service;

    public SentMessage() {
    }

    public SentMessage(String message, Long sentAt, Service service) {
        this.message = message;
        this.sentAt = sentAt;
        this.service = service;
        this.isRead = false;
    }
}
