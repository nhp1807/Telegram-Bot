package org.example.test;

import org.example.telegrambot.MessageSender;

public class EventProcessor {
    private MessageSender messageSender;

    public EventProcessor(MessageSender messageSender) {
        this.messageSender = messageSender;
    }

    public void processEvent(String text, String chatId) {
        // Gọi hàm sendMessage từ đối tượng messageSender
        messageSender.sendMessage(text, chatId);
    }
}
