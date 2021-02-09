package com.mikaelsarkiniemi.chatserver;

import java.time.OffsetDateTime;

public class ChatMessage {

    public OffsetDateTime sent;
    public String nick;
    public String message;

    public ChatMessage(String n, String m, OffsetDateTime s){
        this.nick = n;
        this.message = m;
        this.sent = s;
    }

    public String getNick(){
        return this.nick;
    }
    public String getMsg(){
        return this.message;
    }
    public OffsetDateTime getDate(){
        return this.sent;
    }
}
