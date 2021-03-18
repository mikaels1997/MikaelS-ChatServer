package com.mikaelsarkiniemi.chatserver;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

// Shared methods for registration, chat and administration
public abstract class ContextHandler {

    // Sends a specific error message to the user
    protected void sendErrorMsg(String msg, HttpExchange exchange, int rCode) {
        byte[] msgBytes = msg.getBytes(StandardCharsets.UTF_8);
        try {
            exchange.sendResponseHeaders(rCode, msgBytes.length);
            OutputStream resBody = exchange.getResponseBody();
            resBody.write(msgBytes);
            resBody.close();
            System.out.println("Responding to the user with error code " + rCode +
             " Message to user: "+ msg);
        } catch (IOException ioe) {
            System.out.println("An error while sending an errormessage has occurred");
        }
    }

    // Returns true if Content-Type header exists and is supported
    protected boolean checkContentType(HttpExchange exchange) {
        Headers reqHeaders = exchange.getRequestHeaders();
        String type = reqHeaders.getFirst("Content-Type");
        return type.equalsIgnoreCase("application/json");
    }
}
