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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Slf4j
public class ServiceService {
    private static UserRepository userRepository = new UserRepository();
    private static ServiceRepository serviceRepository = new ServiceRepository();
    private static SentMessageRepository sentMessageRepository = new SentMessageRepository();
    private static DataReturnRepository dataReturnRepository = new DataReturnRepository();
    private static CheckBounderyService checkBounderyService = new CheckBounderyService();
    private static NotificationBot bot = BotSingleton.getInstance();
    private static ServiceManager serviceManager = new ServiceManager();

    /**
     * Add new service
     *
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
            String category = jsonBody.getString("category");
            String name = jsonBody.getString("name");
            String owner = jsonBody.getString("owner");
            User ownerObj = userRepository.findUserByEmail(owner);
            JSONArray members = jsonBody.getJSONArray("members");
            JSONArray fields = jsonBody.getJSONArray("fields");
            JSONObject warning_duration = jsonBody.getJSONObject("warning_duration");
            Long hours = warning_duration.getLong("hours");
            Long minutes = warning_duration.getLong("minutes");
            String message = warning_duration.getString("message");
            Long warningDuration = hours * 60 + minutes;
            members.put(owner);
            Long createdAt = System.currentTimeMillis();
            Long updatedAt = System.currentTimeMillis();

            // Kiểm tra xem Service đã tồn tại chưa
            Service serviceCheck = serviceRepository.findServiceByName(name);

            if (serviceCheck != null) {
                jsonResponse.put("message", "Service already exists.");
                jsonResponse.put("status", false);

                response.body(jsonResponse.toString());
                response.status(400);
                return response.body();
            }

            Service service = new Service(name, Category.valueOf(category), ownerObj.getId(), createdAt, updatedAt, warningDuration);
            service.setToken(UUID.randomUUID().toString());

            if (!fields.isEmpty()) {
                for (Object field : fields) {
                    JSONObject fieldJson = (JSONObject) field;
                    addField(service, fieldJson);
                }
            }

            session.save(service);

            // Thêm các User vào Service
            for (int i = 0; i < members.length(); i++) {
                String telegramId = userRepository.findUserByEmail(members.getString(i)).getIdTelegram();
                User user = userRepository.findUserByEmail(members.getString(i));
                if (user != null) {
                    service.addUser(user);
                    session.update(user);
                } else {
                    log.error("User not found with email: " + members.getString(i));
                }
                bot.sendMessage("Bạn đã được thêm vào service: " + service.getToken(), telegramId);
            }

            transaction.commit();

            if (warningDuration > 0) {
                serviceManager.addService(service);
            }

            jsonResponse.put("message", "Service created successfully.");
            jsonResponse.put("status", true);
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
     *
     * @param request
     * @param response
     * @return
     */
    public static Object deleteService(Request request, Response response) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = session.beginTransaction();
        JSONObject jsonResponse = new JSONObject();

        try {
            String body = request.body();
            JSONObject jsonBody = new JSONObject(body);
            String userEmail = jsonBody.getString("request_user");

            Long id = Long.valueOf(request.params(":id"));
            Service service = serviceRepository.findById(id);

            if (service == null) {
                return NotFoundObject.serviceNotFound(response).body();
            }

            if (!checkOwner(service, userEmail)) {
                return NotFoundObject.userNotOwner(response).body();
            }

            session.delete(service);
            transaction.commit();

            serviceManager.removeService(service.getId());

            jsonResponse.put("message", "Service deleted successfully.");
            jsonResponse.put("status", true);

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

    public static Object updateService(Request request, Response response){
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = session.beginTransaction();
        JSONObject jsonResponse = new JSONObject();

        try {
            String body = request.body();
            JSONObject jsonBody = new JSONObject(body);
            Long id = jsonBody.getLong("id");
            String userEmail = jsonBody.getString("request_user");
            String category = jsonBody.getString("category");
            String name = jsonBody.getString("name");
            Long updatedAt = System.currentTimeMillis();

            Service service = serviceRepository.findById(id);

            if (service == null) {
                return NotFoundObject.serviceNotFound(response).body();
            }

            if (!checkOwner(service, userEmail)) {
                return NotFoundObject.userNotOwner(response).body();
            }

            service.setCategory(Category.valueOf(category));
            service.setName(name);
            service.setUpdatedAt(updatedAt);


            session.update(service);

            transaction.commit();

            jsonResponse.put("message", "Service updated successfully.");
            jsonResponse.put("status", true);
            JSONObject serviceJson = new JSONObject()
                    .put("name", service.getName())
                    .put("category", service.getCategory().toString())
                    .put("token", service.getToken())
                    .put("owner", service.getOwner())
                    .put("created_at", service.getCreatedAt())
                    .put("warning_duration", service.getWarningDuration());

            jsonResponse.put("service", serviceJson);

            response.body(jsonResponse.toString());
            response.status(200);

            return response.body();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            log.error("Error updating service", e);
            throw e;
        } finally {
            session.close();
        }
    }

    /**
     * Turn on/off warning duration
     *
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
            String userEmail = jsonBody.getString("request_user");
            Long id = jsonBody.getLong("id");
            Boolean warning_enable = jsonBody.getBoolean("warning_enable");
            JSONObject warning_duration = jsonBody.getJSONObject("warning_duration");
            Long hours = warning_duration.getLong("hours");
            Long minutes = warning_duration.getLong("minutes");
            Long warningDuration = hours * 60 + minutes;
            Service service = serviceRepository.findById(id);
            if (service == null) {
                return NotFoundObject.serviceNotFound(response).body();
            }

            if (!checkOwner(service, userEmail)) {
                return NotFoundObject.userNotOwner(response).body();
            }

            if (!warning_enable || warningDuration == 0) {
                // Vô hiệu hóa cảnh báo nếu warning_enable là false hoặc thời gian cảnh báo là 0
                service.setWarningDuration(0L);
                session.update(service);
                transaction.commit();
                jsonResponse.put("message", "Warning duration disabled successfully.");
                jsonResponse.put("status", true);

                serviceManager.removeService(service.getId()); // Dừng service nếu cần

                response.body(jsonResponse.toString());
                response.status(200);
                return response.body();
            }

            // Nếu warning_duration > 0 và warning_enable là true
            service.setWarningDuration(warningDuration);
            session.update(service);
            transaction.commit();

            if (serviceManager.isRunning(service.getId())) {
                // Nếu service đang chạy, chỉ cần cập nhật thời gian mà không cần khởi động lại
                serviceManager.restartService(service.getId());
            } else {
                serviceManager.addService(service); // Nếu service chưa chạy, khởi động nó
            }

            jsonResponse.put("message", "Update warning duration successfully.");
            jsonResponse.put("status", true);

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
     *
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
            JSONArray members = new JSONArray();
            for (User user : service.getUsers()) {
                Set<Email> emails = user.getEmails();
                for (Email email : emails) {
                    members.put(email.getEmailAddress());
                }
            }

            serviceJson.put("members", members);

            JSONArray fields = new JSONArray();
            for (Field field : service.getFields()) {
                JSONObject fieldJson = new JSONObject();
                fieldJson.put("name", field.getName());
                fieldJson.put("type", field.getType().toString());
                fieldJson.put("is_monitor", field.isMonitor());
                if (field.isMonitor()) {
                    JSONObject safeBoundery = new JSONObject();
                    safeBoundery.put("operator", field.getSafeBoundery().getOperator().toString());
                    safeBoundery.put("value1", field.getSafeBoundery().getValue1());
                    safeBoundery.put("value2", field.getSafeBoundery().getValue2());
                    safeBoundery.put("string", field.getSafeBoundery().getString());
                    fieldJson.put("safe_boundery", safeBoundery);
                }
                fields.put(fieldJson);
            }

            Long warningDuration = service.getWarningDuration();

            JSONObject warningDurationJson = new JSONObject();
            warningDurationJson.put("hours", warningDuration / 60);
            warningDurationJson.put("minutes", warningDuration % 60);
            warningDurationJson.put("message", "Warning message");

            serviceJson.put("warning_duration", warningDurationJson);
            serviceJson.put("fields", fields);

            DataReturn dataReturn = dataReturnRepository.getLatestDataReturn(service.getId());
            if (dataReturn == null) {
                serviceJson.put("last_sent", "No data sent.");
                serviceArray.put(serviceJson);
                continue;
            }

            Long lastSent = System.currentTimeMillis() - dataReturnRepository.getLatestDataReturn(service.getId()).getCreatedAt();
            // Convert to hours and minutes
            Long hours = lastSent / 3600000;
            Long minutes = (lastSent % 3600000) / 60000;
            serviceJson.put("last_sent", hours + " hours " + minutes + " minutes");

            serviceArray.put(serviceJson);
        }

        jsonResponse.put("message", "List of services.");
        jsonResponse.put("status", true);
        jsonResponse.put("services", serviceArray);

        response.body(jsonResponse.toString());
        response.status(200);

        return response.body();
    }

    /**
     * Get service by id
     *
     * @param request
     * @param response
     * @return
     */
    public static Object getServiceById(Request request, Response response) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = session.beginTransaction();
        JSONObject jsonResponse = new JSONObject();

        String body = request.body();
        JSONObject jsonBody = new JSONObject(body);
        String userEmail = jsonBody.getString("request_user");

        Long id = Long.parseLong(request.params(":id"));
        Service service = session.get(Service.class, id);

        transaction.commit();
        session.close();

        if (service == null) {
            return NotFoundObject.serviceNotFound(response).body();
        }


        if (!checkMember(service, userEmail)) {
            return NotFoundObject.userNotMember(response).body();
        }

        JSONObject serviceJson = service.toJson();
        JSONArray members = new JSONArray();
        for (User user : service.getUsers()) {
            members.put(user.getIdTelegram());
        }
        serviceJson.put("members", members);

        jsonResponse.put("message", "Service found.");
        jsonResponse.put("status", true);
        jsonResponse.put("service", serviceJson);

        response.body(jsonResponse.toString());
        response.status(200);
        return response.body();
    }

    /**
     * Get service by name
     *
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
            jsonResponse.put("status", false);

            return jsonResponse.toString();
        }


        jsonResponse.put("message", "Service found.");
        jsonResponse.put("status", true);
        jsonResponse.put("service", service.toJson());

        return jsonResponse.toString();
    }

    /**
     * Add new field to an existing service
     *
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
            String userEmail = jsonBody.getString("request_user");
            JSONObject field = jsonBody.getJSONObject("field");

            Service service = serviceRepository.findById(id);

            if (service == null) {
                return NotFoundObject.serviceNotFound(response).body();
            }

            if (!checkOwner(service, userEmail)) {
                return NotFoundObject.userNotOwner(response).body();
            }

            addField(service, field);

            session.update(service);

            transaction.commit();

            jsonResponse.put("message", "Field added successfully.");
            jsonResponse.put("status", true);

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

    public static Object addMember(Request request, Response response) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = session.beginTransaction();
        JSONObject jsonResponse = new JSONObject();

        try {
            String body = request.body();
            JSONObject jsonBody = new JSONObject(body);
            Long id = jsonBody.getLong("id");
            String userEmail = jsonBody.getString("request_user");
            String member = jsonBody.getString("member");

            Service service = serviceRepository.findById(id);

            if (service == null) {
                return NotFoundObject.serviceNotFound(response).body();
            }

            if (!checkOwner(service, userEmail)) {
                return NotFoundObject.userNotOwner(response).body();
            }

            User user = userRepository.findUserByEmail(member);
            if (user == null) {
                return NotFoundObject.userNotFound(response).body();
            }

            service.addUser(user);
            session.update(user);

            transaction.commit();

            bot.sendMessage("Bạn đã được thêm vào service: " + service.getToken(), user.getIdTelegram());

            jsonResponse.put("message", "Member added successfully.");
            jsonResponse.put("status", true);

            response.body(jsonResponse.toString());
            response.status(200);

            return response.body();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            log.error("Error adding member", e);
            throw e;
        } finally {
            session.close();
        }
    }

    /**
     * Service sends data to API
     *
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
                return NotFoundObject.serviceNotFound(response).body();
            }

            DataReturn dataReturn = new DataReturn(data.toString(), System.currentTimeMillis(), service);

            service.addDataReturn(dataReturn);

            // Gửi thông báo nếu dữ liệu vượt quá ngưỡng

            for (Field field : service.getFields()) {
                String fieldValue = null;
                if (field.isMonitor()) {
                    if (!data.has(field.getName())) {
                        System.out.println("Field " + field.getName() + " not found in data");
                        continue;
                    }
                    if (field.getType() == FieldType.NUMBER) {
                        fieldValue = String.valueOf(data.getDouble(field.getName()));
                    } else if (field.getType() == FieldType.STRING) {
                        fieldValue = data.getString(field.getName());
                    }
                    SafeBoundery safeBoundery = field.getSafeBoundery();

                    if (checkBounderyService.checkBoundery(safeBoundery, fieldValue)) {
                        String warningMessage = "Dữ liệu \"" + field.getName() + " = " + fieldValue + "\" của service \"" + service.getName() + "\" nằm ngoài biên an toàn.";

                        SentWarning sentWarning = new SentWarning(warningMessage, System.currentTimeMillis(), service);
                        service.addSentWarning(sentWarning);
                        Set<User> members = service.getUsers();
                        for (User member : members) {
                            bot.sendMessage(warningMessage, member.getIdTelegram());
                        }
                    }
                }
            }

            session.update(service);

            jsonResponse.put("message", "Data sent successfully.");
            jsonResponse.put("status", true);
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
     *
     * @param request
     * @param response
     * @return
     */
    public static Object getLatestData(Request request, Response response) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = session.beginTransaction();
        JSONObject jsonResponse = new JSONObject();

        try {
            String body = request.body();
            JSONObject jsonBody = new JSONObject(body);
            String userEmail = jsonBody.getString("request_user");

            Long id = Long.valueOf(request.params(":id"));
            Service service = serviceRepository.findById(id);

            if (service == null) {
                return NotFoundObject.serviceNotFound(response).body();
            }

            if (!checkMember(service, userEmail)) {
                return NotFoundObject.userNotMember(response).body();
            }

            DataReturn dataReturn = dataReturnRepository.getLatestDataReturn(id);

            if (dataReturn == null) {
                jsonResponse.put("message", "No data found.");
                jsonResponse.put("status", false);

                response.body(jsonResponse.toString());
                response.status(404);

                return response.body();
            }

            jsonResponse.put("message", "Latest data.");
            jsonResponse.put("status", true);
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
     *
     * @param request
     * @param response
     * @return
     */
    public static Object getHistoryData(Request request, Response response) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = session.beginTransaction();
        JSONObject jsonResponse = new JSONObject();

        try {
            String body = request.body();
            JSONObject jsonBody = new JSONObject(body);
            String userEmail = jsonBody.getString("request_user");

            Long id = Long.valueOf(request.params(":id"));
            Service service = serviceRepository.findById(id);

            if (service == null) {
                return NotFoundObject.serviceNotFound(response).body();
            }

            if (!checkMember(service, userEmail)) {
                return NotFoundObject.userNotMember(response).body();
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
            jsonResponse.put("status", true);
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
     *
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
            String userEmail = jsonBody.getString("request_user");
            String message = jsonBody.getString("message");

            Service service = serviceRepository.findById(id);

            if (service == null) {
                return NotFoundObject.serviceNotFound(response).body();
            }

            if (!checkMember(service, userEmail)) {
                return NotFoundObject.userNotMember(response).body();
            }

            SentMessage sentMessage = new SentMessage(message, System.currentTimeMillis(), service);
            service.addSentMessage(sentMessage);
            session.update(service);

            jsonResponse.put("message", "Message sent successfully.");
            jsonResponse.put("status", true);
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

    public static void addField(Service service, JSONObject field) {
        Field fieldNew = new Field(field.getString("field_name"), FieldType.valueOf(field.getString("field_type")), field.getBoolean("is_monitor"), service);
        service.addField(fieldNew);

        if (field.getBoolean("is_monitor")) {
            SafeBoundery safeBoundery = new SafeBoundery();

            safeBoundery.setOperator(Operator.valueOf(field.getString("operator")));

            if (field.getString("field_type").equals("NUMBER")) {
                safeBoundery.setValue1(field.isNull("value1") ? null : field.optDouble("value1"));
                safeBoundery.setValue2(field.isNull("value2") ? null : field.optDouble("value2"));
            } else if (field.getString("field_type").equals("STRING")) {
                safeBoundery.setString(field.isNull("string") ? null : field.optString("string"));
            }

            fieldNew.setSafeBoundery(safeBoundery);
            safeBoundery.setField(fieldNew);
        }
    }

    public static boolean checkOwner(Service service, String userEmail) {
        User user = userRepository.findUserByEmail(userEmail);
        if (user == null) {
            return false;
        }
        return service.getOwner() == user.getId();
    }

    public static boolean checkMember(Service service, String userEmail) {
        Set<Long> listMembers = new HashSet<>();
        for (User user : service.getUsers()) {
            listMembers.add(user.getId());
        }
        User user = userRepository.findUserByEmail(userEmail);
        if (user == null) {
            return false;
        }
        return listMembers.contains(user.getId());
    }

    public static void main(String[] args) {
        Spark.port(8080);
        Spark.post("/api/service/create", ServiceService::createService);
        Spark.get("/api/service/get/all", ServiceService::getAllServices);
        Spark.get("/api/service/get/:id", ServiceService::getServiceById);
        Spark.get("/api/service/get/name/:name", ServiceService::getServiceByName);
        Spark.post("/api/service/add/field", ServiceService::addField);
        Spark.post("/api/service/add/member", ServiceService::addMember);
        Spark.post("/api/service/update/toggle-warning", ServiceService::updateWarningDuration);
        Spark.post("/api/service/update", ServiceService::updateService);
        Spark.delete("/api/service/delete/:id", ServiceService::deleteService);
        Spark.post("/api/data/send", ServiceService::sendData);
        Spark.get("/api/data/get/:id", ServiceService::getLatestData);
        Spark.get("/api/data/history/:id", ServiceService::getHistoryData);
        Spark.post("/api/message/send", ServiceService::sendMessage);

        // Start all service
//        List<Service> services = serviceRepository.getAllServices();
//        for (Service service : services) {
//            if (service.getWarningDuration() > 0) {
//                serviceManager.addService(service);
//            }
//        }
    }
}
