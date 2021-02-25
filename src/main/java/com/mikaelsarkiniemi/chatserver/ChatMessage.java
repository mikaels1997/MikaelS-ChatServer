package com.mikaelsarkiniemi.chatserver;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public class ChatMessage {

    // UTC timestamp, username, message content
    public OffsetDateTime sent;
    public String nick;
    public String message;

    public ChatMessage(String n, String m, OffsetDateTime s){
        this.nick = n;
        this.message = m;
        this.sent = s;
    }

    public long dateAsInt() {
        // Implemented for storing the message date as numerical form to database.db
        return sent.toInstant().toEpochMilli();
    }
    public void setSent(long epoch) {
        this.sent = OffsetDateTime.ofInstant(Instant.ofEpochMilli(epoch), ZoneOffset.UTC);
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
