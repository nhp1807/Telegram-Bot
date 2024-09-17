package org.example.telegrambot;

import org.example.database.HibernateUtil;
import org.example.entity.Email;
import org.example.entity.User;
import org.example.repository.UserRepository;
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
import java.util.*;

public class NotificationBot extends TelegramLongPollingBot implements MessageSender {
    private String USER_CHAT_ID;
    private String BOT_NAME;
    private String BOT_TOKEN;

    private UserRepository userRepository;
    // Map để lưu trạng thái lệnh hiện tại của người dùng theo chatId
    private Map<String, String> userCommands = new HashMap<>();
    private SendMailConfirm sendMailConfirm = new SendMailConfirm();

    public NotificationBot() {
        this.userRepository = new UserRepository();
        readPropertiesFile();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            String chatId = update.getMessage().getChatId().toString();

            checkInput(messageText, chatId);
        }
    }

    public void checkInput(String messageText, String chatId){
        if (messageText.equals("/start")){
            sendMessage("Bạn đã bắt đầu nhận thông báo từ bot!", chatId);
            sendMessage("Sử dụng /help để được hỗ trợ", chatId);
            sendMessage("Sử dụng /add_email để thêm email đăng ký Budibase", chatId);
        } else if (messageText.equals("/help")){
            sendMessage(showHelp(), chatId);
        } else if (messageText.equals("/remove_email")){
            sendMessage("Hãy nhập email cần xóa", chatId);
            userCommands.put(chatId, "DELETE_EMAIL");
        } else if (messageText.equals("/add_email")){
            sendMessage("Hãy nhập email để đăng ký Budibase", chatId);
            userCommands.put(chatId, "ADD_EMAIL");
        } else if (messageText.equals("/list_email")){
            User user = userRepository.findUserByIdTelegram(chatId);
            List<Email> emails = user.getEmails().stream().toList();
            StringBuilder sb = new StringBuilder();
            sb.append("Danh sách email của bạn:").append("\n");
            for (Email email : emails) {
                sb.append(email.getEmailAddress()).append("\n");
            }
            sendMessage(sb.toString(), chatId.toString());
        } else if (messageText.contains("@")){
            processEmailInput(chatId, messageText);
        } else {
            sendMessage("Nội dung không hợp lệ!", chatId);
        }
    }

    public void processEmailInput(String chatId, String emailAddress) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = session.beginTransaction();

        // Tìm người dùng trong database
        User user = session.createQuery("FROM User WHERE idTelegram = :idTelegram", User.class)
                .setParameter("idTelegram", chatId)
                .uniqueResult();

        String currentCommand = userCommands.get(chatId);

        if (user == null) {
            // Nếu người dùng chưa tồn tại, tạo mới
            user = new User(chatId);
            session.save(user);
        }

        if ("ADD_EMAIL".equals(currentCommand)) {
            // Thêm email vào user
            Email email = new Email(emailAddress, user);
            user.addEmail(email);
            session.save(email);
            sendMessage("Bạn đã thêm email: " + emailAddress, chatId.toString());
            String text = "<h1>Thêm email thành công!</h1>"
                    + "<p>Nhấn vào đường <a href=\"google.com\" target=\"_blank\">link</a> sau để đăng nhập</p>";
            sendMailConfirm.sendMail(text, chatId, emailAddress);
        } else if ("DELETE_EMAIL".equals(currentCommand)) {
            // Xóa email từ user
            Email emailToRemove = session.createQuery("FROM Email WHERE emailAddress = :emailAddress AND user.id = :userId", Email.class)
                    .setParameter("emailAddress", emailAddress)
                    .setParameter("userId", user.getId())
                    .uniqueResult();

            if (emailToRemove != null) {
                user.removeEmail(emailToRemove);
                session.delete(emailToRemove);
                sendMessage("Email đã được xóa: " + emailAddress, chatId.toString());
                sendMailConfirm.sendMail("Xoá email thành công!", chatId, emailAddress);
            } else {
                sendMessage("Email này không tồn tại trong tài khoản của bạn: " + emailAddress, chatId.toString());
            }
        }

        session.update(user);
        transaction.commit();
        session.close();
        userCommands.remove(chatId); // Xóa trạng thái lệnh sau khi xử lý
    }

    public String showHelp() {
        StringBuilder sb = new StringBuilder();
        sb.append("/add_email: Thêm email đăng ký").append("\n");
        sb.append("/remove_email: Xóa email đăng ký").append("\n");
        sb.append("/list_email: Xem danh sách email đăng ký").append("\n");
        return sb.toString();
    }

    @Override
    public void sendMessage(String text, String chatId) {
        SendMessage message = new SendMessage();
        message.setText(text);
        message.setChatId(chatId);
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
