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

    final ChatDatabase database = ChatDatabase.getInstance();
    static final String SERVERERROR = "Internal server error";
    static final String MESSAGE = "message";

    public void handle(HttpExchange exchange) throws UnsupportedEncodingException {
        if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            // Handle POST request including chat commands
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
        // Handles POST requests; user is submitting new message or command
        try {
            if (checkContentType(exchange)) {
                // Content-Type is supported

                // Receiving the request
                InputStream reqBody = exchange.getRequestBody();
                InputStreamReader reader = new InputStreamReader(reqBody, StandardCharsets.UTF_8);
                String text = new BufferedReader(reader).lines().collect(Collectors.joining("\n"));

                JSONObject js = new JSONObject(text);
                String msgContent = js.getString(MESSAGE);

                if (msgContent.strip().isEmpty()) {
                    // Message is empty or only contains white spaces
                    throw new JSONException("");
                } else {

                    if (msgContent.charAt(0) == '!') {
                        // Message is interpreted as command
                        handleCommand(msgContent, exchange);
                        return;
                    }

                    // Creating an instance of ChatMessage
                    String nick = js.getString("user");
                    String dateStr = js.getString("sent");
                    String channel;
                    if (js.has("channel")){
                        channel = js.getString("channel");
                    } else {
                        channel = "global";
                    }
                    OffsetDateTime odt = OffsetDateTime.parse(dateStr);
                    ChatMessage newMessage = new ChatMessage(nick, msgContent, odt, channel);

                    // Storing the message
                    database.storeMessage(nick, msgContent, newMessage.dateAsInt(), channel);

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
            sendErrorMsg(SERVERERROR, exchange, 500);
        } catch (JSONException je) {
            System.out.println("The info wasnt in proper json format");
            sendErrorMsg("Error 403: Unallowed string", exchange, 403);
        } catch (SQLException sqle) {
            System.out.print("SQLException during post request");
            sendErrorMsg("Internal server database error", exchange, 500);
        }
    }

    private void handleGET(HttpExchange exchange) {
        // Handles GET requests; user wants to see the message history of all channels
        try {

            // For temporal storage for msg history when responding to GET request
            ArrayList<ChatMessage> messages;

            if (exchange.getRequestHeaders().containsKey("If-Modified-Since")) {
                // Has done GET requests before
                String fromDateString = exchange.getRequestHeaders().getFirst("If-Modified-Since");

                // Formatting the date time of last get request by the user
                DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("EEE, dd MMM uuuu HH:mm:ss.SSS VV");
                ZonedDateTime z = ZonedDateTime.parse(fromDateString, dateFormat);
                OffsetDateTime fromWhichDate = z.toOffsetDateTime();
                long messageSince = fromWhichDate.toInstant().toEpochMilli();

                // Reading the message since the last GET request by that certain user
                messages = database.readMessages(messageSince, null);

            } else {
                // First ever GET request from this user
                messages = database.readMessages(0, null); // Reads the whole msg history
            }

            if (messages.isEmpty()) {
                // Empty message history
                exchange.sendResponseHeaders(204, -1);
                System.out.println("User requested empty msg history");
            } else {
                // Message history exists

                // Creating JSONarray containing every individual message information
                JSONArray msgJsHistory = new JSONArray();

                // "Lowest" time possible
                OffsetDateTime newest = OffsetDateTime.parse("1979-12-09T09:50:25+07:00");

                for (ChatMessage msg : messages) {

                    // Keeping track of the date of newest message
                    if (msg.getDate().isAfter(newest)) {
                        newest = msg.getDate();
                    }

                    JSONObject newJson = new JSONObject().put("user", msg.getNick()).put(MESSAGE, msg.getMsg())
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
            sendErrorMsg(SERVERERROR, exchange, 500);
        } catch (SQLException sqle) {
            System.out.println("SQLException during GET request");
            sendErrorMsg("Internal server database error", exchange, 500);
        }
    }

    public boolean handleCommand(String msg, HttpExchange exchange){
        // Possible chat commands: !view <target>, !create <target>, !channels
        // Target needed for every command but !channels

        String[] commandInfo = msg.split(" ");
        String command = commandInfo[0];
        try {
            if(commandInfo.length == 1){ // Has to be !channels command
                if(command.equals("!channels")){
                    // Requesting list of channels from database
                    ArrayList<String> channels = database.viewChannels();

                    // Formatting and forwarding the list to the user
                    JSONObject channelsJSON = new JSONObject().put("channels", channels);
                    String channelsString = channelsJSON.toString();
                    byte[] chnBytes = channelsString.getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(200, chnBytes.length);
                    OutputStream resBody = exchange.getResponseBody();
                    resBody.write(chnBytes);
                    System.out.println(channelsString);
                    System.out.println("Respond to !channels command has been successful");
                    return true;
                }
                String errorMsg = "Target for command is needed";
                sendErrorMsg(errorMsg, exchange, 202);
                return false;
            } if(commandInfo.length == 2) {
                String target = commandInfo[1];
                if(command.equals("!create")) {
                    database.createChannel(target);
                    System.out.println("A new channel<"+target+"> has been created");
                    exchange.sendResponseHeaders(200, -1);
                    return true;
                }
                if(command.equals("!view")) {
                    ArrayList<ChatMessage> messages;
                    JSONArray msgJsHistory = new JSONArray();

                    // Reading messages from specific channel
                    messages = database.readMessages(0, target);

                    // Formatting and forwarding the message history to the user
                    for (ChatMessage message : messages) {
                        JSONObject newJson = new JSONObject().put("user", message.getNick()).put(MESSAGE, message.getMsg())
                                .put("sent", message.getDate());
                        msgJsHistory.put(newJson);
                    }
                    String msgHistory = msgJsHistory.toString();
                    System.out.println(msgHistory);
                    byte[] msgBytes = msgHistory.getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(200, msgBytes.length);
                    OutputStream resBody = exchange.getResponseBody();
                    resBody.write(msgBytes);
                    return true;
                }
            } else {
                // Command not in proper format
                String errorMsg = "The command wasnt in proper format (!command <target>)";
                sendErrorMsg(errorMsg, exchange, 403);
            }
        } catch (IOException ioe) {
            System.out.println("IOException while responding to chat command");
            sendErrorMsg(SERVERERROR, exchange, 500);
        } catch (SQLException sqle) {
            System.out.println("SQLException while responding to chat command");
            sendErrorMsg("Duplicate channel", exchange, 403);
        }
        return false;
    }
}
