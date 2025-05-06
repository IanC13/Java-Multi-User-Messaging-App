
package server;

import java.io.*;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


// Server side client handler code,
public class ClientHandler implements Runnable {
    private final Socket socket;
    private final Server server;
    private final Set<ClientHandler> clients;
    private final Set<String> activeUsernames;


    private PrintWriter out;
    private BufferedReader in;
    private String username = null;

    public ClientHandler(Socket socket, Server server, Set<ClientHandler> clients, Set<String> usernames) {
        this.socket  = socket;
        this.server  = server;
        this.clients = clients;
        this.activeUsernames = usernames;
    }

    @Override
    public void run() {
        try {
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);


            // Ask for username
            out.println("ENTER NAME:");
            String name = in.readLine();
            
            // Validate username

            if (name == null) {
                cleanup();
                return;
            }

            while (name == null || name.isBlank() || !activeUsernames.add(name)) {
                out.println("Invalid Username or already in use. Choose a new name");
                out.println("ENTER NAME:");
                name = in.readLine();

                if (name == null) {
                    cleanup();
                    return;
                }
            }

            out.println("You have entered the chat!");

            // Announce new user
            this.username = name;
            server.broadcast(null, name + " has joined the chat.");

            String message;

            while ((message = in.readLine()) != null) {
                System.out.println("Received: " + message);

                // Broadcast message to all clients except self
                server.broadcast(this.username, message);
            }

        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            cleanup();
        }
    }

    public void send(String senderUsername, String msg) {
        if (senderUsername == null) {
            out.println(msg);

        } else {
            out.println(senderUsername + ": " + msg);
        }
    }

    private void cleanup() {
        clients.remove(this);
        if (username != null) {
            activeUsernames.remove(username);
            server.broadcast(null, username + " has left the chat");
        }
        try { 
            socket.close(); 
        } catch (IOException ignored) {}
    }
}