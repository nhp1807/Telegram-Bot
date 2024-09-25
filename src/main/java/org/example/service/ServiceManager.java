package org.example.service;

import org.example.entity.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServiceManager {

    private final Map<Long, Service> services = new HashMap<>();
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    public void addService(Service service) {
        if (service != null && !services.containsKey(service.getId())) {
            services.put(service.getId(), service);
            executorService.submit(service);
            System.out.println("Đã khởi động Service: " + service.getName());
        } else {
            System.out.println("Service đã tồn tại hoặc null.");
        }
    }

    public void removeService(Long serviceId) {
        Service service = services.get(serviceId);
        if (service != null) {
            service.stopTask();
            services.remove(serviceId);
            System.out.println("Đã dừng Service: " + service.getName());
        } else {
            System.out.println("Không tìm thấy Service với ID: " + serviceId);
        }
    }

    public void restartService(Long serviceId) {
        removeService(serviceId);
        Service service = services.get(serviceId);
        if (service != null) {
            addService(service);
            System.out.println("Đã khởi động lại Service: " + service.getName());
        }
    }

    public boolean isRunning(Long serviceId) {
        return services.containsKey(serviceId);
    }

    public void shutdown() {
        for (Service service : services.values()) {
            service.stopTask();
            System.out.println("Đã dừng Service: " + service.getName());
        }
        executorService.shutdown();
        System.out.println("Đã tắt ServiceManager.");
    }

    public Map<Long, Service> getServices() {
        return new HashMap<>(services); // Trả về một bản sao để đảm bảo an toàn
    }
}

