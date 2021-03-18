package com.mikaelsarkiniemi.chatserver;

public class ChatAuthenticator extends com.sun.net.httpserver.BasicAuthenticator {

    private ChatDatabase database = ChatDatabase.getInstance();

    public ChatAuthenticator() {
        super("chat");
    }

    @Override
    public boolean checkCredentials(String username, String password) {
        // Returns true if username and password matches, otherwise returns false
        if (database.validateUser(username, password)) {
            return true;
        } else {
            System.out.println("GET request has been denied; info not correct");
            return false;
        }
    }

    public boolean addUser(User user) {
        // returns true if user does not already exist
        return database.registerUser(user.getUsername(), 
        user.getPasswd(), user.getEmail(), user.getRole());
    }
}
