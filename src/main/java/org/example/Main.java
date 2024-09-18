package org.example;

import org.example.database.HibernateUtil;
import org.example.entity.Service;
import org.example.entity.User;
import org.example.enums.Category;
import org.example.repository.UserRepository;
import org.example.telegrambot.NotificationBot;
import org.hibernate.Session;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.UUID;


public class Main {
    public static void main(String[] args) throws TelegramApiException {
//        sendNoticeTele();

//        UserRepository userRepository = new UserRepository();
//        Session session = HibernateUtil.getSessionFactory().openSession();
//
//        Service service1 = new Service("service 1", Category.CAT1, "5619763598", System.currentTimeMillis(), System.currentTimeMillis());
//        service1.setToken(UUID.randomUUID().toString());
//        User user = userRepository.findUserByIdTelegram("6784049826");
//        service1.setOwner(user.getIdTelegram());
//        user.getServices().add(service1);
//
//        session.beginTransaction();
//        session.save(service1);
//        session.update(user);
//
//        session.getTransaction().commit();
//        session.close();
    }

    public static void sendNoticeTele() throws TelegramApiException {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);

        NotificationBot bot = new NotificationBot();

        try {
            botsApi.registerBot(bot);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}