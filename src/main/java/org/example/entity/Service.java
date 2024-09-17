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

    @Column(name = "token", nullable = false, unique = true)
    private String token;

    @Column(name = "owner", nullable = false)
    private String owner;

    @Column(name = "created_at", nullable = false)
    private Long createdAt;

    @Column(name = "updated_at", nullable = false)
    private Long updatedAt;

    @ManyToMany(mappedBy = "services")
    private Set<User> users = new HashSet<>();

    @OneToMany(
            mappedBy = "service",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER)
    private Set<Field> fields = new HashSet<>();

    public Service() {
    }

    public Service(String name, Category category, String owner, Long createdAt, Long updatedAt) {
        this.name = name;
        this.category = category;
        this.owner = owner;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void addUser(User user) {
        users.add(user);
        user.getServices().add(this);
    }

    public void removeUser(User user) {
        users.remove(user);
        user.getServices().remove(this);
    }

    public void addField(Field field) {
        fields.add(field);
        field.setService(this);
    }

    public void removeField(Field field) {
        fields.remove(field);
        field.setService(null);
    }
}
