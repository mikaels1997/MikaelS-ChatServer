package com.mikaelsarkiniemi.chatserver;

import java.util.Hashtable;
import java.util.Map;

public class ChatAuthenticator extends com.sun.net.httpserver.BasicAuthenticator {

    //contains all usernames and passwords
    private Map <String,User> users = null;

    public ChatAuthenticator(){
        super("chat");
        users = new Hashtable<String,User>();
    }

    @Override
    public boolean checkCredentials(String username, String password) {
        //Returns true if username and password matches, otherwise returns false
        if (users.containsKey(username) && users.get(username).getPasswd().equals(password)){
            return true;
        } else {
            System.out.println("GET request to /chat has been denied; info not correct");
            return false;
        }
    }

    public boolean addUser(String nick, User user) {
        if (users.putIfAbsent(nick, user) == null){
            return true;
        } else{
            return false;
        }
    }
}
