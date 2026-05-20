package network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import model.bean.AssignmentResult;
import model.bean.Invigilator;
import model.bean.Room;
import model.bean.ScheduleInput;
import model.bo.AssignmentBO;
import model.dao.InvigilatorDAO;
import model.dao.RoomDAO;
import util.DatabaseConnection;
import util.ExcelReader;
import util.ExcelWriter;

public class ClientHandler implements Runnable {

        private static final DateTimeFormatter TIMESTAMP_FORMATTER =
                        DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    private Socket socket;

    private DataInputStream dis;

    private DataOutputStream dos;

    public ClientHandler(Socket socket) {

        this.socket = socket;
    }

    @Override
    public void run() {

        try {

            dis = new DataInputStream(
                    socket.getInputStream()
            );

            dos = new DataOutputStream(
                    socket.getOutputStream()
            );

                        while (true) {

                                String command;

                                try {

                                        command = dis.readUTF();

                                } catch (EOFException | SocketException e) {

                                        System.out.println("Client disconnected");
                                        break;
                                }

                                System.out.println(
                                                "Received command: " + command
                                );

                                switch (command) {

                                        case "LOAD":

                                                handleLoad();

                                                break;

                                        case "ASSIGN":

                                                handleAssign();

                                                break;

                                        case "QUIT":

                                                System.out.println("Client requested disconnect");
                                                return;

                                        default:

                                                dos.writeUTF(
                                                                "ERROR:Unknown command"
                                                );

                                                dos.flush();
                                }
            }

        } catch (Exception e) {

            e.printStackTrace();

        } finally {

            try {

                if (socket != null) {

                    socket.close();
                }

            } catch (IOException e) {

                e.printStackTrace();
            }
        }
    }

    private void handleLoad() throws IOException {

        try {

            // =====================================================
            // RECEIVE FILE
            // =====================================================

                        String fileName = new File(dis.readUTF()).getName();

                        if (fileName == null || fileName.trim().isEmpty()) {
                                throw new IOException("Invalid upload filename");
                        }

            long fileSize = dis.readLong();

            String inputDir = createTimestampedDirectory(
                    "uploads" + File.separator + "input"
            );

            String filePath =
                    inputDir +
                    File.separator +
                    fileName;

            try (FileOutputStream fos =
                         new FileOutputStream(filePath)) {

                byte[] buffer = new byte[8192];

                long remaining = fileSize;

                int read;

                while (
                        remaining > 0 &&
                        (
                                read = dis.read(
                                        buffer,
                                        0,
                                        (int) Math.min(
                                                buffer.length,
                                                remaining
                                        )
                                )
                        ) > 0
                ) {

                    fos.write(buffer, 0, read);

                    remaining -= read;
                }
            }

            System.out.println(
                    "File received: " + filePath
            );

            // =====================================================
            // READ EXCEL
            // =====================================================

            ExcelReader excelReader =
                    new ExcelReader(filePath);

            List<Invigilator> invigilators =
                    excelReader.readInvigilators();

            List<Room> rooms =
                    excelReader.readRooms();

                        // =====================================================
                        // CHECK DUPLICATE MA_GV IN EXCEL (report for user)
                        // =====================================================
                        try {
                                Map<String, Integer> dupMap = findDuplicateMaGVs(invigilators);
                                if (!dupMap.isEmpty()) {
                                        System.out.println("Found duplicate MA_GV entries: " + dupMap.size());
                                        String dupFile = inputDir + File.separator + "duplicate_maGV_" + TIMESTAMP_FORMATTER.format(java.time.LocalDateTime.now()) + ".log";
                                        try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.BufferedWriter(new java.io.FileWriter(dupFile, false)))) {
                                                pw.println("MA_GV,COUNT");
                                                int c = 0;
                                                for (Map.Entry<String, Integer> e : dupMap.entrySet()) {
                                                        pw.println(e.getKey() + "," + e.getValue());
                                                        if (c++ < 50) {
                                                                System.out.println("Duplicate MA_GV: " + e.getKey() + " -> " + e.getValue());
                                                        }
                                                }
                                                pw.flush();
                                                System.out.println("Duplicate MA_GV report written to: " + dupFile);
                                        } catch (IOException ioe) {
                                                System.err.println("Failed to write duplicate MA_GV file: " + ioe.getMessage());
                                        }
                                } else {
                                        System.out.println("No duplicate MA_GV found in uploaded Excel.");
                                }
                        } catch (Exception ex) {
                                System.err.println("Error while checking duplicate MA_GV: " + ex.getMessage());
                        }

                                // =====================================================
                                // CHECK DUPLICATE TT IN EXCEL (report for user)
                                // =====================================================
                                try {
                                        Map<Integer, Integer> dupTtMap = findDuplicateTts(invigilators);
                                        if (!dupTtMap.isEmpty()) {
                                                System.out.println("Found duplicate TT entries: " + dupTtMap.size());
                                                String dupFile = inputDir + File.separator + "duplicate_tt_" + TIMESTAMP_FORMATTER.format(java.time.LocalDateTime.now()) + ".log";
                                                try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.BufferedWriter(new java.io.FileWriter(dupFile, false)))) {
                                                        pw.println("TT,COUNT");
                                                        int c = 0;
                                                        for (Map.Entry<Integer, Integer> e : dupTtMap.entrySet()) {
                                                                pw.println(e.getKey() + "," + e.getValue());
                                                                if (c++ < 50) {
                                                                        System.out.println("Duplicate TT: " + e.getKey() + " -> " + e.getValue());
                                                                }
                                                        }
                                                        pw.flush();
                                                        System.out.println("Duplicate TT report written to: " + dupFile);
                                                } catch (IOException ioe) {
                                                        System.err.println("Failed to write duplicate TT file: " + ioe.getMessage());
                                                }
                                        } else {
                                                System.out.println("No duplicate TT found in uploaded Excel.");
                                        }
                                } catch (Exception ex) {
                                        System.err.println("Error while checking duplicate TT: " + ex.getMessage());
                                }

            System.out.println(
                    "Excel data read - Invigilators: "
                            + invigilators.size()
                            + ", Rooms: "
                            + rooms.size()
            );

            // =====================================================
            // CLEAN DATA
            // =====================================================

            invigilators =
                    cleanAndDeduplicateInvigilators(
                            invigilators
                    );

            rooms =
                    cleanAndDeduplicateRooms(
                            rooms
                    );

            System.out.println(
                    "After deduplication - Invigilators: "
                            + invigilators.size()
                            + ", Rooms: "
                            + rooms.size()
            );

            // =====================================================
            // SAVE DATABASE
            // =====================================================

            long startTime =
                    System.currentTimeMillis();

            saveToDatabase(
                    invigilators,
                    rooms
            );

            long endTime =
                    System.currentTimeMillis();

            System.out.println(
                    "Database save completed in "
                            + (endTime - startTime)
                            + "ms"
            );

            dos.writeUTF("SUCCESS");

            dos.writeInt(invigilators.size());

            dos.writeInt(rooms.size());

            dos.flush();

            System.out.println(
                    "File loaded successfully"
            );

        } catch (Exception e) {

            dos.writeUTF(
                    "ERROR:" + e.getMessage()
            );

            dos.flush();

            e.printStackTrace();
        }
    }

    private List<Invigilator>
    cleanAndDeduplicateInvigilators(
            List<Invigilator> invigilators
    ) {

                Map<String, Invigilator> uniqueInvigilators = new LinkedHashMap<>();
                List<String> duplicates = new ArrayList<>();

                for (Invigilator inv : invigilators) {
                        String ma = inv.getMaGV();
                        if (ma == null || ma.trim().isEmpty()) {
                                System.out.println("NULL/EMPTY maGV -> " + inv.getHoTen());
                                continue;
                        }
                        ma = ma.trim();

                        if (uniqueInvigilators.containsKey(ma)) {
                                duplicates.add(ma);
                                System.out.println("⚠️ Duplicate maGV found and skipped: " + ma);
                        } else {
                                uniqueInvigilators.put(ma, inv);
                        }
                }

                if (!duplicates.isEmpty()) {
                        System.out.println("Total duplicate maGV removed: " + duplicates.size());
                }

                return new ArrayList<>(uniqueInvigilators.values());
    }

    private List<Room>
    cleanAndDeduplicateRooms(
            List<Room> rooms
    ) {

        Map<Integer, Room>
                uniqueRooms =
                new LinkedHashMap<>();

        List<Integer> duplicates =
                new ArrayList<>();

        for (Room room : rooms) {

            if (room.getStt() == null) {

    System.out.println(
        "NULL STT -> " +
        room.getPhongThi()
    );

    continue;
}

            int stt = room.getStt();

            if (stt <= 0) {

                System.out.println(
                        "⚠️ Invalid stt skipped: "
                                + stt
                );

                continue;
            }

            if (uniqueRooms.containsKey(stt)) {

                duplicates.add(stt);

                System.out.println(
                        "⚠️ Duplicate stt found and skipped: "
                                + stt
                );

            } else {

                uniqueRooms.put(stt, room);
            }
        }

        if (!duplicates.isEmpty()) {

            System.out.println(
                    "Total duplicate stt removed: "
                            + duplicates.size()
            );
        }

        return new ArrayList<>(
                uniqueRooms.values()
        );
    }

    private void saveToDatabase(
            List<Invigilator> invigilators,
            List<Room> rooms
    ) throws Exception {

        Connection conn =
                DatabaseConnection
                        .getInstance()
                        .getConnection();

        String currentStage = "INIT";

        try {

            conn.setAutoCommit(false);

            // =====================================================
            // DISABLE FK
            // =====================================================

            currentStage = "DISABLE_FOREIGN_KEY_CHECKS";

            try (
                    PreparedStatement pstmt =
                            conn.prepareStatement(
                                    "SET FOREIGN_KEY_CHECKS = 0"
                            )
            ) {

                pstmt.executeUpdate();

            } catch (SQLException e) {

                logSqlFailure(
                        currentStage,
                        "system",
                        "SET FOREIGN_KEY_CHECKS = 0",
                        null,
                        e
                );

                throw e;
            }

            // =====================================================
            // CLEAR OLD DATA
            // =====================================================

            executeDeleteWithLog(conn, "assignments");

            executeDeleteWithLog(conn, "supervisors");

            executeDeleteWithLog(conn, "pair_history");

            executeDeleteWithLog(conn, "room_history");

            executeDeleteWithLog(conn, "invigilators");

            executeDeleteWithLog(conn, "rooms");

            System.out.println(
                    "Database cleared successfully"
            );

            // =====================================================
            // INSERT INVIGILATORS
            // =====================================================

            currentStage = "INSERT_INVIGILATORS";

            String insertInvSql =
                    "INSERT INTO invigilators " +
                    "(tt, ma_gv, ho_ten, ngay_sinh, don_vi_cong_tac) " +
                    "VALUES (?, ?, ?, ?, ?)";

            try (
                    PreparedStatement pstmt =
                            conn.prepareStatement(
                                    insertInvSql
                            )
            ) {

                int count = 0;

                for (Invigilator inv : invigilators) {

                    if (inv.getTt() == null) {

                        continue;
                    }

                    pstmt.setInt(
                            1,
                            inv.getTt()
                    );

                    pstmt.setString(
                            2,
                            inv.getMaGV()
                    );

                    pstmt.setString(
                            3,
                            inv.getHoTen()
                    );

                    if (inv.getNgaySinh() != null) {

                        pstmt.setDate(
                                4,
                                new java.sql.Date(
                                        inv.getNgaySinh()
                                                .getTime()
                                )
                        );

                    } else {

                        pstmt.setNull(
                                4,
                                java.sql.Types.DATE
                        );
                    }

                    pstmt.setString(
                            5,
                            inv.getDonViCongTac()
                    );

                    try {

                        pstmt.executeUpdate();

                        count++;

                        if (count % 500 == 0) {

                            System.out.println(
                                    "Inserted "
                                            + count
                                            + " invigilators..."
                            );
                        }

                    } catch (SQLException e) {

                        logSqlFailure(
                                currentStage,
                                "invigilators",
                                insertInvSql,
                                buildInvigilatorPayload(inv),
                                e
                        );

                        throw e;
                    }
                }

                System.out.println(
                        "Inserted all invigilators"
                );

            } catch (SQLException e) {

                logSqlFailure(
                        currentStage,
                        "invigilators",
                        insertInvSql,
                        "batch execution",
                        e
                );

                throw e;
            }

            // =====================================================
            // INSERT ROOMS
            // =====================================================

            currentStage = "INSERT_ROOMS";

            String insertRoomSql =
                    "INSERT INTO rooms " +
                    "(stt, phong_thi, dia_diem) " +
                    "VALUES (?, ?, ?)";

            try (
                    PreparedStatement pstmt =
                            conn.prepareStatement(
                                    insertRoomSql
                            )
            ) {

                int count = 0;

                for (Room room : rooms) {

                    if (room.getStt() == null) {

                        continue;
                    }

                    pstmt.setInt(
                            1,
                            room.getStt()
                    );

                    pstmt.setString(
                            2,
                            room.getPhongThi()
                    );

                    pstmt.setString(
                            3,
                            room.getDiaDiem()
                    );

                    try {

                        pstmt.executeUpdate();

                        count++;

                        if (count % 500 == 0) {

                            System.out.println(
                                    "Inserted "
                                            + count
                                            + " rooms..."
                            );
                        }

                    } catch (SQLException e) {

                        logSqlFailure(
                                currentStage,
                                "rooms",
                                insertRoomSql,
                                buildRoomPayload(room),
                                e
                        );

                        throw e;
                    }
                }

                System.out.println(
                        "Inserted all rooms"
                );

            } catch (SQLException e) {

                logSqlFailure(
                        currentStage,
                        "rooms",
                        insertRoomSql,
                        "batch execution",
                        e
                );

                throw e;
            }

            // =====================================================
            // ENABLE FK
            // =====================================================

            currentStage = "ENABLE_FOREIGN_KEY_CHECKS";

            try (
                    PreparedStatement pstmt =
                            conn.prepareStatement(
                                    "SET FOREIGN_KEY_CHECKS = 1"
                            )
            ) {

                pstmt.executeUpdate();

            } catch (SQLException e) {

                logSqlFailure(
                        currentStage,
                        "system",
                        "SET FOREIGN_KEY_CHECKS = 1",
                        null,
                        e
                );

                throw e;
            }

            currentStage = "COMMIT_TRANSACTION";

            conn.commit();

            System.out.println(
                    "Transaction committed successfully"
            );

        } catch (Exception e) {

            System.err.println(
                    "[DB-ERROR] Stage failed: "
                            + currentStage
                            + " | message="
                            + e.getMessage()
            );

            conn.rollback();

            e.printStackTrace();

            throw e;

        } finally {

            try {

                conn.setAutoCommit(true);

            } catch (Exception e) {

                e.printStackTrace();
            }

            DatabaseConnection.closeConnection(conn);
        }
    }

    private void executeDeleteWithLog(Connection conn, String tableName) throws SQLException {

        String sql = "DELETE FROM " + tableName;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.executeUpdate();

            System.out.println("Cleared table: " + tableName);

        } catch (SQLException e) {

            logSqlFailure(
                    "CLEAR_TABLE",
                    tableName,
                    sql,
                    null,
                    e
            );

            throw e;
        }
    }

    private void logSqlFailure(
            String stage,
            String table,
            String sql,
            String payload,
            SQLException e
    ) {

        System.err.println("[DB-ERROR] stage=" + stage + " table=" + table);
        System.err.println("[DB-ERROR] sql=" + sql);

        if (payload != null && !payload.isEmpty()) {
            System.err.println("[DB-ERROR] payload=" + payload);
        }

        System.err.println("[DB-ERROR] sqlState=" + e.getSQLState());
        System.err.println("[DB-ERROR] errorCode=" + e.getErrorCode());
        System.err.println("[DB-ERROR] message=" + e.getMessage());
    }

    private String buildInvigilatorPayload(Invigilator invigilator) {
        return "tt=" + invigilator.getTt()
                                + ", ma_gv=" + maskCode(invigilator.getMaGV());
    }

        private String buildRoomPayload(Room room) {
                return "stt=" + room.getStt()
                                + ", phong_thi=" + room.getPhongThi();
        }

        private Map<String, Integer> findDuplicateMaGVs(List<Invigilator> invigilators) {
                Map<String, Integer> counts = new LinkedHashMap<>();
                for (Invigilator inv : invigilators) {
                        if (inv == null) continue;
                        String ma = inv.getMaGV();
                        if (ma == null) continue;
                        ma = ma.trim();
                        if (ma.isEmpty()) continue;
                        counts.put(ma, counts.getOrDefault(ma, 0) + 1);
                }

                Map<String, Integer> duplicates = new LinkedHashMap<>();
                for (Map.Entry<String, Integer> e : counts.entrySet()) {
                        if (e.getValue() > 1) duplicates.put(e.getKey(), e.getValue());
                }
                return duplicates;
        }

        private Map<Integer, Integer> findDuplicateTts(List<Invigilator> invigilators) {
                Map<Integer, Integer> counts = new LinkedHashMap<>();
                for (Invigilator inv : invigilators) {
                        if (inv == null) continue;
                        Integer tt = inv.getTt();
                        if (tt == null) continue;
                        counts.put(tt, counts.getOrDefault(tt, 0) + 1);
                }

                Map<Integer, Integer> duplicates = new LinkedHashMap<>();
                for (Map.Entry<Integer, Integer> e : counts.entrySet()) {
                        if (e.getValue() > 1) duplicates.put(e.getKey(), e.getValue());
                }
                return duplicates;
        }

        private String maskCode(String code) {
                if (code == null || code.isEmpty()) {
                        return "null";
                }

                if (code.length() <= 3) {
                        return "***";
                }

                return "***" + code.substring(code.length() - 3);
        }

        private void sendFileToClient(File file) throws IOException {
                dos.writeUTF(file.getName());
                dos.writeLong(file.length());
                dos.flush();

                try (FileInputStream fis = new FileInputStream(file)) {
                        byte[] buffer = new byte[8192];
                        int read;

                        while ((read = fis.read(buffer)) > 0) {
                                dos.write(buffer, 0, read);
                        }
                }

                dos.flush();
    }

    private void handleAssign() throws IOException {

        try {

                        // Keep protocol compatibility with current client request payload.
                        dis.readUTF();

            int numberOfRooms = dis.readInt();

            int numberOfInvigilators = dis.readInt();

            int shift = dis.readInt();

                System.out.println(
                        "Assigning shift " + shift
                );
            // Always use cleaned data already persisted by LOAD phase.
            List<Invigilator> invigilators =
                    new InvigilatorDAO().findAll();

            List<Room> rooms =
                    new RoomDAO().findAll();

            if (invigilators.isEmpty() || rooms.isEmpty()) {

                throw new IllegalArgumentException(
                        "Dữ liệu cán bộ/phòng thi trong database đang trống. Vui lòng gửi file trước."
                );
            }

            ScheduleInput input =
                    new ScheduleInput(
                            invigilators,
                            rooms,
                            numberOfRooms,
                            numberOfInvigilators,
                            shift
                    );

            System.out.println(
                    "Schedule input created"
            );
                        AssignmentBO assignmentBO = new AssignmentBO(msg -> {
                                try {
                                        if (dos != null) {
                                                dos.writeUTF("LOG:" + msg);
                                                dos.flush();
                                        }
                                } catch (IOException ioe) {
                                        System.err.println("Failed sending log to client: " + ioe.getMessage());
                                }
                        });

                        AssignmentResult result = assignmentBO.generateAssignments(input);

            String outputDir = createTimestampedDirectory("output");

            Map<String, Invigilator> invMap =
                    new HashMap<>();

            for (Invigilator inv : invigilators) {

                invMap.put(
                        inv.getMaGV(),
                        inv
                );
            }

            String assignmentFile =
                    outputDir +
                    File.separator +
                    "DANHSACHPHANCONG_Ca" + shift + ".xlsx";

            String supervisorFile =
                    outputDir +
                    File.separator +
                    "DANHSACHGIAMSAT_Ca" + shift + ".xlsx";

            ExcelWriter.writeAssignments(
                    assignmentFile,
                    result.getAssignments(),
                    invMap
            );

            ExcelWriter.writeSupervisors(
                    supervisorFile,
                    result.getSupervisors(),
                    invMap
            );

            dos.writeUTF("SUCCESS");
            dos.writeInt(result.getAssignments().size() * 2);
            dos.writeInt(result.getSupervisors().size());
            dos.writeUTF(new File(outputDir).getName());
            dos.writeInt(2);
            sendFileToClient(new File(assignmentFile));
            sendFileToClient(new File(supervisorFile));

            System.out.println(
                    "Assignment completed successfully"
            );

        } catch (IllegalArgumentException e) {

            dos.writeUTF(
                    "ERROR:" + e.getMessage()
            );

            dos.flush();

        } catch (Exception e) {

            dos.writeUTF(
                    "ERROR:Assignment failed - "
                            + e.getMessage()
            );

            dos.flush();

            e.printStackTrace();
        }
    }

        private String createTimestampedDirectory(String baseDir) {
                String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
                String sessionId = timestamp + "_" + UUID.randomUUID().toString().substring(0, 8);
                File directory = new File(baseDir, sessionId);
                if (!directory.exists()) {
                        directory.mkdirs();
                }
                return directory.getPath();
        }
}