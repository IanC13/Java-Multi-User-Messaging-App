package server;

import java.io.*;
import java.net.*;
import java.util.Set;
import java.util.concurrent.*;

import common.ProtocolConstants;


public class Server {
    private final Set<ClientHandler> clients = ConcurrentHashMap.newKeySet();
    private final ExecutorService pool = Executors.newCachedThreadPool();
    private final Set<String> usernames = ConcurrentHashMap.newKeySet();

    public void start() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(ProtocolConstants.PORT)) {

            System.out.println("Server launched, listening on port " + ProtocolConstants.PORT + "\n");
            
            // Listen on socket, handle new clients
            while (true) {
                Socket sock = serverSocket.accept();

                ClientHandler handler = new ClientHandler(sock, this, clients, usernames);

                // Add new client to current list in HMap
                clients.add(handler);
                pool.execute(handler);
            }
        }
    }

    // Broadcasting message to all connected clients, except self
    public void broadcast(String senderUsername, String msg) { //, ClientHandler exclude) {

        for (ClientHandler c: clients) {
            c.send(senderUsername, msg);
            // if (c != exclude) {
            // }
        }
    }

    public static void main(String[] args) throws IOException {
        new Server().start();
    }
}