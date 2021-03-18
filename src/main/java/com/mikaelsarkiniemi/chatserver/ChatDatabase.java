package com.mikaelsarkiniemi.chatserver;

import java.io.File;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Base64;

import org.apache.commons.codec.digest.Crypt;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;

// Handles SQL-server interface
public class ChatDatabase {

    private static ChatDatabase singleton;
    private Connection connection;
    private SecureRandom secureRandom = new SecureRandom();

    private ChatDatabase() {
        // Singleton (only one instance of this class is created)
    }

    public static synchronized ChatDatabase getInstance() {
        if (singleton == null) {
            singleton = new ChatDatabase();
        }
        return singleton;
    }

    public void open(String dbName) throws SQLException {
        File f = new File(dbName);
        boolean doesExist = f.exists() && !f.isDirectory();
        String jdbcAddress = "jdbc:sqlite:" + f.getAbsolutePath();
        this.connection = DriverManager.getConnection(jdbcAddress);
        if (!doesExist) {
            // If database.db does not exist it'll be created
            initializeDatabase();
        }
    }

    public void close() throws SQLException {
        connection.close();
    }

    // Registers user unless duplicate already exists
    public boolean registerUser(String name, String passwd, String email, String role) {
        if (connection != null) {

            // Hash + salt
            byte bytes[] = new byte[13];
            secureRandom.nextBytes(bytes);
            String saltBytes = new String(Base64.getEncoder().encode(bytes));
            String salt = "$6$" + saltBytes;
            String hashedPassword = Crypt.crypt(passwd, salt);

            try (Statement createStatement = connection.createStatement();) {
                String insertToUsers = "INSERT INTO USERS " + "VALUES('" + name + "','" + hashedPassword + "','" + email
                        + "','" + salt + "','" + role + "')";
                createStatement.executeUpdate(insertToUsers);
                return true;
            } catch (SQLException sqle) {
                System.out.println("User " + name + " already registered");
                return false;
            }
        }
        return false;
    }

    // Check if name-password pair has a match in the database
    public boolean validateUser(String name, String passwd) {
        if (connection != null) {
            try (Statement createStatement = connection.createStatement();) {
                String passwdQuery = "SELECT password FROM USERS WHERE username = '" + name + "'";
                ResultSet rs = createStatement.executeQuery(passwdQuery);
                if (rs.next()) {
                    String hashedPassword = rs.getString("password");
                    String passCheck = Crypt.crypt(passwd, hashedPassword);
                    if (hashedPassword.equals(passCheck)) {
                        return true; // User authenticated
                    }
                }
            } catch (SQLException sqle) {
                System.out.println("SQLException during user validating");
            }
        }
        return false;
    }

    // Edits target's password, email and role if the actor is an admin 
    public boolean changeInfo(String [] details) {
        // Details contains the username of target, the new password, new email
        // and new role (username cannot be changed)

        // Hash + salt
        byte bytes[] = new byte[13];
        secureRandom.nextBytes(bytes);
        String saltBytes = new String(Base64.getEncoder().encode(bytes));
        String salt = "$6$" + saltBytes;
        details[1] = Crypt.crypt(details[1], salt);

        try (Statement createStatement = connection.createStatement();) {
            // Updating the info
            String updateString = "UPDATE users SET password = '"+details[1]+
             "', email = '" +details[3]+ "', role= '"+details[2] + 
             "' WHERE username = '" + details[0] + "'";
            int isUpdated = createStatement.executeUpdate(updateString);
            if (isUpdated == 0) {
                return false;
            }
            return true;
        } catch (SQLException sqle) {
            System.out.println("SQLException while updating user info");
        }
        return false;
    }

    // Deletes certain user if the requester is an admin
    public boolean deleteUser(String target) {
        try (Statement createStatement = connection.createStatement();){
            String removeString = "DELETE FROM users WHERE username = '"+target+"'";
            int isUpdated = createStatement.executeUpdate(removeString);
            if (isUpdated == 0) {
                return false;
            }
            return true;
        } catch (SQLException sqle) {
            System.out.println("SQLException while removing an user");
        }
        return false;
    }

    // Confirms if the user is an admin
    public boolean isAdmin(String name) {
        try (Statement createStatement = connection.createStatement();) {
            String statusString = "SELECT role FROM USERS where USERNAME = '"
            + name + "'";
            ResultSet rs = createStatement.executeQuery(statusString);
            if (rs.next()){
                String role = rs.getString("role");
                return role.equals("administrator");
            }
        } catch (SQLException sqle) {
            System.out.println("SQLException while checking the role of the user");
        }
        return false;
    }

    // Stores individual message to the database
    public void storeMessage(String nick, String msg, long date, String channel) throws SQLException {
        try (Statement createStatement = connection.createStatement();) {
            String insertToMessages = "INSERT INTO MESSAGES VALUES('" + nick + "', '" + msg + "', '" + date +
             "', '" + channel +"')";
            createStatement.executeUpdate(insertToMessages);
        }
    }

    public ArrayList<ChatMessage> readMessages(long since, String channel) throws SQLException {

        // Stores the messages temporarily to this list and then returns it
        ArrayList<ChatMessage> messages = new ArrayList<>();

        try (Statement createStatement = connection.createStatement();) {

            String getMessages;
            if(channel != null) {
                // Certain channel
                getMessages = "SELECT * FROM MESSAGES WHERE channel = '" + channel +"'";
            } else {
                // All channels
                getMessages = "SELECT * FROM MESSAGES WHERE date > " 
                + since + " ORDER BY date ASC";
            }
            ResultSet rs = createStatement.executeQuery(getMessages);
            while (rs.next()) {
                String n = rs.getString("nickname");
                String m = rs.getString("content");
                long date = rs.getLong("date");
                String c = rs.getString("channel");
                ChatMessage msg = new ChatMessage(n, m, null, c);

                // Sets the time as OffsetTimeDate type
                msg.setSent(date);

                messages.add(msg);
            }
        }
        return messages;
    }

    // Adds new channel to the database
    public void createChannel(String name) throws SQLException{
        try (Statement createStatement = connection.createStatement();) {
            String createChannelString = "INSERT INTO channels VALUES('" +
            name + "')";
            createStatement.executeUpdate(createChannelString);
        }
    }

    // Returns a list of existing channels
    public ArrayList<String> viewChannels(){
        ArrayList<String> channels = new ArrayList<>();
        try (Statement createStatement = connection.createStatement();) {
            String viewChannelsString = "SELECT * FROM channels";
            ResultSet rs = createStatement.executeQuery(viewChannelsString);
            while(rs.next()){
                String channel = rs.getString("channel");
                channels.add(channel);
            }
        } catch (SQLException sqle) {
            System.out.println("SQLException while creating new channel");
        }
        return channels;
    }

    // Initializes the database if it does not already exist
    private boolean initializeDatabase() {
        if (connection != null) {
            try (Statement createStatement = connection.createStatement();) {

                // Table configurations
                String createUsersString = "CREATE TABLE USERS " + "(username VARCHAR(255) not NULL, "
                        + " password VARCHAR(255) not NULL, " + " email VARCHAR(255), " + " salt VARCHAR(255), "
                        + " role VARCHAR(255) not NULL, " + "PRIMARY KEY ( username ))"; // " role VARCHAR(255) not NULL, "+ 
                String createMessageString = "CREATE TABLE MESSAGES " + "(nickname VARCHAR(255) not NULL, "
                        + " content VARCHAR(255), " + " date BIGINT, "+ " channel VARCHAR(255), "+ "PRIMARY KEY ( nickname, content, date))";
                String createChannelsString = "CREATE TABLE CHANNELS " + "(channel VARCHAR(255) not NULL, "
                        + "PRIMARY KEY ( channel ))";   

                createStatement.executeUpdate(createUsersString);
                createStatement.executeUpdate(createMessageString);
                createStatement.executeUpdate(createChannelsString);
                return true;
            } catch (SQLException sqle) {
                System.out.println("SQLEXCEPTION while creating database");
            }
        }
        return false;
    }
}
