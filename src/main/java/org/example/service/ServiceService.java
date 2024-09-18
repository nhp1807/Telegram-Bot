package org.example.service;

import lombok.extern.slf4j.Slf4j;
import org.example.entity.Field;
import org.example.entity.SafeBoundery;
import org.example.entity.User;
import org.example.enums.FieldType;
import org.example.enums.Operator;
import org.example.repository.UserRepository;
import org.example.telegrambot.NotificationBot;
import org.example.database.HibernateUtil;
import org.example.entity.Service;
import org.example.enums.Category;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.json.JSONArray;
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
    private static UserRepository userRepository = new UserRepository();

    public static Object createService(Request request, Response response) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = session.beginTransaction();

        try {


            String body = request.body();
            JSONObject jsonBody = new JSONObject(body);
            String category = jsonBody.getString("category");
            String name = jsonBody.getString("name");
            String owner = jsonBody.getString("owner");
            JSONArray members = jsonBody.getJSONArray("members");
            JSONArray fields = jsonBody.getJSONArray("fields");
            members.put(owner);
            Long createdAt = System.currentTimeMillis();
            Long updatedAt = System.currentTimeMillis();

            Service service = new Service(name, Category.valueOf(category), owner, createdAt, updatedAt);
            service.setToken(UUID.randomUUID().toString());

            for (Object field : fields) {
                JSONObject fieldJson = (JSONObject) field;
                String fieldName = fieldJson.getString("name");
                String fieldType = fieldJson.getString("type");
                boolean isMonitor = fieldJson.getBoolean("is_monitor");

                // Tạo Field mới và thêm vào Service
                Field fieldNew = new Field(fieldName, FieldType.valueOf(fieldType), isMonitor, service);
                service.addField(fieldNew);

                if (isMonitor) {
                    SafeBoundery safeBoundery = new SafeBoundery();
                    safeBoundery.setOperator(Operator.valueOf(fieldJson.getString("operator")));

                    // Nếu trường tồn tại nhưng có giá trị null, sẽ lưu null
                    safeBoundery.setValue1(fieldJson.isNull("value1") ? null : fieldJson.optInt("value1"));
                    safeBoundery.setValue2(fieldJson.isNull("value2") ? null : fieldJson.optInt("value2"));
                    safeBoundery.setString(fieldJson.isNull("string") ? null : fieldJson.optString("string"));

                    fieldNew.setSafeBoundery(safeBoundery); // Liên kết SafeBoundery với Field
                    safeBoundery.setField(fieldNew);        // Liên kết ngược Field với SafeBoundery
                }
            }

            session.save(service);

            // Thêm các User vào Service
            for (int i = 0; i < members.length(); i++) {
                String memberId = members.getString(i);
                User user = userRepository.findUserByIdTelegram(memberId);
                if (user != null) {
                    service.addUser(user);  // Thêm user vào service
                    session.update(user);   // Cập nhật user
                } else {
                    log.error("User not found with id: " + memberId);
                }
            }

            transaction.commit();
            session.close();

            JSONObject jsonResponse = new JSONObject();
            jsonResponse.put("token", service.getToken());
            jsonResponse.put("created_at", service.getCreatedAt());

            response.body(jsonResponse.toString());

            return response.body();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback(); // Rollback nếu có lỗi
            }
            log.error("Error creating service", e);
            throw e;
        } finally {
            session.close();
        }
    }

    public static Object getAllServices(Request request, Response response) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = session.beginTransaction();

        // Sử dụng JOIN FETCH để lấy cả danh sách User trong từng Service
        List<Service> services = session.createQuery("SELECT DISTINCT s FROM Service s LEFT JOIN FETCH s.users", Service.class).list();

        transaction.commit();
        session.close();

        // Chuyển đổi kết quả thành JSON
        JSONArray serviceArray = new JSONArray();
        for (Service service : services) {
            JSONObject serviceJson = new JSONObject();
            serviceJson.put("id", service.getId());
            serviceJson.put("name", service.getName());
            serviceJson.put("category", service.getCategory().toString());
            serviceJson.put("token", service.getToken());
            serviceJson.put("owner", service.getOwner());
            serviceJson.put("createdAt", service.getCreatedAt());
            serviceJson.put("updatedAt", service.getUpdatedAt());

            // Lấy danh sách User của Service
            StringBuilder users = new StringBuilder();
            for (User user : service.getUsers()) {
                users.append(user.getIdTelegram()).append(" ");
            }

            serviceJson.put("users", users.toString().replace(" ", ", ").substring(0, users.length() - 1));

            serviceArray.put(serviceJson);
        }

        JSONObject jsonResponse = new JSONObject();
        jsonResponse.put("services", serviceArray);

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

    public static Object storeData(Request request, Response response) {
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


    public static void main(String[] args) throws InterruptedException {
        Spark.port(8080);
        Spark.post("/api/service/create", ServiceService::createService);
        Spark.get("/api/service/get-all", ServiceService::getAllServices);
        Spark.get("/api/service/get/:id", ServiceService::getServiceById);
        Spark.post("/store-data", ServiceService::storeData);
        Spark.get("/receive", ServiceService::receive);
    }
}
