package org.example.service;

import lombok.extern.slf4j.Slf4j;
import org.example.entity.*;
import org.example.enums.FieldType;
import org.example.enums.Operator;
import org.example.repository.SentMessageRepository;
import org.example.repository.ServiceRepository;
import org.example.repository.UserRepository;
import org.example.database.HibernateUtil;
import org.example.enums.Category;
import org.example.telegrambot.BotSingleton;
import org.example.telegrambot.NotificationBot;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.json.JSONArray;
import org.json.JSONObject;
import spark.Request;
import spark.Response;
import spark.Spark;

import java.util.*;

@Slf4j
public class ServiceService {
    private static UserRepository userRepository = new UserRepository();
    private static ServiceRepository serviceRepository = new ServiceRepository();
    private static CheckBounderyService checkBounderyService = new CheckBounderyService();
    private static NotificationBot bot = BotSingleton.getInstance();
    static Map<Long, List<SentMessage>> sentMessagesMap = new HashMap<>();

    public static Object createService(Request request, Response response) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = session.beginTransaction();
        JSONObject jsonResponse = new JSONObject();

        try {
            String body = request.body();
            JSONObject jsonBody = new JSONObject(body);
            System.out.println("Body: " + jsonBody.toString());
            String category = jsonBody.getString("category");
            String name = jsonBody.getString("name");
            String owner = jsonBody.getString("owner");
            JSONArray members = jsonBody.getJSONArray("members");
            JSONArray fields = jsonBody.getJSONArray("fields");
            JSONObject warning_duration = jsonBody.getJSONObject("warning_duration");
            Long hours = warning_duration.getLong("hours");
            Long minutes = warning_duration.getLong("minutes");
            Long warningDuration = hours * 60 + minutes;
            members.put(owner);
            Long createdAt = System.currentTimeMillis();
            Long updatedAt = System.currentTimeMillis();

            // Kiểm tra xem Service đã tồn tại chưa
            Service serviceCheck = serviceRepository.findServiceByName(name);

            if (serviceCheck != null) {
                jsonResponse.put("message", "Service already exists.");

                response.body(jsonResponse.toString());
                response.status(400);
                return response.body();
            }

            Service service = new Service(name, Category.valueOf(category), owner, createdAt, updatedAt, warningDuration);
            service.setToken(UUID.randomUUID().toString());

            for (Object field : fields) {
                JSONObject fieldJson = (JSONObject) field;
                String fieldName = fieldJson.getString("field_name");
                String fieldType = fieldJson.getString("field_type");
                boolean isMonitor = fieldJson.getBoolean("is_monitor");

                // Tạo Field mới và thêm vào Service
                Field fieldNew = new Field(fieldName, FieldType.valueOf(fieldType), isMonitor, service);
                service.addField(fieldNew);

                if (isMonitor) {
                    SafeBoundery safeBoundery = new SafeBoundery();

                    String operator = fieldJson.getString("operator");

                    safeBoundery.setOperator(Operator.valueOf(operator));

                    if (fieldType.equals("NUMBER")) {
                        safeBoundery.setValue1(fieldJson.isNull("value1") ? null : fieldJson.optDouble("value1"));
                        safeBoundery.setValue2(fieldJson.isNull("value2") ? null : fieldJson.optDouble("value2"));
                    } else if (fieldType.equals("STRING")) {
                        safeBoundery.setString(fieldJson.isNull("string") ? null : fieldJson.optString("string"));
                    }

                    fieldNew.setSafeBoundery(safeBoundery);
                    safeBoundery.setField(fieldNew);
                }
            }

            session.save(service);

            // Thêm các User vào Service
            for (int i = 0; i < members.length(); i++) {
                String memberId = userRepository.findUserByEmail(members.getString(i)).getIdTelegram();
                User user = userRepository.findUserByIdTelegram(memberId);
                if (user != null) {
                    service.addUser(user);
                    session.update(user);
                } else {
                    log.error("User not found with id: " + memberId);
                }
                bot.sendMessage("Bạn đã được thêm vào service: " + service.getToken(), memberId);
            }

            sentMessagesMap.put(service.getId(), new ArrayList<>());

            transaction.commit();

            jsonResponse.put("message", "Service created successfully.");
            JSONObject serviceJson = new JSONObject();
            serviceJson.put("name", service.getName());
            serviceJson.put("category", service.getCategory().toString());
            serviceJson.put("token", service.getToken());
            serviceJson.put("owner", service.getOwner());
            serviceJson.put("created_at", service.getCreatedAt());
            serviceJson.put("warning_duration", service.getWarningDuration());
            jsonResponse.put("service", serviceJson);

            response.body(jsonResponse.toString());
            response.status(200);

            return response.body();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            log.error("Error creating service", e);
            throw e;
        } finally {
            session.close();
        }
    }

    public static Object updateWarningDuration(Request request, Response response){
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = session.beginTransaction();
        JSONObject jsonResponse = new JSONObject();

        try {
            String body = request.body();
            JSONObject jsonBody = new JSONObject(body);
            Long id = jsonBody.getLong("id");
            Boolean warning_enable = jsonBody.getBoolean("warning_enable");
            JSONObject warning_duration = jsonBody.getJSONObject("warning_duration");
            Long hours = warning_duration.getLong("hours");
            Long minutes = warning_duration.getLong("minutes");
            Long warningDuration = hours * 60 + minutes;
            Service service = serviceRepository.findById(id);
            if (service == null) {
                jsonResponse.put("message", "Service not found.");

                response.body(jsonResponse.toString());
                response.status(404);

                return response.body();
            }

            if(!warning_enable){
                service.setWarningDuration(0L);
                session.update(service);
                transaction.commit();
                jsonResponse.put("message", "Disable warning duration successfully.");

                response.body(jsonResponse.toString());
                response.status(200);

                return response.body();
            }

            service.setWarningDuration(warningDuration);
            session.update(service);
            transaction.commit();

            jsonResponse.put("message", "Update warning duration successfully.");

            response.body(jsonResponse.toString());
            response.status(200);

            return response.body();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            log.error("Error updating warning duration", e);
            throw e;
        } finally {
            session.close();
        }
    }

    public static Object getAllServices(Request request, Response response) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = session.beginTransaction();
        JSONObject jsonResponse = new JSONObject();

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
            serviceJson.put("warningDuration", service.getWarningDuration());

            // Lấy danh sách User của Service
            StringBuilder users = new StringBuilder();
            for (User user : service.getUsers()) {
                users.append(user.getIdTelegram()).append(" ");
            }

            serviceJson.put("users", users.toString().replace(" ", ", ").substring(0, users.length() - 1));

            serviceArray.put(serviceJson);
        }


        jsonResponse.put("message", "List of services.");
        jsonResponse.put("services", serviceArray);

        response.body(jsonResponse.toString());
        response.status(200);

        return response.body();
    }

    public static Object getServiceById(Request request, Response response) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = session.beginTransaction();
        JSONObject jsonResponse = new JSONObject();

        Long id = Long.parseLong(request.params(":id"));
        Service service = session.get(Service.class, id);

        transaction.commit();
        session.close();

        if (service == null) {
            jsonResponse.put("message", "Service not found.");

            response.body(jsonResponse.toString());
            response.status(404);

            return response.body();
        }

        jsonResponse.put("message", "Service found.");
        jsonResponse.put("service", service);

        response.body(jsonResponse.toString());
        response.status(200);
        return response.body();
    }

    public static Object sendData(Request request, Response response) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = session.beginTransaction();
        JSONObject jsonResponse = new JSONObject();

        try {
            String body = request.body();
            JSONObject jsonBody = new JSONObject(body);
            String token = jsonBody.getString("token");
            JSONObject data = jsonBody.getJSONObject("data");

            Service service = serviceRepository.findServiceByToken(token);

            if (service == null) {
                jsonResponse.put("message", "Service not found.");

                response.body(jsonResponse.toString());
                response.status(404);

                return response.body();
            }

            DataReturn dataReturn = new DataReturn(data.toString(), System.currentTimeMillis(), service);

            service.addDataReturn(dataReturn);

            // Gửi thông báo nếu dữ liệu vượt quá ngưỡng

            for (Field field : service.getFields()) {
                String fieldValue = null;
                if (field.isMonitor()) {
                    if (field.getType() == FieldType.NUMBER) {
                        fieldValue = String.valueOf(data.getDouble(field.getName()));
                    } else if (field.getType() == FieldType.STRING) {
                        fieldValue = data.getString(field.getName());
                    }
                    SafeBoundery safeBoundery = field.getSafeBoundery();

                    if (!checkBounderyService.checkBoundery(safeBoundery, fieldValue)) {
                        String warningMessage = "Dữ liệu \"" + field.getName() + " = " + fieldValue + "\" nằm ngoài biên an toàn.";

                        SentWarning sentWarning = new SentWarning(warningMessage, System.currentTimeMillis(), service);
                        service.addSentWarning(sentWarning);
                        bot.sendMessage(warningMessage, userRepository.findUserByEmail(service.getOwner()).getIdTelegram());
                    }
                }
            }

            session.update(service);

            jsonResponse.put("message", "Data sent successfully.");
            jsonResponse.put("data", data);
            JSONArray latestMessages = new JSONArray();
            for (SentMessage sentMessage : sentMessagesMap.get(service.getId())) {
                JSONObject message = new JSONObject();
                message.put("message", sentMessage.getMessage());
                message.put("sentAt", sentMessage.getSentAt());
                latestMessages.put(message);
            }
            jsonResponse.put("latestMessages", latestMessages.isEmpty() ? "No message sent." : latestMessages);

            sentMessagesMap.put(service.getId(), new ArrayList<>());

            response.body(jsonResponse.toString());

            return response.body();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback(); // Rollback nếu có lỗi
            }
            log.error("Error sending data", e);
            throw e;
        } finally {
            session.close();
        }
    }

    public static Object sendMessage(Request request, Response response){
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = session.beginTransaction();
        JSONObject jsonResponse = new JSONObject();

        try {
            String body = request.body();
            JSONObject jsonBody = new JSONObject(body);
            Long id = jsonBody.getLong("id");
            String message = jsonBody.getString("message");

            Service service = serviceRepository.findById(id);

            if (service == null) {
                jsonResponse.put("message", "Service not found.");

                response.body(jsonResponse.toString());
                response.status(404);

                return response.body();
            }

            SentMessage sentMessage = new SentMessage(message, System.currentTimeMillis(), service);
            service.addSentMessage(sentMessage);
            session.update(service);

            sentMessagesMap.get(service.getId()).add(sentMessage);

            jsonResponse.put("message", "Message sent successfully.");
            jsonResponse.put("data", message);

            response.body(jsonResponse.toString());

            return response.body();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback(); // Rollback nếu có lỗi
            }
            log.error("Error sending message", e);
            throw e;
        } finally {
            session.close();
        }
    }

    public static void main(String[] args) {
        Spark.port(8080);
        Spark.post("/api/service/create", ServiceService::createService);
        Spark.get("/api/service/get-all", ServiceService::getAllServices);
        Spark.get("/api/service/get/:id", ServiceService::getServiceById);
        Spark.post("/api/service/update/toggle-warning", ServiceService::updateWarningDuration);
        Spark.post("/api/data/send", ServiceService::sendData);
        Spark.post("/api/message/send", ServiceService::sendMessage);
    }
}
