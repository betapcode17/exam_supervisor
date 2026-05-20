package network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;

public class TCPClient {
    private final String host;
    private final int port;
    private Socket socket;
    private DataInputStream dis;
    private DataOutputStream dos;
    private int lastInvigilatorCount;
    private int lastSupervisorCount;
    private String lastOutputSessionDir;

    public TCPClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void connect() throws IOException {
        socket = new Socket(host, port);
        dis = new DataInputStream(socket.getInputStream());
        dos = new DataOutputStream(socket.getOutputStream());
        System.out.println("Connected to server at " + host + ":" + port);
    }

    public boolean loadFile(String filePath) throws IOException {
        dos.writeUTF("LOAD");
        dos.flush();
        
        File file = new File(filePath);
        if (!file.exists()) {
            throw new FileNotFoundException("File not found: " + filePath);
        }
        
        // Send filename and size
        dos.writeUTF(file.getName());
        dos.writeLong(file.length());
        dos.flush();
        
        // Send file content with larger buffer for better performance
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[8192]; // 8KB buffer for faster I/O
            int read;
            while ((read = fis.read(buffer)) > 0) {
                dos.write(buffer, 0, read);
            }
        }
        dos.flush();
        
        System.out.println("File sent to server: " + file.getName());
        
        // Receive response
        String response = dis.readUTF();
        System.out.println("Server response: " + response);
        
        if ("SUCCESS".equals(response)) {
            int invigilators = dis.readInt();
            int rooms = dis.readInt();
            System.out.println("File loaded successfully - Invigilators: " + invigilators + ", Rooms: " + rooms);
            return true;
        } else {
            System.err.println("Failed to load file: " + response);
            return false;
        }
    }

    public boolean generateAssignment(String filePath, int numberOfRooms, 
                                     int numberOfInvigilators, int shift) throws IOException {
        return generateAssignment(filePath, numberOfRooms, numberOfInvigilators, shift, null);
    }

    public boolean generateAssignment(String filePath, int numberOfRooms,
                                      int numberOfInvigilators, int shift,
                                      java.util.function.Consumer<String> logHandler) throws IOException {
        dos.writeUTF("ASSIGN");
        dos.flush();

        // Send parameters
        dos.writeUTF(filePath);
        dos.writeInt(numberOfRooms);
        dos.writeInt(numberOfInvigilators);
        dos.writeInt(shift);
        dos.flush();

        // Read responses until we get SUCCESS or ERROR
        while (true) {
            String response = dis.readUTF();
            if (response == null) return false;

            if (response.startsWith("LOG:")) {
                String msg = response.substring(4);
                if (logHandler != null) {
                    try { logHandler.accept(msg); } catch (Exception ex) { System.err.println("Log handler error: " + ex.getMessage()); }
                } else {
                    System.out.println("LOG: " + msg);
                }
                continue;
            }

            System.out.println("Server response: " + response);

            if ("SUCCESS".equals(response)) {
                lastInvigilatorCount = dis.readInt();
                lastSupervisorCount = dis.readInt();
                String outputSessionDir = dis.readUTF();
                this.lastOutputSessionDir = outputSessionDir;
                int fileCount = dis.readInt();
                System.out.println("Receiving result files: " + fileCount);
                System.out.println("Assignment summary - Invigilators: " + lastInvigilatorCount + ", Supervisors: " + lastSupervisorCount);

                File outputDir = new File("output", outputSessionDir);
                if (!outputDir.exists()) {
                    outputDir.mkdirs();
                }

                for (int i = 0; i < fileCount; i++) {
                    String fileName = dis.readUTF();
                    long fileSize = dis.readLong();
                    System.out.println("Receiving file " + (i + 1) + "/" + fileCount + ": " + fileName + " (" + fileSize + " bytes)");

                    String outputPath = new File(outputDir, fileName).getPath();
                    try (FileOutputStream fos = new FileOutputStream(outputPath)) {
                        byte[] buffer = new byte[8192];
                        long remaining = fileSize;
                        int read;
                        while (remaining > 0 && (read = dis.read(buffer, 0, (int) Math.min(buffer.length, remaining))) > 0) {
                            fos.write(buffer, 0, read);
                            remaining -= read;
                        }
                    }

                    System.out.println("Result saved to: " + outputPath);
                }
                return true;
            } else if (response.startsWith("ERROR:")) {
                System.err.println("Failed to generate assignment: " + response);
                return false;
            } else {
                // Unknown response - continue or break
                System.err.println("Unknown response from server: " + response);
                return false;
            }
        }
    }

    public void disconnect() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Error while disconnecting: " + e.getMessage());
        }
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    public int getLastInvigilatorCount() {
        return lastInvigilatorCount;
    }

    public int getLastSupervisorCount() {
        return lastSupervisorCount;
    }

    public String getLastOutputSessionDir() {
        return lastOutputSessionDir;
    }
}
