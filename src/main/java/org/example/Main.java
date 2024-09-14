package org.example;

import org.example.entity.User;
import org.example.repository.UserRepository;
import org.example.telegrambot.NotificationBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;


public class Main {
    public static void main(String[] args) throws TelegramApiException {
        sendNoticeTele();

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