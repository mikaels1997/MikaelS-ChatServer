package com.mikaelsarkiniemi.chatserver;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.KeyStore;

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
            HttpsServer server = HttpsServer.create(new InetSocketAddress(8001), 0);

            SSLContext sslContext = chatServerSSLContext();
            server.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
                @Override
                public void configure(HttpsParameters params) {
                    // InetSocketAddress remote = params.getClientAddress()
                    SSLContext c = getSSLContext();
                    SSLParameters sslparams = c.getDefaultSSLParameters();
                    params.setSSLParameters(sslparams);
                }
            });

            // Initializing the contexts
            ChatAuthenticator auth = new ChatAuthenticator();
            HttpContext chatcontext = server.createContext("/chat", new ChatHandler());
            chatcontext.setAuthenticator(auth);
            server.createContext("/registration", new RegistrationHandler(auth));

            // Starting the server
            server.setExecutor(null);
            server.start();

            // Error handling
        } catch (FileNotFoundException e) {
            System.out.println("Certificate not found!");
            e.printStackTrace();
        } catch (IOException ioe) {
            System.out.println("Creation of the server failed");
        } catch (IllegalArgumentException iae) {
            System.out.println("Port number is not valid");
        }
    }

    private static SSLContext chatServerSSLContext() throws Exception {
        char[] passphrase = "mikaelsarkiniemiohjelmointi397!".toCharArray();
        KeyStore ks = KeyStore.getInstance("JKS");
        try {
            ks.load(new FileInputStream("keystore.jks"), passphrase);
        } catch (FileNotFoundException fnfe) {
            System.out.println("File not found!");
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
