package org.example.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;
import org.example.enums.Category;
import org.json.JSONObject;

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

    @Column(name = "warning_duration")
    private Long warningDuration;

    @ManyToMany(
            mappedBy = "services",
//            cascade = {CascadeType.PERSIST, CascadeType.MERGE},
            fetch = FetchType.EAGER)
    private Set<User> users = new HashSet<>();

    @OneToMany(
            mappedBy = "service",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER)
    private Set<Field> fields = new HashSet<>();

    @OneToMany(
            mappedBy = "service",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER)
    private Set<SentWarning> sentWarnings = new HashSet<>();

    @OneToMany(
            mappedBy = "service",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER)
    private Set<SentMessage> sentMessages = new HashSet<>();

    @OneToMany(
            mappedBy = "service",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER)
    private Set<DataReturn> dataReturns = new HashSet<>();

    public Service() {
    }

    public Service(String name, Category category, String owner, Long createdAt, Long updatedAt, Long warningDuration) {
        this.name = name;
        this.category = category;
        this.owner = owner;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.warningDuration = warningDuration;
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

    public void addSentWarning(SentWarning sentWarning) {
        sentWarnings.add(sentWarning);
        sentWarning.setService(this);
    }

    public void removeSentWarning(SentWarning sentWarning) {
        sentWarnings.remove(sentWarning);
        sentWarning.setService(null);
    }

    public void addSentMessage(SentMessage sentMessage) {
        sentMessages.add(sentMessage);
        sentMessage.setService(this);
    }

    public void removeSentMessage(SentMessage sentMessage) {
        sentMessages.remove(sentMessage);
        sentMessage.setService(null);
    }

    public void addDataReturn(DataReturn dataReturn) {
        dataReturns.add(dataReturn);
        dataReturn.setService(this);
    }

    public void removeDataReturn(DataReturn dataReturn) {
        dataReturns.remove(dataReturn);
        dataReturn.setService(null);
    }

    @Override
    public String toString() {
        return "Service{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", category=" + category +
                ", token='" + token + '\'' +
                ", owner='" + owner + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", users=" + users +
                ", fields=" + fields +
                '}';
    }

    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", id);
        jsonObject.put("name", name);
        jsonObject.put("category", category);
        jsonObject.put("token", token);
        jsonObject.put("owner", owner);
        jsonObject.put("created_at", createdAt);
        jsonObject.put("updated_at", updatedAt);
        jsonObject.put("warning_duration", warningDuration);
        return jsonObject;
    }
}
