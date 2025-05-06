
package server;

import java.io.*;
import java.net.Socket;
import java.util.Set;


// Server side client handler code,
public class ClientHandler implements Runnable {
    private final Socket socket;
    private final Server server;
    private final Set<ClientHandler> clients;

    private       PrintWriter out;
    private       BufferedReader in;

    public ClientHandler(Socket socket, Server server, Set<ClientHandler> clients) {
        this.socket  = socket;
        this.server  = server;
        this.clients = clients;
    }

    @Override
    public void run() {
        try {
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            String message;

            while ((message = in.readLine()) != null) {
                System.out.println("Received: " + message);

                // Broadcast message to all clients except self
                server.broadcast(message, this);
            }

        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    public void send(String msg) {
        out.println(msg);
    }

    private void cleanup() {
        clients.remove(this);
        try { 
            socket.close(); 
        } catch (IOException ignored) {}
    }
}