package com.mikaelsarkiniemi.chatserver;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.stream.Collectors;

//implements the functions for user chatting
public class ChatHandler implements HttpHandler{

    private ArrayList<String> messages = new ArrayList<String>();
    
    public void handle(HttpExchange exchange) throws UnsupportedEncodingException {   
        if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            // Handle POST request
            handlePOST(exchange);
        } else if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            //Handle GET requests
            handleGET(exchange);
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

                String text = new BufferedReader(reader).lines()
                .collect(Collectors.joining("\n"));

                if(text.length() != 0){
                   //Everything OK, sending a response
                   messages.add(text);
                   reqBody.close();
                   exchange.sendResponseHeaders(200, -1);
                   System.out.println("POST request to /chat has been approved");
                } else {
                    String errorMsg = "Error 403: Unallowed string";
                    sendErrorMsg(errorMsg, exchange, 403);
                }
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

    private void handleGET(HttpExchange exchange) {
        try {
            String messageBody = "Test text\n";
            for (String msg : messages){
                messageBody += (msg + "\n");
            }
            byte[] msgBytes = messageBody.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, msgBytes.length);
            OutputStream resBody = exchange.getResponseBody();

            resBody.write(msgBytes);
            resBody.close();
            System.out.println("GET request to /chat has been approved");

        } catch (IOException ioe) {
            System.out.println("Sending response for GET request failed");
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
            System.out.println("(/chat)Following error has been sent: "+msg);
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
