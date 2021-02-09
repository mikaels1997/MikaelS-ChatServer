package com.mikaelsarkiniemi.chatserver;

public class User {
    String name;
    String email;
    String passwd;

    public User(String n, String e, String p){
        this.name = n;
        this.email = e;
        this.passwd = p;
    }

    public String getUsername(){
        return this.name;
    }
    public void setUsername(String username){
        this.name = username;
    }
    public String getEmail(){
        return this.email;
    }
    public void setEmail(String em){
        this.email = em;
    }
    public String getPasswd(){
        return this.passwd;
    }
}