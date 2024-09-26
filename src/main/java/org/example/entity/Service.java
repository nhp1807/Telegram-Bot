package org.example.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;
import lombok.experimental.FieldDefaults;
import org.example.enums.Category;
import org.example.repository.DataReturnRepository;
import org.example.repository.UserRepository;
import org.example.telegrambot.BotSingleton;
import org.example.telegrambot.NotificationBot;
import org.json.JSONObject;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Setter
@Getter
@Entity
@Table(name = "service")
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class Service implements Runnable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "name", nullable = false, unique = true)
    String name;

    @Column(name = "category", nullable = false)
    Category category;

    @Column(name = "token", nullable = false, unique = true)
    String token;

    @Column(name = "owner", nullable = false)
    Long owner;
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "owner_id", nullable = false)
//    User owner;

    @Column(name = "created_at", nullable = false)
    Long createdAt;

    @Column(name = "updated_at", nullable = false)
    Long updatedAt;

    @Column(name = "warning_duration")
    Long warningDuration;

    @ManyToMany(mappedBy = "services", fetch = FetchType.EAGER)
    Set<User> users = new HashSet<>();

    @OneToMany(mappedBy = "service", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    Set<Field> fields = new HashSet<>();

    @OneToMany(mappedBy = "service", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    Set<SentWarning> sentWarnings = new HashSet<>();

    @OneToMany(mappedBy = "service", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    Set<SentMessage> sentMessages = new HashSet<>();

    @OneToMany(mappedBy = "service", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    Set<DataReturn> dataReturns = new HashSet<>();

    private volatile boolean stopTask = false; // Cờ để dừng luồng

    public Service() {
    }

    public Service(String name, Category category, Long owner, Long createdAt, Long updatedAt, Long warningDuration) {
        this.name = name;
        this.category = category;
        this.owner = owner;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.warningDuration = warningDuration;
    }

//    public Service(String name, Category category, Long createdAt, Long updatedAt, Long warningDuration) {
//        this.name = name;
//        this.category = category;
//        this.createdAt = createdAt;
//        this.updatedAt = updatedAt;
//        this.warningDuration = warningDuration;
//    }

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
        return "Service{" + "id=" + id + ", name='" + name + '\'' + ", category=" + category + ", token='" + token + '\'' + ", owner='" + owner + '\'' + ", createdAt=" + createdAt + ", updatedAt=" + updatedAt + ", users=" + users + ", fields=" + fields + '}';
    }

    public JSONObject toJson() {
        UserRepository userRepository = new UserRepository();
        User user = userRepository.findUserById(owner);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", id);
        jsonObject.put("name", name);
        jsonObject.put("category", category);
        jsonObject.put("token", token);
        jsonObject.put("owner", owner);
        jsonObject.put("created_at", createdAt);
        jsonObject.put("updated_at", updatedAt);
        return jsonObject;
    }

    @Override
    public void run() {
        try {
            while (!stopTask) {
                checkAndExecuteTask(); // Kiểm tra điều kiện
                Thread.sleep(30000); // Kiểm tra mỗi 30 giây
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Dừng luồng khi có ngắt
        }
    }

    public void stopTask() {
        this.stopTask = true; // Cờ để dừng luồng
    }

    public void checkAndExecuteTask() {
        if (warningDuration > 0) {
            DataReturnRepository dataReturnRepository = new DataReturnRepository();
            DataReturn dataReturn = dataReturnRepository.getLatestDataReturn(id);

            long currentTime = System.currentTimeMillis();

            if (dataReturn != null) {
                long timeElapsed = (currentTime - dataReturn.getCreatedAt()) / 1000 / 60; // Tính thời gian đã trôi qua theo phút

                if (timeElapsed >= warningDuration) {
                    executeTask(); // Thực hiện nhiệm vụ nếu quá thời gian cảnh báo
                }
            } else {
                System.out.println("Service: " + name + " chưa có DataReturn nào.");
            }
        }
    }

    public void executeTask() {
        NotificationBot bot = BotSingleton.getInstance();
        for (User u : users) {
            System.out.println("Sent warning to user: " + u.getIdTelegram());
            bot.sendMessage("Service \"" + name + "\" đã vượt quá thời gian cảnh báo.", u.getIdTelegram());
        }
        System.out.println("Thực hiện nhiệm vụ cho Service: " + name + " vào lúc " + LocalDate.now());
    }
}
