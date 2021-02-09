package com.mikaelsarkiniemi.chatserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import com.sun.net.httpserver.HttpHandler;

import org.json.JSONException;
import org.json.JSONObject;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

public class RegistrationHandler implements HttpHandler {

    ChatAuthenticator auth = null;

    public RegistrationHandler(ChatAuthenticator authenticator) {
        this.auth = authenticator;
    }

    public void handle(HttpExchange exchange) throws UnsupportedEncodingException {   
        if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            // Handle POST request
            handlePOST(exchange);
        } else {
            // User tries to request with other method that POST
            String errorMsg = "Error 400: this type of request is not supported";
            sendErrorMsg(errorMsg, exchange, 400);
        }
    }

    private void handlePOST(HttpExchange exchange){
        try{

            //Checking if "Content-Type" header exists and is supported
            if (checkContentType(exchange)){

                //Receiving the user info
                InputStream reqBody = exchange.getRequestBody();
                InputStreamReader reader = new InputStreamReader(reqBody, 
                StandardCharsets.UTF_8);
                String userinfo = new BufferedReader(reader).lines()
                .collect(Collectors.joining("\n"));

                // Formatting the info into JSON for storage
                try{
                    JSONObject regInfo = new JSONObject(userinfo);
                    String username = regInfo.getString("username");
                    String passwd = regInfo.getString("password");
                    String email = regInfo.getString("email");
                    User user = new User(username, email, passwd);
                    if (username.strip().isEmpty()||passwd.strip().isEmpty()||email.strip().isEmpty()){
                        // User info contains empty strings
                        throw new JSONException("");
                    }
                    if (auth.addUser(username, user)){
                        // Everything OK, registering the user
                        exchange.sendResponseHeaders(200, -1);
                        System.out.println("User: \""+username+"\" registered successfully");
                    } else{
                        String errorMsg = "Error 403: User already registered";
                        sendErrorMsg(errorMsg, exchange, 403);
                    }
                } catch (JSONException je){
                    //The info wasn't in proper JSON format
                    String errorMsg = "Error 403: Unallowed string";
                    sendErrorMsg(errorMsg, exchange, 403);
                }

                reqBody.close();
                reader.close();
                
            } else{
                String msg = "Error 400: Content-Type header needs to exist and be supported";
                sendErrorMsg(msg, exchange, 400);
            }
        } catch (IOException ioe){
            System.out.println("Sending response for POST request failed");
            String error = "Internal server error";
            sendErrorMsg(error, exchange, 500);
        }
    }
    
    private void sendErrorMsg(String msg, HttpExchange exchange, int rCode){
        byte[] msgBytes = msg.getBytes(StandardCharsets.UTF_8);
        try{        
            exchange.sendResponseHeaders(rCode, msgBytes.length);
            OutputStream resBody = exchange.getResponseBody();
            resBody.write(msgBytes);
            resBody.close();
            System.out.println("(/registration) Responding to the user with error code "+rCode);
        } catch (IOException ioe) {
            System.out.println("An error with sending the errormsg has occurred");
        }
    }

    private boolean checkContentType(HttpExchange exchange){
        //Returns true if Content-Type header exists and is supported
        Headers reqHeaders = exchange.getRequestHeaders();
        String type = reqHeaders.getFirst("Content-Type");
        return type.equalsIgnoreCase("application/json");
    }
}
