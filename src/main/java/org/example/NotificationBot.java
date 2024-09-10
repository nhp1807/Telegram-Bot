package org.example;

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

            // Xử lý tin nhắn từ user nếu cần
            if (messageText.equals("/start")) {
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText("Xin chào! Bot của bạn đã sẵn sàng.");

                try {
                    execute(message);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void checkConditionAndNotify() {
        boolean condition = checkSomeCondition(); // Giả sử có một điều kiện nào đó

        if (condition) {
            sendNotificationToUser();
        }
    }

    // Hàm giả lập kiểm tra điều kiện
    private boolean checkSomeCondition() {
        return true;
    }

    // Hàm gửi thông báo cho user cụ thể
    private void sendNotificationToUser() {
        SendMessage message = new SendMessage();
        message.setChatId(USER_CHAT_ID);
        message.setText("Thông báo: Điều kiện đã được thỏa mãn!");

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
