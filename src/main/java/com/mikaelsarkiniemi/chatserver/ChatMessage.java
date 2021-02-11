package com.mikaelsarkiniemi.chatserver;

import java.time.OffsetDateTime;

public class ChatMessage {

    // UTC timestamp, username, message content
    public final OffsetDateTime sent;
    public String nick;
    public final String message;

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
