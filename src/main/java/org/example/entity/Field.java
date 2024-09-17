package org.example.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.example.enums.FieldType;

@Getter
@Setter
@Entity
@Table(name = "fields")
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class Field {
    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    Long id;

    @Column(name = "name", nullable = false)
    String name;

    @Column(name = "type", nullable = false)
    FieldType type;

    @Column(name = "is_monitor", nullable = false)
    boolean is_monitor;

    @ManyToOne
    @JoinColumn(name = "service_id", nullable = false)
    Service service;

    public Field() {
    }

    public Field(String name, FieldType type, boolean is_monitor, Service service) {
        this.name = name;
        this.type = type;
        this.is_monitor = is_monitor;
        this.service = service;
    }
}
