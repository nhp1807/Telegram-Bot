package org.example.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Entity
@Table(name = "emails")
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class Email {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "email_address", nullable = false, unique = true)
    String emailAddress;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    public Email() {
    }

    public Email(String emailAddress, User user) {
        this.emailAddress = emailAddress;
        this.user = user;
    }
}
