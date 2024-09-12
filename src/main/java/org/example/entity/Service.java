package org.example.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;
import org.example.enums.Category;

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
    @Column(name = "name", nullable = false, unique = true)
    private String name;
    @Column(name = "category", nullable = false)
    private Category category;
    @Column(name = "token", nullable = false)
    private String token;
    @Column(name = "owner", nullable = false)
    private String owner;
    @Column(name = "created_at", nullable = false)
    private Long createdAt;
    @Column(name = "updated_at", nullable = false)
    private Long updatedAt;
    @ManyToMany(mappedBy = "services")
    private Set<User> users = new HashSet<>();

    public Service() {
    }

    public Service(String name, Category category, String token, String owner, Long createdAt, Long updatedAt) {
        this.name = name;
        this.category = category;
        this.token = token;
        this.owner = owner;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
