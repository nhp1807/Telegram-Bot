package org.example.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Setter
@Getter
@Entity
@Table(name = "sent_warnings")
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class SentWarning {
    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    Long id;

    @Column(name = "message", nullable = false)
    String message;

    @Column(name = "sent_at", nullable = false)
    Long sentAt;

    @ManyToOne
    @JoinColumn(name = "service_id", nullable = false)
    Service service;

    public SentWarning() {
    }

    public SentWarning(String message, Long sentAt, Service service) {
        this.message = message;
        this.sentAt = sentAt;
        this.service = service;
    }
}
