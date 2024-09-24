package org.example.test;

import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import com.sun.management.OperatingSystemMXBean;
import org.json.JSONObject;

public class ResourceUsageMonitor {
    public static void main(String[] args) {
        // Lấy đối tượng OperatingSystemMXBean
        OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

        // Create http post to post data to api

        while(true) {
            try {
                sendPostRequest("http://localhost:8080/api/data/send", displayResourceUsage(osBean).toString());
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            displayResourceUsage(osBean);
        }
    }

    public static JSONObject displayResourceUsage(OperatingSystemMXBean osBean) {
        // Hiển thị dung lượng bộ nhớ đã sử dụng
        long totalMemorySize = osBean.getTotalPhysicalMemorySize();
        long freeMemorySize = osBean.getFreePhysicalMemorySize();
        long usedMemorySize = totalMemorySize - freeMemorySize;
        double cpuLoad = osBean.getProcessCpuLoad() * 100;

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("token", "95473ec6-05aa-43c2-b12f-ecd89fa9560a");
        JSONObject jsonData = new JSONObject();
        jsonData.put("memory", usedMemorySize);
        jsonData.put("cpu", cpuLoad);
        jsonObject.put("data", jsonData);

        return jsonObject;
    }

    public static void sendPostRequest(String urlString, String jsonInputString) {
        try {
            // Tạo URL đối tượng
            URL url = new URL(urlString);

            // Mở kết nối với URL
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            // Thiết lập phương thức HTTP là POST
            conn.setRequestMethod("POST");

            // Thiết lập headers (nếu cần)
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setRequestProperty("Accept", "application/json");

            // Cho phép ghi dữ liệu vào body của request
            conn.setDoOutput(true);

            // Ghi dữ liệu vào request body
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // Kiểm tra mã phản hồi từ server
            int responseCode = conn.getResponseCode();
            System.out.println("Response Code: " + responseCode);

            // Đóng kết nối
            conn.disconnect();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

