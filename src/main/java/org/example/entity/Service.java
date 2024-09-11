package org.example.entity;

import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Setter
@Getter
@Entity
@Table(name = "service")
public class Service {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String name;
    @Column(nullable = false)
    private String token;
    @Column(nullable = false)
    private String owner;
    @Column(nullable = false)
    private Long createdAt;
    @Column(nullable = false)
    private Long updatedAt;
    @ManyToMany(mappedBy = "services")
    private Set<User> users = new HashSet<>();

    public Service() {
    }

    public Service(String name, String token, String owner, Long createdAt, Long updatedAt) {
        this.name = name;
        this.token = token;
        this.owner = owner;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
