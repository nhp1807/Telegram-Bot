package org.example;

import org.example.database.HibernateUtil;
import org.example.entity.User;
import org.example.service.SendMailConfirm;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class NotificationBot extends TelegramLongPollingBot {
    private String USER_CHAT_ID;
    private String BOT_NAME;
    private String BOT_TOKEN;

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            String chatId = update.getMessage().getChatId().toString();
            String username = update.getMessage().getChat().getUserName();

            if (messageText.equals("/start")) {
                sendMessage("Bạn đã bắt đầu nhận thông báo từ bot!");
                sendMessage("Hãy nhập email mà bạn đã đăng ký Budibase!");
            } else if (messageText.contains("@tech.admicro.vn")) {
                String output = "Bạn đã đăng ký bằng email: " + messageText + ", hãy vào email để xác nhận!";

                SendMailConfirm sendMailConfirm = new SendMailConfirm();
                sendMailConfirm.sendMail(chatId);

                Session session = HibernateUtil.getSessionFactory().openSession();
                Transaction transaction = session.beginTransaction();
                User user = new User(Long.parseLong(chatId), username, messageText);
                session.save(user);
                transaction.commit();
                session.close();

                sendMessage(output);
            }

        }
    }

    public void sendMessage(String input) {
        SendMessage message = new SendMessage();
        message.setText(input);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getBotUsername() {
        return BOT_NAME;
    }

    @Override
    public String getBotToken() {
        return BOT_TOKEN;
    }

    // Read data from properties file
    public void readPropertiesFile() {
        Properties prop = new Properties();
        InputStream input;

        try {
            input = new FileInputStream("src/main/resources/application.properties");

            // load a properties file
            prop.load(input);

            USER_CHAT_ID = prop.getProperty("USER_CHAT_ID");
            BOT_NAME = prop.getProperty("BOT_NAME");
            BOT_TOKEN = prop.getProperty("BOT_TOKEN");

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
