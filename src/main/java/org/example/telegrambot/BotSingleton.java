package org.example.telegrambot;

public class BotSingleton {
    private static NotificationBot instance;

    private BotSingleton() {}

    public static NotificationBot getInstance() {
        if (instance == null) {
            instance = new NotificationBot();
        }
        return instance;
    }
}
