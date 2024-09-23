package org.example.service;

import lombok.extern.slf4j.Slf4j;
import org.example.entity.*;
import org.example.enums.FieldType;
import org.example.enums.Operator;
import org.example.repository.DataReturnRepository;
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
    private static SentMessageRepository sentMessageRepository = new SentMessageRepository();
    private static DataReturnRepository dataReturnRepository = new DataReturnRepository();
    private static CheckBounderyService checkBounderyService = new CheckBounderyService();
    private static NotificationBot bot = BotSingleton.getInstance();

    /**
     * Add new service
     * @param request
     * @param response
     * @return
     */
    public static Object createService(Request request, Response response) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = session.beginTransaction();
        JSONObject jsonResponse = new JSONObject();

        try {
            String body = request.body();
            JSONObject jsonBody = new JSONObject(body);
            System.out.println("Body: " + jsonBody);
            String category = jsonBody.getString("category");
            String name = jsonBody.getString("name");
            String owner = jsonBody.getString("owner");
            String memberString = jsonBody.getString("members");
            JSONArray members;
            if (memberString.isEmpty()) {
                members = new JSONArray();
            } else if (!memberString.contains(",")) {
                members = new JSONArray();
                members.put(memberString);
            } else {
                members = new JSONArray(memberString.split(","));
            }
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

    /**
     * Delete service
     * @param request
     * @param response
     * @return
     */
    public static Object deleteService(Request request, Response response) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = session.beginTransaction();
        JSONObject jsonResponse = new JSONObject();

        try {
            Long id = Long.valueOf(request.params(":id"));
            Service service = serviceRepository.findById(id);

            if (service == null) {
                jsonResponse.put("message", "Service not found.");

                response.body(jsonResponse.toString());
                response.status(404);

                return response.body();
            }

            session.delete(service);
            transaction.commit();

            jsonResponse.put("message", "Service deleted successfully.");

            response.body(jsonResponse.toString());
            response.status(200);

            return response.body();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            log.error("Error removing service", e);
            throw e;
        } finally {
            session.close();
        }
    }

    /**
     * Get general service information which showed on dashboard
     * @param request
     * @param response
     * @return
     */
    public static Object showInfoDashboard(Request request, Response response) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = session.beginTransaction();
        JSONObject jsonResponse = new JSONObject();

        try {
            List<Service> services = serviceRepository.getAllServices();

            JSONArray serviceArray = new JSONArray();
            for (Service service : services) {
                JSONObject serviceJson = new JSONObject();
                serviceJson.put("name", service.getName());
                serviceJson.put("category", service.getCategory().toString());
                serviceJson.put("owner", service.getOwner());
                Long lastSent = System.currentTimeMillis() - dataReturnRepository.getLatestDataReturn(service.getId()).getCreatedAt();
                // COnvert to hours and minutes
                Long hours = lastSent / 3600000;
                Long minutes = (lastSent % 3600000) / 60000;
                serviceJson.put("last_sent", hours + " hours " + minutes + " minutes");
                serviceArray.put(serviceJson);
            }

            jsonResponse.put("message", "List of services.");
            jsonResponse.put("services", serviceArray);

            response.body(jsonResponse.toString());
            response.status(200);

            return response.body();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            log.error("Error show info dashboard", e);
            throw e;
        } finally {
            session.close();
        }
    }

    /**
     * Turn on/off warning duration
     * @param request
     * @param response
     * @return
     */
    public static Object updateWarningDuration(Request request, Response response) {
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

            if (!warning_enable) {
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

    /**
     * Get all services
     * @param request
     * @param response
     * @return
     */
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
            JSONObject serviceJson = service.toJson();

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

    /**
     * Get service by id
     * @param request
     * @param response
     * @return
     */
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
        jsonResponse.put("service", service.toJson());

        response.body(jsonResponse.toString());
        response.status(200);
        return response.body();
    }

    /**
     * Get service by name
     * @param request
     * @param response
     * @return
     */
    public static Object getServiceByName(Request request, Response response) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = session.beginTransaction();
        JSONObject jsonResponse = new JSONObject();

        String name = request.params(":name");
        Service service = serviceRepository.findServiceByName(name);

        transaction.commit();
        session.close();

        if (service == null) {
            jsonResponse.put("message", "Service not found.");

            return jsonResponse.toString();
        }

        jsonResponse.put("message", "Service found.");
        jsonResponse.put("service", service.toJson());

        return jsonResponse.toString();
    }

    /**
     * Add new field to an existing service
     * @param request
     * @param response
     * @return
     */
    public static Object addField(Request request, Response response) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = session.beginTransaction();
        JSONObject jsonResponse = new JSONObject();

        try {
            String body = request.body();
            JSONObject jsonBody = new JSONObject(body);
            Long id = jsonBody.getLong("id");
            JSONObject field = jsonBody.getJSONObject("field");

            Service service = serviceRepository.findById(id);

            if (service == null) {
                jsonResponse.put("message", "Service not found.");

                response.body(jsonResponse.toString());
                response.status(404);

                return response.body();
            }

            String fieldName = field.getString("field_name");
            String fieldType = field.getString("field_type");
            boolean isMonitor = field.getBoolean("is_monitor");

            // Tạo Field mới và thêm vào Service
            Field fieldNew = new Field(fieldName, FieldType.valueOf(fieldType), isMonitor, service);
            service.addField(fieldNew);

            if (isMonitor) {
                SafeBoundery safeBoundery = new SafeBoundery();

                String operator = field.getString("operator");

                safeBoundery.setOperator(Operator.valueOf(operator));

                if (fieldType.equals("NUMBER")) {
                    safeBoundery.setValue1(field.isNull("value1") ? null : field.optDouble("value1"));
                    safeBoundery.setValue2(field.isNull("value2") ? null : field.optDouble("value2"));
                } else if (fieldType.equals("STRING")) {
                    safeBoundery.setString(field.isNull("string") ? null : field.optString("string"));
                }

                fieldNew.setSafeBoundery(safeBoundery);
                safeBoundery.setField(fieldNew);
            }

            session.update(service);
            transaction.commit();

            jsonResponse.put("message", "Field added successfully.");

            response.body(jsonResponse.toString());
            response.status(200);

            return response.body();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            log.error("Error adding field", e);
            throw e;
        } finally {
            session.close();
        }
    }

    /**
     * Service sends data to API
     * @param request
     * @param response
     * @return
     */
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

            List<SentMessage> latestMessages = sentMessageRepository.getLatestSentMessage(service.getId());
            JSONArray latestMessagesArray = new JSONArray();
            for (SentMessage sentMessage : latestMessages) {
                JSONObject message = new JSONObject();
                message.put("message", sentMessage.getMessage());
                message.put("sentAt", sentMessage.getSentAt());
                latestMessagesArray.put(message);
            }
            jsonResponse.put("latestMessages", latestMessagesArray.isEmpty() ? "No message sent." : latestMessagesArray);

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

    /**
     * Get latest data from service
     * @param request
     * @param response
     * @return
     */
    public static Object getLatestData(Request request, Response response) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = session.beginTransaction();
        JSONObject jsonResponse = new JSONObject();

        try {
            Long id = Long.valueOf(request.params(":id"));
            Service service = serviceRepository.findById(id);

            if (service == null) {
                jsonResponse.put("message", "Service not found.");

                response.body(jsonResponse.toString());
                response.status(404);

                return response.body();
            }

            DataReturn dataReturn = dataReturnRepository.getLatestDataReturn(id);

            if (dataReturn == null) {
                jsonResponse.put("message", "No data found.");

                response.body(jsonResponse.toString());
                response.status(404);

                return response.body();
            }

            jsonResponse.put("message", "Latest data.");
            jsonResponse.put("data", new JSONObject(dataReturn.getData()));

            response.body(jsonResponse.toString());
            response.status(200);

            return response.body();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback(); // Rollback nếu có lỗi
            }
            log.error("Error getting latest data", e);
            throw e;
        } finally {
            session.close();
        }
    }

    /**
     * Get history data from service
     * @param request
     * @param response
     * @return
     */
    public static Object getHistoryData(Request request, Response response) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = session.beginTransaction();
        JSONObject jsonResponse = new JSONObject();

        try {
            Long id = Long.valueOf(request.params(":id"));
            Service service = serviceRepository.findById(id);

            if (service == null) {
                jsonResponse.put("message", "Service not found.");

                response.body(jsonResponse.toString());
                response.status(404);

                return response.body();
            }

            Set<DataReturn> dataReturns = service.getDataReturns();
            JSONArray dataReturnsArray = new JSONArray();
            for (DataReturn dataReturn : dataReturns) {
                JSONObject dataReturnJson = new JSONObject();
                dataReturnJson.put("data", new JSONObject(dataReturn.getData()));
                dataReturnJson.put("create_at", dataReturn.getCreatedAt());
                dataReturnsArray.put(dataReturnJson);
            }

            jsonResponse.put("message", "List of data returns.");
            jsonResponse.put("dataReturns", dataReturnsArray);

            response.body(jsonResponse.toString());
            response.status(200);

            return response.body();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback(); // Rollback nếu có lỗi
            }
            log.error("Error getting history data", e);
            throw e;
        } finally {
            session.close();
        }
    }

    /**
     * Send message to service by user
     * @param request
     * @param response
     * @return
     */
    public static Object sendMessage(Request request, Response response) {
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
        Spark.get("/api/service/get/all", ServiceService::getAllServices);
        Spark.get("/api/service/dashboard/get/all", ServiceService::showInfoDashboard);
        Spark.get("/api/service/get/:id", ServiceService::getServiceById);
        Spark.get("/api/service/get/name/:name", ServiceService::getServiceByName);
        Spark.post("/api/service/add/field", ServiceService::addField);
        Spark.post("/api/service/update/toggle-warning", ServiceService::updateWarningDuration);
        Spark.delete("/api/service/delete/:id", ServiceService::deleteService);
        Spark.post("/api/data/send", ServiceService::sendData);
        Spark.get("/api/data/get/:id", ServiceService::getLatestData);
        Spark.get("/api/data/history/:id", ServiceService::getHistoryData);
        Spark.post("/api/message/send", ServiceService::sendMessage);
    }
}
