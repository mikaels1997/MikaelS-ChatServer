package com.mikaelsarkiniemi.chatserver;

// Authenticator for /administration
public class AdminAuthenticator extends com.sun.net.httpserver.BasicAuthenticator {

    private ChatDatabase database = ChatDatabase.getInstance();

    public AdminAuthenticator() {
        super("administration");
    }

    @Override
    public boolean checkCredentials(String username, String password) {
        // Returns true if username and password matches and admin status is active
        if (database.validateUser(username, password) && database.isAdmin(username)) {
            return true;
        } else {
            System.out.println("POST request to /administration denied");
            return false;
        }
    }

    public boolean updateUserInfo(String [] details) {
        // returns true if the user info update was successful
        return database.changeInfo(details);
    }

    public boolean removeUser(String target) {
        // returns true if the removal of the user was successful
        return database.deleteUser(target);
    }
}
