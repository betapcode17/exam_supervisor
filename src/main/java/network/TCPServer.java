package network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class TCPServer {
    private ServerSocket serverSocket;
    private int port;
    private boolean running = false;

    public TCPServer(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;
        System.out.println("Server started on port " + port);

        try {
            while (running) {
                Socket socket = null;
                try {
                    socket = serverSocket.accept();
                } catch (IOException acceptEx) {
                    // If serverSocket was closed due to stop(), accept will throw.
                    if (!running) {
                        // expected during shutdown
                        break;
                    } else {
                        throw acceptEx;
                    }
                }

                if (socket != null) {
                    System.out.println("New client connected: " + socket.getInetAddress().getHostAddress());
                    ClientHandler handler = new ClientHandler(socket);
                    Thread thread = new Thread(handler);
                    thread.start();
                }
            }
        } finally {
            // ensure socket closed when leaving
            stop();
        }
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        int port = 8888;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number");
            }
        }
        
        TCPServer server = new TCPServer(port);
        try {
            server.start();
        } catch (IOException e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
