package org.example.service;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.*;

public class SendMailConfirm {
    private static String SMTP_HOST;
    private static String SMTP_PORT;
    private static String SMTP_USERNAME;
    private static String SMTP_PASSWORD;
    private static String EMAIL_FROM;


    public static void main(String[] args) {

    }

    public void sendMail(String text, String sendTo){
        readPropertiesFile();

        // Cấu hình các thuộc tính cho phiên email
        Properties properties = new Properties();
        properties.put("mail.smtp.host", SMTP_HOST);
        properties.put("mail.smtp.port", SMTP_PORT);
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");

        // Tạo phiên gửi email với xác thực
        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SMTP_USERNAME, SMTP_PASSWORD);
            }
        });

        try {
            // Tạo đối tượng tin nhắn
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(EMAIL_FROM));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(sendTo));
            message.setSubject("Confirm your email");

            // Nội dung HTML với link POST tới API
            String htmlContent = text;

            // Đặt nội dung HTML cho email
            message.setContent(htmlContent, "text/html; charset=UTF-8");

            // Gửi email
            Transport.send(message);

            System.out.println("Email sent successfully with HTML content!");

        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    public void readPropertiesFile() {
        Properties prop = new Properties();
        InputStream input;

        try {
            input = new FileInputStream("src/main/resources/application.properties");

            // load a properties file
            prop.load(input);

            SMTP_HOST = prop.getProperty("SMTP_HOST");
            SMTP_PORT = prop.getProperty("SMTP_PORT");
            SMTP_USERNAME = prop.getProperty("SMTP_USERNAME");
            SMTP_PASSWORD = prop.getProperty("SMTP_PASSWORD");
            EMAIL_FROM = prop.getProperty("EMAIL_FROM");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
