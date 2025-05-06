package client;

import common.ProtocolConstants;
import java.io.*;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Client {

    public void start() throws IOException {
        try (

            Socket socket = new Socket("localhost", ProtocolConstants.PORT);

            BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) { 
            System.out.println("Connected to server on port " + ProtocolConstants.PORT + "\n");

            ExecutorService pool = Executors.newSingleThreadExecutor();

            pool.execute(() -> {
                try {
                    String msg;
                    while ((msg = in.readLine()) != null) {
                        System.out.println(msg);
                    }
                } catch (IOException ignored) {}
            });

            String message;

            while ((message = console.readLine()) != null) {
                out.println(message);
            }
        }
    }

    public static void main(String[] args) throws IOException {
        new Client().start();
    }
}