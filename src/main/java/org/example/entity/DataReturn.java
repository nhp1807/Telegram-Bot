package org.example.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "datas_return")
public class DataReturn {
    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;

    @Column(name = "data", nullable = false)
    private String data;

    @Column(name = "created_at", nullable = false)
    private Long createdAt;

    @ManyToOne
    @JoinColumn(name = "service_id", nullable = false)
    Service service;

    public DataReturn() {
    }

    public DataReturn(String data, Long createdAt, Service service) {
        this.data = data;
        this.createdAt = createdAt;
        this.service = service;
    }
}
