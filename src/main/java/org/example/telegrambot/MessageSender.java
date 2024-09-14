package org.example.telegrambot;

public interface MessageSender {
    void sendMessage(String text, String chatId);
}
