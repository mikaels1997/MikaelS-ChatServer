package com.mikaelsarkiniemi.chatserver;

import java.io.Console;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import com.sun.net.httpserver.HttpsParameters;

public class ChatServer {

    public static void main(String[] args) throws Exception {

        try {

            if(args.length != 3){ // args defined in ".vscode/launch.json file"
                // database file name -> arg[0]
                // keystore file name -> arg[1]
                // passphrase -> arg[2]
                System.out.println("Didn't give 3 arguments for launch");
                return;
            }
            ChatDatabase database = ChatDatabase.getInstance();
            database.open(args[0]);

            HttpsServer server = HttpsServer.create(new InetSocketAddress(8001), 0);

            SSLContext sslContext = chatServerSSLContext(args[2].toCharArray(), args[1]);
            server.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
                @Override
                public void configure(HttpsParameters params) {
                    // InetSocketAddress remote = params.getClientAddress()
                    SSLContext c = getSSLContext();
                    SSLParameters sslparams = c.getDefaultSSLParameters();
                    params.setSSLParameters(sslparams);
                }
            });

            // Initializing the chat, registration and administration contexts
            ChatAuthenticator auth = new ChatAuthenticator();
            HttpContext chatcontext = server.createContext("/chat", new ChatHandler());
            chatcontext.setAuthenticator(auth);

            server.createContext("/registration", new RegistrationHandler(auth));

            AdminAuthenticator adm = new AdminAuthenticator();
            HttpContext admcontext = server.createContext("/administration", new AdministrationHandler(adm));
            admcontext.setAuthenticator(adm);

            // Starting the server
            ExecutorService executor = Executors.newCachedThreadPool();
            server.setExecutor(executor);
            server.start();

            Console console = System.console();
            boolean running = true;
            while (running){
                if (console.readLine().equals("/quit")) {
                    // Stopping the server
                    running = false;
                    server.stop(3);
                    database.close();
                }
            }

        // Error handling
        } catch (FileNotFoundException e) {
            System.out.println("Certificate not found!");
            e.printStackTrace();
        } catch (IOException ioe) {
            System.out.println("Creation of the server failed");
        } catch (IllegalArgumentException iae) {
            System.out.println("Port number is not valid");
        } catch (SQLException sqle) {
            System.out.println("Error while trying to close the database");
        }
    }

    private static SSLContext chatServerSSLContext(char[] passphrase, String keystoreName) throws Exception {
        KeyStore ks = KeyStore.getInstance("JKS");
        try {
            ks.load(new FileInputStream(keystoreName), passphrase);
        } catch (FileNotFoundException fnfe) {
            System.out.println("Keystore file is not found!");
        } catch (SecurityException se) {
            System.out.println("Passphrase is invalid");
        }

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, passphrase);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ks);

        SSLContext ssl = SSLContext.getInstance("TLS");
        ssl.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        return ssl;
    }
}
