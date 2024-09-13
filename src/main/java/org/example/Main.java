package org.example;

import org.example.database.HibernateUtil;
import org.example.entity.Category;
import org.example.entity.Service;
import org.example.entity.User;
import org.example.service.MainService;
import org.example.service.ServiceService;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import spark.Spark;

import java.util.Date;


public class Main {
    public static void main(String[] args) throws TelegramApiException {
        sendNoticeTele();
    }

//    public static void demoService(){
//        // Mở session từ Hibernate
//        Session session = HibernateUtil.getSessionFactory().openSession();
//        Transaction transaction = session.beginTransaction();
//
//        // Tạo các đối tượng User
//        User user1 = new User(1L, "user1");
//        User user2 = new User(2L,"user2");
//
//        // Tạo đối tượng Service1 với user1 và user2
//        Service service1 = new Service("service1", "token1", user1.getUsername(), System.currentTimeMillis(), System.currentTimeMillis());
//        service1.getUsers().add(user1);
//        service1.getUsers().add(user2);
//
//        // Thêm Service1 vào danh sách services của user1 và user2
//        user1.getServices().add(service1);
//        user2.getServices().add(service1);
//
//        // Lưu user1, user2 và service1 vào cơ sở dữ liệu
//        session.save(user1);
//        session.save(user2);
//        session.save(service1);
//
//        // Tạo đối tượng Service2 và chỉ thêm user2
//        Service service2 = new Service("service2", "token2", user2.getUsername(), System.currentTimeMillis(), System.currentTimeMillis());
//        service2.getUsers().add(user2);
//
//        // Thêm Service2 vào danh sách services của user2
//        user2.getServices().add(service2);
//
//        // Lưu service2 vào cơ sở dữ liệu
//        session.save(service2);
//
//        // Commit transaction
//        transaction.commit();
//
//        // Đóng session
//        session.close();
//    }

    public static void sendNoticeTele() throws TelegramApiException {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);

        NotificationBot bot = new NotificationBot();
        bot.readPropertiesFile();

        try {
            botsApi.registerBot(bot);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}