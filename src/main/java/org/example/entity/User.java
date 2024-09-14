package org.example.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.HashSet;
import java.util.Set;

@Setter
@Getter
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
@Entity
@Table(name = "user")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "id_telegram", nullable = false)
    String idTelegram;

    @Column(name = "username", nullable = false)
    String username;

    @OneToMany(
            mappedBy = "user",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER)
    Set<Email> emails = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "user_service",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "service_id")
    )
    Set<Service> services = new HashSet<>();

    public User() {
    }

    public User(String idTelegram, String username) {
        this.idTelegram = idTelegram;
        this.username = username;
    }

    // Thêm email vào User
    public void addEmail(Email email) {
        emails.add(email);
        email.setUser(this);  // Thiết lập quan hệ với User
    }

    // Xóa email khỏi User
    public void removeEmail(Email email) {
        emails.remove(email);
        email.setUser(null);  // Hủy quan hệ với User
    }
}


