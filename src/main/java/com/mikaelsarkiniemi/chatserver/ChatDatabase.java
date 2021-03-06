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

    public boolean registerUser(String name, String passwd, String email) {
        if (connection != null) {

            // Hash + salt
            byte bytes[] = new byte[13];
            secureRandom.nextBytes(bytes);
            String saltBytes = new String(Base64.getEncoder().encode(bytes));
            String salt = "$6$" + saltBytes;
            String hashedPassword = Crypt.crypt(passwd, salt);

            try (Statement createStatement = connection.createStatement();) {
                String insertToUsers = "INSERT INTO USERS " + "VALUES('" + name + "','" + hashedPassword + "','" + email
                        + "','" + salt + "')";
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

    // Stores individual message to the database
    public void storeMessage(String nick, String msg, long date) throws SQLException {
        try (Statement createStatement = connection.createStatement();) {
            String insertToMessages = "INSERT INTO MESSAGES VALUES('" + nick + "', '" + msg + "', '" + date + "')";
            createStatement.executeUpdate(insertToMessages);
        }
    }

    public ArrayList<ChatMessage> readMessages(long since) throws SQLException {

        // Stores the messages temporarily to this list and then returns it
        ArrayList<ChatMessage> messages = new ArrayList<>();

        try (Statement createStatement = connection.createStatement();) {
            String getMessages = "SELECT * FROM MESSAGES WHERE date > " + since + " ORDER BY date ASC";
            ResultSet rs = createStatement.executeQuery(getMessages);
            while (rs.next()) {
                String n = rs.getString("nickname");
                String m = rs.getString("content");
                long date = rs.getLong("date");
                ChatMessage msg = new ChatMessage(n, m, null);

                // Sets the time as OffsetTimeDate type
                msg.setSent(date);

                messages.add(msg);
            }
        }
        return messages;
    }

    // Initializes the database if it does not already exist
    private boolean initializeDatabase() {
        if (connection != null) {
            try (Statement createStatement = connection.createStatement();) {

                // Table configurations
                String createUsersString = "CREATE TABLE USERS " + "(username VARCHAR(255) not NULL, "
                        + " password VARCHAR(255) not NULL, " + " email VARCHAR(255), " + " salt VARCHAR(255), "
                        + "PRIMARY KEY ( username ))";
                String createMessageString = "CREATE TABLE MESSAGES " + "(nickname VARCHAR(255) not NULL, "
                        + " content VARCHAR(255), " + " date BIGINT, " + "PRIMARY KEY ( nickname, content, date))";

                createStatement.executeUpdate(createUsersString);
                createStatement.executeUpdate(createMessageString);
                return true;
            } catch (SQLException sqle) {
                System.out.println("SQLEXCEPTION while creating database");
            }
        }
        return false;
    }
}
