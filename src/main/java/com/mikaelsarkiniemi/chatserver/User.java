package com.mikaelsarkiniemi.chatserver;

// Contains information of individual user
public class User {
    String name;
    String email;
    String passwd;

    public User(String n, String e, String p) {
        this.name = n;
        this.email = e;
        this.passwd = p;
    }

    public String getUsername() {
        return this.name;
    }

    public void setUsername(String username) {
        this.name = username;
    }

    public String getEmail() {
        return this.email;
    }

    public void setEmail(String em) {
        this.email = em;
    }

    public void setPasswd(String p) {
        this.passwd = p;
    }

    public String getPasswd() {
        return this.passwd;
    }
}
