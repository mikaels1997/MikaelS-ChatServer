package com.mikaelsarkiniemi.chatserver;

import com.sun.net.httpserver.HttpHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.stream.Collectors;

// Implements the functions for user chatting
public class ChatHandler extends ContextHandler implements HttpHandler {

    // For temporal storage when responding to GET request (message history)
    private ArrayList<ChatMessage> messages = new ArrayList<>();
    ChatDatabase database = ChatDatabase.getInstance();

    public void handle(HttpExchange exchange) throws UnsupportedEncodingException {
        if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            // Handle POST request
            handlePOST(exchange);
        } else if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            // Handle GET requests
            handleGET(exchange);
        } else {
            // Request not supported
            String errorMsg = "Error 400: this type of request is not supported";
            sendErrorMsg(errorMsg, exchange, 400);
        }
    }

    private void handlePOST(HttpExchange exchange) {
        // Handles POST requests; user is submitting new message
        try {
            if (checkContentType(exchange)) {
                // Content-Type is supported

                // Receiving the request
                InputStream reqBody = exchange.getRequestBody();
                InputStreamReader reader = new InputStreamReader(reqBody, StandardCharsets.UTF_8);
                String text = new BufferedReader(reader).lines().collect(Collectors.joining("\n"));

                JSONObject js = new JSONObject(text);
                String msgContent = js.getString("message");

                if (msgContent.strip().isEmpty()) {
                    // Message is empty or only contains white spaces
                    throw new JSONException("");
                } else {

                    // Creating an instance of ChatMessage
                    String nick = js.getString("user");
                    String dateStr = js.getString("sent");
                    OffsetDateTime odt = OffsetDateTime.parse(dateStr);
                    ChatMessage newMessage = new ChatMessage(nick, msgContent, odt);

                    // Storing the message
                    database.storeMessage(nick, msgContent, newMessage.dateAsInt());

                    reqBody.close();
                    exchange.sendResponseHeaders(200, -1);
                    System.out.println("POST request to /chat has been approved");
                }
                reader.close();
            } else {
                String msg = "Error 400: Content-Type header needs to exist and be supported";
                sendErrorMsg(msg, exchange, 400);
            }
        } catch (IOException ioe) {
            System.out.println("Sending response for POST request failed");
            sendErrorMsg("Internal server error", exchange, 500);
        } catch (JSONException je) {
            System.out.println("The info wasnt in proper json format");
            sendErrorMsg("Error 403: Unallowed string", exchange, 403);
        } catch (SQLException sqle) {
            System.out.print("SQLException during post request");
            sendErrorMsg("Internal server database error", exchange, 500);
        }
    }

    private void handleGET(HttpExchange exchange) {
        // Handles GET requests; user wants to see the message history
        try {

            if (exchange.getRequestHeaders().containsKey("If-Modified-Since")) {
                // Has done GET requests before
                String fromDateString = exchange.getRequestHeaders().getFirst("If-Modified-Since");

                // Formatting the date time and reading the message history from that point
                DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("EEE, dd MMM uuuu HH:mm:ss.SSS VV");
                ZonedDateTime z = ZonedDateTime.parse(fromDateString, dateFormat);
                OffsetDateTime fromWhichDate = z.toOffsetDateTime();
                long messageSince = fromWhichDate.toInstant().toEpochMilli();
                messages = database.readMessages(messageSince);

            } else {
                // First get request from this user
                messages = database.readMessages(0); // Reads the whole msg history
            }

            if (messages.isEmpty()) {
                // Empty message history
                exchange.sendResponseHeaders(204, -1);
                System.out.println("User requested empty msg history");
            } else {

                // Creating JSONarray containing every individual message information
                JSONArray msgJsHistory = new JSONArray();

                // "Lowest" time possible
                OffsetDateTime newest = OffsetDateTime.parse("1979-12-09T09:50:25+07:00");

                for (ChatMessage msg : messages) {

                    // Keeping track of the date of newest message
                    if (msg.getDate().isAfter(newest)) {
                        newest = msg.getDate();
                    }

                    JSONObject newJson = new JSONObject().put("user", msg.getNick()).put("message", msg.getMsg())
                            .put("sent", msg.getDate());
                    msgJsHistory.put(newJson);
                }

                // Specifying "Last modified" header
                DateTimeFormatter dt = DateTimeFormatter.ofPattern("EEE, dd MMM uuuu HH:mm:ss.SSS");
                String newestString = dt.format(newest) + " GMT";
                exchange.getResponseHeaders().add("Last-Modified", newestString);

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
            sendErrorMsg("Internal server error", exchange, 500);
        } catch (SQLException sqle) {
            System.out.println("SQLException during GET request");
            sendErrorMsg("Internal server database error", exchange, 500);
        }
    }
}
