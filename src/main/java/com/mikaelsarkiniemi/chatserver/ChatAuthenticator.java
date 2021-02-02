package com.mikaelsarkiniemi.chatserver;

import java.util.Hashtable;
import java.util.Map;

public class ChatAuthenticator extends com.sun.net.httpserver.BasicAuthenticator {

    //contains all usernames and passwords
    private Map <String,String> users = null;

    public ChatAuthenticator(){
        super("chat");
        users = new Hashtable<String,String>();
        //creating test user for unit testing
        users.put("dummy", "passwd");
    }

    @Override
    public boolean checkCredentials(String username, String password) {
        //Returns true if username and password matches, otherwise returns false
        if (users.get(username).equals(password)){
            return true;
        } else {
            System.out.println("GET request to /chat has been denied; info not correct");
            return false;
        }
    }

    public boolean addUser(String userName, String password) {
        if (users.putIfAbsent(userName, password) == null){
            return true;
        } else{
            return false;
        }
    }
}
