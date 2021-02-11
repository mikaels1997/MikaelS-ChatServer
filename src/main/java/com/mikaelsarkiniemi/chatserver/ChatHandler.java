package com.mikaelsarkiniemi.chatserver;

import com.sun.net.httpserver.HttpHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.Collections;

//implements the functions for user chatting
public class ChatHandler implements HttpHandler{

    private ArrayList<ChatMessage> messages = new ArrayList<>();
    
    public void handle(HttpExchange exchange) throws UnsupportedEncodingException {   
        if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            // Handle POST request
            handlePOST(exchange);
        } else if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            //Handle GET requests
            handleGET(exchange);
        } else {
            //Request not supported
            String errorMsg = "Error 400: this type of request is not supported";
            sendErrorMsg(errorMsg, exchange, 400);
        }
    }

    private void handlePOST(HttpExchange exchange){
        //Handles POST requests; user is submitting new message
        try{
            if (checkContentType(exchange)){
                // Content-Type is supported

                InputStream reqBody = exchange.getRequestBody();
                InputStreamReader reader = new InputStreamReader(reqBody, 
                StandardCharsets.UTF_8);
                String text = new BufferedReader(reader).lines()
                .collect(Collectors.joining("\n"));

                JSONObject js = new JSONObject(text);
                String msgContent = js.getString("message");

                if (msgContent.strip().isEmpty()){
                    // Message is empty or only contains white spaces
                    throw new JSONException("");
                } else {

                    // Creating an instance of ChatMessage
                    String nick = js.getString("user");
                    String dateStr = js.getString("sent");
                    OffsetDateTime odt = OffsetDateTime.parse(dateStr);
                    ChatMessage newMessage = new ChatMessage(nick, msgContent, odt);

                    // Storing the message
                    messages.add(newMessage);

                    // Sort all stored messages by date
                    Collections.sort(messages, new Comparator<ChatMessage>() {
                        @Override
                        public int compare(ChatMessage lhs, ChatMessage rhs) {
                            return lhs.sent.compareTo(rhs.sent);
                        }
                    });
                    
                    reqBody.close();
                    exchange.sendResponseHeaders(200, -1);
                    System.out.println("POST request to /chat has been approved");
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
        } catch (JSONException je){
            //The info wasn't in proper JSON format
            System.out.println("The info wasnt in proper json format");
            String errorMsg = "Error 403: Unallowed string";
            sendErrorMsg(errorMsg, exchange, 403);
        }
    }

    private void handleGET(HttpExchange exchange) {
        //Handles GET requests; user wants to see the message history
        try {
            if (messages.isEmpty()) {
                // No message history
                exchange.sendResponseHeaders(204, -1);
                System.out.println("User requested empty msg history");
            } else {
                // Message history exists

                //Creating JSONarray containing every individual message information
                JSONArray msgJsHistory = new JSONArray();
                for (ChatMessage msg : messages){
                    System.out.println(msg.getDate());
                    JSONObject newJson = new JSONObject().put("user", msg.getNick())
                    .put("message", msg.getMsg()).put("sent", msg.getDate());
                    msgJsHistory.put(newJson);
                }
      
                // Forwarding the message history to the user
                String msgHistory = msgJsHistory.toString();
                byte[] msgBytes = msgHistory.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, msgBytes.length);
                OutputStream resBody = exchange.getResponseBody();
                resBody.write(msgBytes);
                
                resBody.close();
                System.out.println("GET request to /chat has been approved");
            }
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
            System.out.println("(/chat) Responding to the user with error code "+rCode);
        } catch (IOException ioe) {
            System.out.println("An error has occurred");
        }
    }

    private boolean checkContentType(HttpExchange exchange){
        //Returns true if Content-Type header exists and is supported
        Headers reqHeaders = exchange.getRequestHeaders();
        String type = reqHeaders.getFirst("Content-Type");
        return type.equalsIgnoreCase("application/json");
    }
}
