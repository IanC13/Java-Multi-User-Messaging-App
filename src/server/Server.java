package server;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.*;

import common.ProtocolConstants;


public class Server {
    private final Set<ClientHandler> clients = ConcurrentHashMap.newKeySet();
    private final ExecutorService pool = Executors.newCachedThreadPool();
    private final Set<String> usernames = ConcurrentHashMap.newKeySet();

    private Connection db;

    private void InitializeDB() throws IOException {
        try {
            Class.forName("org.sqlite.JDBC");

            db = DriverManager.getConnection("jdbc:sqlite:chat.db");

            try (Statement s = db.createStatement()) {
                s.executeUpdate("""
                  CREATE TABLE IF NOT EXISTS users (
                    id   INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT UNIQUE NOT NULL
                  );""");

                s.executeUpdate("""
                  CREATE TABLE IF NOT EXISTS messages (
                    id       INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id  INTEGER NOT NULL,
                    content  TEXT NOT NULL,
                    ts       DATETIME DEFAULT (strftime('%Y-%m-%d %H:%M:%S','now','localtime')),
                    FOREIGN KEY(user_id) REFERENCES users(id)
                  );""");
            }
        } catch (ClassNotFoundException | SQLException e) {
            throw new IOException("Error", e);
        }
    }


    void insertUser(String name) {
        try (PreparedStatement ps = db.prepareStatement(
            "INSERT OR IGNORE INTO users(name) VALUES(?)"
            )) {
            ps.setString(1, name);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Send chat history
    void sendChatHistory(PrintWriter out, int limit) {
        try (PreparedStatement ps = db.prepareStatement(
               "SELECT u.name, m.content, m.ts " +
               "  FROM messages m JOIN users u ON m.user_id=u.id " +
               " ORDER BY m.id DESC LIMIT ?"
             )) {
                ps.setInt(1, limit);

                try (ResultSet rs = ps.executeQuery()) {
                    var r = new ArrayList<String>();
                    while (rs.next()) {
                        String ts      = rs.getString("ts");
                        String sender  = rs.getString("name");
                        String content    = rs.getString("content");

                        r.add(ts + "|" + sender + "|" + content);
                    }
                    Collections.reverse(r);
                    for (String line: r) {
                        out.println("HISTORY " + line);       
                    }
                }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void start() throws IOException {
        InitializeDB();

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
    public void broadcast(String senderUsername, String msg) {
        // Broadcast user message history
        if (senderUsername != null) {
            try (PreparedStatement ps = db.prepareStatement(
                   "INSERT INTO messages(user_id,content) " +" VALUES((SELECT id FROM users WHERE name=?), ?)")) {

                ps.setString(1, senderUsername);
                ps.setString(2, msg);

                ps.executeUpdate();

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        for (ClientHandler c: clients) {
            c.send(senderUsername, msg);
        }
    }

    // To display all users connected
    void broadcastUserList() {
        String userList = String.join(",", usernames);

        // Prefix with a marker so clients know it’s a user‐list update
        broadcast(null, "USERLIST " + userList);
    }

    public static void main(String[] args) throws IOException {
        new Server().start();
    }
}