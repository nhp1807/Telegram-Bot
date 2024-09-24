package org.example.entity;

import org.json.JSONObject;
import spark.Response;

public class NotFoundObject {
    public static Response serviceNotFound(Response response) {
        JSONObject jsonResponse = new JSONObject();
        jsonResponse.put("message", "Service not found.");
        jsonResponse.put("status", false);

        response.body(jsonResponse.toString());
        response.status(404);

        return response;
    }

    public static Response userNotMember(Response response) {
        JSONObject jsonResponse = new JSONObject();
        jsonResponse.put("message", "User is not a member of this service.");
        jsonResponse.put("status", false);

        response.body(jsonResponse.toString());
        response.status(403);

        return response;
    }

    public static Response userNotOwner(Response response) {
        JSONObject jsonResponse = new JSONObject();
        jsonResponse.put("message", "You are not the owner of this service.");
        jsonResponse.put("status", false);

        response.body(jsonResponse.toString());
        response.status(403);

        return response;
    }

    public static Response userNotFound(Response response) {
        JSONObject jsonResponse = new JSONObject();
        jsonResponse.put("message", "User not found.");
        jsonResponse.put("status", false);

        response.body(jsonResponse.toString());
        response.status(404);

        return response;
    }
}
