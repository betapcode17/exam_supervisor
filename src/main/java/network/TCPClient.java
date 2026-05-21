package network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.function.Consumer;

public class TCPClient {
    private final String host;
    private final int port;
    private Socket socket;
    private DataInputStream dis;
    private DataOutputStream dos;
    private int lastInvigilatorCount;
    private int lastSupervisorCount;
    private String lastOutputSessionDir;
    private final Consumer<String> logHandler;

    public TCPClient(String host, int port) {
        this(host, port, null);
    }

    public TCPClient(String host, int port, Consumer<String> logHandler) {
        this.host = host;
        this.port = port;
        this.logHandler = logHandler;
    }

    private void log(String message) {
        if (logHandler != null) {
            logHandler.accept(message);
        }
    }

    public void connect() throws IOException {
        socket = new Socket(host, port);
        dis = new DataInputStream(socket.getInputStream());
        dos = new DataOutputStream(socket.getOutputStream());
        log("Đã kết nối tới server " + host + ":" + port);
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
        
        log("Đã gửi file: " + file.getName());
        
        // Receive response
        String response = dis.readUTF();
        log("Phản hồi server: " + response);
        
        if ("SUCCESS".equals(response)) {
            int invigilators = dis.readInt();
            int rooms = dis.readInt();
            log("Nạp file thành công - Cán bộ: " + invigilators + ", Phòng: " + rooms);
            return true;
        } else {
            log("Nạp file thất bại: " + response);
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
                    try { logHandler.accept(msg); } catch (Exception ex) { /* ignore UI log errors */ }
                } else {
                    log(msg);
                }
                continue;
            }

            log("Phản hồi server: " + response);

            if ("SUCCESS".equals(response)) {
                lastInvigilatorCount = dis.readInt();
                lastSupervisorCount = dis.readInt();
                String outputSessionDir = dis.readUTF();
                this.lastOutputSessionDir = outputSessionDir;
                int fileCount = dis.readInt();
                log("Nhận " + fileCount + " file kết quả");
                log("Tổng kết - Giám thị: " + lastInvigilatorCount + ", Giám sát: " + lastSupervisorCount);

                File outputDir = new File("output", outputSessionDir);
                if (!outputDir.exists()) {
                    outputDir.mkdirs();
                }

                for (int i = 0; i < fileCount; i++) {
                    String fileName = dis.readUTF();
                    long fileSize = dis.readLong();
                    log("Đang nhận file " + (i + 1) + "/" + fileCount + ": " + fileName + " (" + fileSize + " bytes)");

                    String outputPath = new File(outputDir, fileName).getPath();
                    try (FileOutputStream fos = new FileOutputStream(outputPath)) {
                        byte[] buffer = new byte[8192];
                        long remaining = fileSize;
                        while (remaining > 0) {
                            int toRead = (int) Math.min(buffer.length, remaining);
                            try {
                                dis.readFully(buffer, 0, toRead);
                                fos.write(buffer, 0, toRead);
                                remaining -= toRead;
                            } catch (EOFException eof) {
                                log("ERROR: Connection closed before receiving all data. Expected " + fileSize + " bytes, got " + (fileSize - remaining) + " bytes.");
                                throw new IOException("Incomplete file transfer for " + fileName, eof);
                            }
                        }
                        fos.flush();
                    }

                    log("Đã lưu kết quả: " + outputPath);
                }
                return true;
            } else if (response.startsWith("ERROR:")) {
                log("Tạo phân công thất bại: " + response);
                return false;
            } else {
                // Unknown response - continue or break
                log("Phản hồi không xác định: " + response);
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
            log("Lỗi khi ngắt kết nối: " + e.getMessage());
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
