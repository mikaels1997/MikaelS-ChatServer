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
            String errorMsg = "Error 400: this type of request is not supported";
            sendErrorMsg(errorMsg, exchange, 400);
        }
    }

    private void handlePOST(HttpExchange exchange){
        try{
            if (checkContentType(exchange)){
                InputStream reqBody = exchange.getRequestBody();
                InputStreamReader reader = new InputStreamReader(reqBody, 
                StandardCharsets.UTF_8);

                String [] userinfo = new BufferedReader(reader).lines()
                .collect(Collectors.joining("\n")).split(":");

                //checking if user sent the info in correct form
                if (userinfo.length == 2){
                    if(auth.addUser(userinfo[0], userinfo[1])){
                        System.out.println("User: "+userinfo[0]+" added");
                    } else {
                        String errorMsg = "Error 403: User already registered";
                        sendErrorMsg(errorMsg, exchange, 403);
                    }
                } else{
                    String errorMsg = "Error 403: Unallowed string";
                    sendErrorMsg(errorMsg, exchange, 403);
                }

                reqBody.close();
                exchange.sendResponseHeaders(200, -1);
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
            System.out.println("(/registration)Following error has been sent: "+msg);
        } catch (IOException ioe) {
            System.out.println("An error has occurred");
        }
    }

    private boolean checkContentType(HttpExchange exchange){
        //Returns true if Content-Type header exists and is supported
        Headers reqHeaders = exchange.getRequestHeaders();
        String type = reqHeaders.getFirst("Content-Type");
        return type.equalsIgnoreCase("text/plain");
    }
}
