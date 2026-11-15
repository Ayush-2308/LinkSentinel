package com.linksentinel.web;

import java.io.IOException;

import jakarta.servlet.http.HttpServletResponse;

public final class JsonErrors {

    private JsonErrors() {
    }

    public static void write(HttpServletResponse response, int status, String error, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"error\":\"" + error + "\",\"message\":\"" + message + "\",\"fields\":{}}");
    }
}
