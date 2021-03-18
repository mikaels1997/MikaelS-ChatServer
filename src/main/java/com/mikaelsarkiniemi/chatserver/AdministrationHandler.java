package com.mikaelsarkiniemi.chatserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import com.sun.net.httpserver.HttpHandler;

import org.json.JSONException;
import org.json.JSONObject;

import com.sun.net.httpserver.HttpExchange;

public class AdministrationHandler extends ContextHandler implements HttpHandler {

    AdminAuthenticator auth = null; 

    public AdministrationHandler(AdminAuthenticator authenticator) {
        this.auth = authenticator;
    }

    public void handle(HttpExchange exchange) throws UnsupportedEncodingException {
        if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            // Handle POST request
            handlePOST(exchange);
        } else {
            // Request type not supported
            String errorMsg = "Error 400: this type of request is not supported";
            sendErrorMsg(errorMsg, exchange, 400);
        }
    }

    // The potential admin requests user removal or user detail edit
    private void handlePOST(HttpExchange exchange) {
        
        try {
            // Checking if "Content-Type" header exists and is supported
            if (checkContentType(exchange)) {

                // Receiving the user info
                InputStream reqBody = exchange.getRequestBody();
                InputStreamReader reader = new InputStreamReader(reqBody, StandardCharsets.UTF_8);
                String userinfo = new BufferedReader(reader).lines().collect(Collectors.joining("\n"));
                JSONObject admInfo = new JSONObject(userinfo);

                // Checking the choice of action
                String action = admInfo.getString("action").strip();

                if (action.strip().equals("edit")) {
                 
                    // Parsing the edited info of the user
                    String [] infoArray = new String[4];
                    JSONObject editedUserDetails = admInfo.getJSONObject("userdetails");
                    infoArray[0] = editedUserDetails.getString("username");
                    infoArray[1] = editedUserDetails.getString("password");
                    infoArray[2] = editedUserDetails.getString("role");
                    infoArray[3] = editedUserDetails.getString("email");

                    if (auth.updateUserInfo(infoArray)) {
                        // User info updated successfully
                        System.out.println("The user info of <"+infoArray[0]+"> has been updated");
                        exchange.sendResponseHeaders(200, -1);
                    } else {
                        String errorMsg = "Invalid request";
                        sendErrorMsg(errorMsg, exchange, 403);
                    }
                } 
                else if (action.strip().equals("remove")){
                    String target = admInfo.getString("target");
                    if(auth.removeUser(target)){
                        // User removed successfully
                        System.out.println("The user <"+target+"> removed successfully");
                        exchange.sendResponseHeaders(200, -1);
                    } else {
                        String errorMsg = "Invalid request";
                        sendErrorMsg(errorMsg, exchange, 403);
                    }
                } 
                else {
                    String errorMsg = "Unsupported command for /administration";
                    sendErrorMsg(errorMsg, exchange, 403);
                }
            }
        } catch(IOException ioe) {
            System.out.println("IOException in /administration");
            String errorMsg = "Internal server error";
            sendErrorMsg(errorMsg, exchange, 500);
        } catch(JSONException jse) {
            String errorMsg = "Invalid command syntax for /administration";
            sendErrorMsg(errorMsg, exchange, 403);
        }
    }
}
