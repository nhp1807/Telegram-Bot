package org.example.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.example.enums.Operator;

@Getter
@Setter
@Entity
@Table(name = "safe_bouderies")
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class SafeBoundery {
    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    Long id;
    @Column(name = "operator", nullable = false)
    Operator operator;
    @Column(name = "value1")
    Double value1;
    @Column(name = "value2")
    Double value2;
    @Column(name = "string")
    String string;

    @OneToOne
    @MapsId
    @JoinColumn(name = "field_id")
    Field field;

    public SafeBoundery() {
    }

    public SafeBoundery(Operator operator, Double value1, Double value2, String string) {
        this.operator = operator;
        this.value1 = value1;
        this.value2 = value2;
        this.string = string;
    }
}
