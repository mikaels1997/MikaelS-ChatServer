package com.mikaelsarkiniemi.chatserver;

// Contains information of individual user
public class User {
    String name;
    String email;
    String passwd;
    String role;

    public User(String n, String e, String p, String r) {
        this.name = n;
        this.email = e;
        this.passwd = p;
        this.role  = r;
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

    public String getRole() {
        return this.role;
    }

    public void setRole(String r) {
        this.role = r;
    }
}
