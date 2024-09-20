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
    boolean isMonitor;

    @ManyToOne
    @JoinColumn(name = "service_id", nullable = false)
    Service service;

    @OneToOne(mappedBy = "field", cascade = CascadeType.ALL)
    @PrimaryKeyJoinColumn
    SafeBoundery safeBoundery;

    public Field() {
    }

    public Field(String name, FieldType type, boolean isMonitor, Service service) {
        this.name = name;
        this.type = type;
        this.isMonitor = isMonitor;
        this.service = service;
    }
}
