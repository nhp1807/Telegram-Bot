package org.example.service;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import lombok.extern.slf4j.Slf4j;
import org.example.NotificationBot;
import org.example.database.HibernateUtil;
import org.example.dto.response.ApiResponse;
import org.example.entity.Service;
import org.example.enums.Category;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.json.JSONObject;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import spark.Request;
import spark.Response;
import spark.Spark;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class ServiceService {
    private static AtomicReference<String> storedData = new AtomicReference<>("Chưa có dữ liệu nào được nhận.");

    public static Object createService(Request request, Response response) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = session.beginTransaction();

        String body = request.body();
        JSONObject jsonBody = new JSONObject(body);
        String category = jsonBody.getString("category");
        String name = jsonBody.getString("name");
        String owner = jsonBody.getString("owner");
        Long createdAt = System.currentTimeMillis();
        Long updatedAt = System.currentTimeMillis();

        Service service = new Service(name, Category.valueOf(category), null, owner, createdAt, updatedAt);
        service.setToken(UUID.randomUUID().toString());

        session.save(service);
        transaction.commit();
        session.close();

        JSONObject jsonResponse = new JSONObject();
        jsonResponse.put("token", service.getToken());
        jsonResponse.put("created_at", service.getCreatedAt());

        response.body(jsonResponse.toString());

        return response.body();
    }

    public static Object getAllServices(Request request, Response response) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = session.beginTransaction();

        List<Service> services = session.createQuery("from Service", Service.class).list();

        transaction.commit();
        session.close();

        JSONObject jsonResponse = new JSONObject();
        jsonResponse.put("services", services);

        response.body(jsonResponse.toString());

        return response.body();
    }

    public static Object getServiceById(Request request, Response response) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = session.beginTransaction();

        Long id = Long.parseLong(request.params(":id"));
        Service service = session.get(Service.class, id);

        transaction.commit();
        session.close();

        JSONObject jsonResponse = new JSONObject();

        if (service == null) {
            response.status(404);
            jsonResponse.put("message", "Service not found.");

            response.body(jsonResponse.toString());
            return response.body();
        }

        jsonResponse.put("service", service);

        response.body(jsonResponse.toString());

        return response.body();
    }

    public static Object storeData(Request request, Response response){
        String data = request.body();
        System.out.println("Dữ liệu nhận được: " + data);

        // Lưu trữ dữ liệu vào biến toàn cục
        storedData.set(data);

        response.status(200);
        return "Dữ liệu đã được nhận.";
    }

    public static Object receive(Request request, Response response) {
        return storedData.get();
    }

    public static void sendData(String data) {
        try {
            // URL của API nhận dữ liệu
            URL url = new URL("http://localhost:8080/store-data");

            // Tạo kết nối HTTP
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            // Gửi dữ liệu
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = data.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            // Kiểm tra phản hồi từ service nhận
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                System.out.println("Dữ liệu đã được gửi thành công.");
            } else {
                System.out.println("Lỗi khi gửi dữ liệu: " + responseCode);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Object confirmTelegram(Request request, Response response) {
        String chatId = request.params(":chatId");

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Xác nhận email thành công!");

        try {
            NotificationBot bot = new NotificationBot();
            bot.sendMessage(message.getText(), chatId);
        } catch (Exception e) {
            e.printStackTrace();
        }

        response.status(200);
        return "Dữ liệu đã được gửi.";
    }

    public static void main(String[] args) throws InterruptedException {
        Spark.port(8080);
        Spark.post("/api/service/create", ServiceService::createService);
        Spark.get("/api/service/get-all", ServiceService::getAllServices);
        Spark.get("/api/service/get/:id", ServiceService::getServiceById);
        Spark.post("/store-data", ServiceService::storeData);
        Spark.get("/receive", ServiceService::receive);
        Spark.get("/confirm-telegram/:chatId", ServiceService::confirmTelegram);
    }
}
