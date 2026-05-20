package model.bo;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import model.bean.Assignment;
import model.bean.AssignmentResult;
import model.bean.Invigilator;
import model.bean.PairHistory;
import model.bean.Room;
import model.bean.RoomHistory;
import model.bean.ScheduleInput;
import model.bean.Supervisor;
import model.dao.AssignmentDAO;
import model.dao.InvigilatorDAO;
import model.dao.PairHistoryDAO;
import model.dao.RoomDAO;
import model.dao.RoomHistoryDAO;
import model.dao.SupervisorDAO;

public class AssignmentBO {
    private InvigilatorDAO invigilatorDAO;
    private RoomDAO roomDAO;
    private AssignmentDAO assignmentDAO;
    private SupervisorDAO supervisorDAO;
    private PairHistoryDAO pairHistoryDAO;
    private RoomHistoryDAO roomHistoryDAO;

    private Consumer<String> logger;

    public AssignmentBO() {
        this(null);
    }

    public AssignmentBO(Consumer<String> logger) {
        this.invigilatorDAO = new InvigilatorDAO();
        this.roomDAO = new RoomDAO();
        this.assignmentDAO = new AssignmentDAO();
        this.supervisorDAO = new SupervisorDAO();
        this.pairHistoryDAO = new PairHistoryDAO();
        this.roomHistoryDAO = new RoomHistoryDAO();
        this.logger = logger;
    }

    private void log(String msg) {
        if (this.logger != null) {
            try {
                this.logger.accept(msg);
            } catch (Exception e) {
                System.out.println("[LOG-STREAM-ERR] " + e.getMessage());
            }
        } else {
            System.out.println(msg);
        }
    }

    public AssignmentResult generateAssignments(ScheduleInput input) throws SQLException {
        List<Assignment> assignments = new ArrayList<>();
        List<Supervisor> supervisors = new ArrayList<>();
        
        // Validate input
        validateInput(input);

        log("[ASSIGN] Bat dau phan cong ca " + input.getShift());
        log("[ASSIGN] Tong can bo dau vao: " + input.getInvigilators().size());
        log("[ASSIGN] Tong phong dau vao: " + input.getRooms().size());
        log("[ASSIGN] So phong can dung: " + input.getNumberOfRooms());
        log("[ASSIGN] So can bo can dung: " + input.getNumberOfInvigilators());
        
        // Shuffle data
        List<Invigilator> shuffledInvigilators = new ArrayList<>(input.getInvigilators());
        List<Room> shuffledRooms = new ArrayList<>(input.getRooms());
        
        Collections.shuffle(shuffledInvigilators);
        Collections.shuffle(shuffledRooms);
        log("[DEBUG] Total invigilators after shuffle: " + shuffledInvigilators.size());
        
        // Select invigilators and rooms
        // Prioritize invigilators whose maGV is unique (not duplicated across rows),
        // then sort by maGV for stable ordering. This ensures we prefer non-duplicate maGVs.
        Map<String, Integer> maCounts = new LinkedHashMap<>();
        for (Invigilator inv : shuffledInvigilators) {
            String code = inv.getMaGV();
            if (code != null) code = code.trim();
            if (code == null || code.isEmpty()) continue;
            maCounts.put(code, maCounts.getOrDefault(code, 0) + 1);
        }

        shuffledInvigilators.sort((a, b) -> {
            String maA = a == null ? null : a.getMaGV();
            String maB = b == null ? null : b.getMaGV();
            if (maA != null) maA = maA.trim();
            if (maB != null) maB = maB.trim();

            int countA = maA == null || maA.isEmpty() ? Integer.MAX_VALUE : maCounts.getOrDefault(maA, Integer.MAX_VALUE);
            int countB = maB == null || maB.isEmpty() ? Integer.MAX_VALUE : maCounts.getOrDefault(maB, Integer.MAX_VALUE);

            // unique maGV (count==1) first
            if (countA != countB) return Integer.compare(countA, countB);

            // then sort by maGV lexicographically (nulls last)
            if (maA == null && maB == null) return 0;
            if (maA == null) return 1;
            if (maB == null) return -1;
            int cmp = maA.compareToIgnoreCase(maB);
            if (cmp != 0) return cmp;

            // fallback: by tt
            Integer ttA = a == null ? null : a.getTt();
            Integer ttB = b == null ? null : b.getTt();
            if (ttA == null && ttB == null) return 0;
            if (ttA == null) return 1;
            if (ttB == null) return -1;
            return Integer.compare(ttA, ttB);
        });

        List<Invigilator> selectedInvigilators = shuffledInvigilators.subList(0,
            Math.min(input.getNumberOfInvigilators(), shuffledInvigilators.size()));
        // Normalize and deduplicate by maGV to ensure we don't select the same person twice
        int originalSelectedCount = selectedInvigilators.size();
        Map<String, Invigilator> uniqueMapByMa = new LinkedHashMap<>();
        for (Invigilator inv : selectedInvigilators) {
            String code = inv.getMaGV();
            if (code != null) code = code.trim();
            if (code == null || code.isEmpty()) continue;
            uniqueMapByMa.putIfAbsent(code, inv);
        }
        selectedInvigilators = new ArrayList<>(uniqueMapByMa.values());
        if (originalSelectedCount != selectedInvigilators.size()) {
            log("[DEBUG] Selected unique invigilators (by maGV): " + selectedInvigilators.size() +
                " (duplicates removed: " + (originalSelectedCount - selectedInvigilators.size()) + ")");
        }
        List<Room> selectedRooms = shuffledRooms.subList(0, 
            Math.min(input.getNumberOfRooms(), shuffledRooms.size()));
        log("[DEBUG] Selected invigilators count: " + selectedInvigilators.size());
        log("[DEBUG] Selected rooms count: " + selectedRooms.size());

        selectedRooms = new ArrayList<>(selectedRooms);
        selectedRooms.sort(Comparator.comparing(Room::getPhongThi, this::compareRoomCodes));
        
        // Check if we have enough invigilators (count distinct rows by tt)
        if (selectedInvigilators.size() < selectedRooms.size() * 2) {
            throw new IllegalArgumentException("Không đủ cán bộ để coi thi. Cần tối thiểu " + 
                (selectedRooms.size() * 2) + " cán bộ, hiện có " + selectedInvigilators.size());
        }
        
        // Load existing histories
        List<PairHistory> pairHistories = pairHistoryDAO.findAll();
        List<RoomHistory> roomHistories = roomHistoryDAO.findAll();
        
        // Assign invigilators to rooms using backtracking to honor history constraints
        // track used invigilators by `maGV` during assignment
        Set<String> usedInvigilators = new HashSet<>();
        int shift = input.getShift();

        // We'll perform assignment in-memory first (no DB writes) so we can backtrack cleanly.
        List<Assignment> tentativeAssignments = new ArrayList<>();

        boolean success = backtrackAssignRooms(
                0,
                selectedRooms,
                selectedInvigilators,
            usedInvigilators,
                pairHistories,
                roomHistories,
                tentativeAssignments,
                shift
        );

        if (!success) {
            throw new IllegalArgumentException("Không thể tìm phương án phân công thỏa mãn ràng buộc cho tất cả phòng");
        }

        // Persist tentative assignments and histories into DB
        for (Assignment asg : tentativeAssignments) {
            log("[ASSIGN-FINAL] Room " + asg.getPhongThi() + " -> GT1=" + asg.getMaGV1() + ", GT2=" + asg.getMaGV2());
            assignments.add(asg);

            try {
                assignmentDAO.insert(asg);
            } catch (SQLException e) {
                logInsertFailure("assignments",
                        "shift=" + shift + ", phong_thi=" + asg.getPhongThi() +
                                ", ma_gv_1=" + maskCode(asg.getMaGV1()) +
                                ", ma_gv_2=" + maskCode(asg.getMaGV2()), e);
                throw e;
            }

            // Persist pair history and room histories
            try {
                pairHistoryDAO.insert(new PairHistory(asg.getMaGV1(), asg.getMaGV2(), shift));
            } catch (SQLException e) {
                logInsertFailure("pair_history",
                        "ma_gv1=" + maskCode(asg.getMaGV1()) +
                                ", ma_gv2=" + maskCode(asg.getMaGV2()) +
                                ", shift=" + shift, e);
                throw e;
            }

            try {
                roomHistoryDAO.insert(new RoomHistory(asg.getMaGV1(), asg.getPhongThi(), shift));
                roomHistoryDAO.insert(new RoomHistory(asg.getMaGV2(), asg.getPhongThi(), shift));
            } catch (SQLException e) {
                logInsertFailure("room_history",
                        "ma_gv=... , phong_thi=" + asg.getPhongThi() + ", shift=" + shift, e);
                throw e;
            }

            // Mark as used (by maGV) for later supervisor assignment
            if (asg.getMaGV1() != null) usedInvigilators.add(asg.getMaGV1());
            if (asg.getMaGV2() != null) usedInvigilators.add(asg.getMaGV2());
        }
        
        // Remaining invigilators become supervisors
        log("[ASSIGN] Da xong phan coi thi, bat dau phan giamsat");
        supervisors.addAll(assignSupervisors(
            selectedInvigilators,
            usedInvigilators,
            selectedRooms,
            shift
        ));

        log("[ASSIGN] Ket thuc phan cong. So lop phan cong: " + assignments.size());
        log("[ASSIGN] So giang vien ben giamsat: " + supervisors.size());
        log("[ASSIGN] So giang vien ben giam thi: " + (assignments.size() * 2));
        
        return new AssignmentResult(assignments, supervisors);
    }

    private List<Invigilator> findValidPair(List<Invigilator> availableInvigilators,
                                           Set<String> usedInvigilators,
                                           List<PairHistory> pairHistories,
                                           List<RoomHistory> roomHistories,
                                           String roomId, Integer shift) {
        for (int i = 0; i < availableInvigilators.size(); i++) {
            Invigilator inv1 = availableInvigilators.get(i);
            String ma1 = inv1.getMaGV();
            if (ma1 == null || ma1.trim().isEmpty()) continue;
            ma1 = ma1.trim();
            if (usedInvigilators.contains(ma1)) continue;

            // Check if inv1 has been in this room before (history keyed by maGV)
            if (hasBeenInRoom(roomHistories, ma1, roomId)) continue;

            for (int j = i + 1; j < availableInvigilators.size(); j++) {
                Invigilator inv2 = availableInvigilators.get(j);
                String ma2 = inv2.getMaGV();
                if (ma2 == null || ma2.trim().isEmpty()) continue;
                ma2 = ma2.trim();
                if (usedInvigilators.contains(ma2)) continue;

                // Check if inv2 has been in this room before
                if (hasBeenInRoom(roomHistories, ma2, roomId)) continue;

                // Check if they've been paired before (history keyed by maGV)
                if (!hasPairedBefore(pairHistories, ma1, ma2)) {
                    return Arrays.asList(inv1, inv2);
                }
            }
        }

        return null;
    }

    private boolean hasBeenInRoom(List<RoomHistory> roomHistories, String maGV, String roomId) {
        for (RoomHistory rh : roomHistories) {
            if (rh.getMaGV().equals(maGV) && rh.getPhongThi().equals(roomId)) {
                return true;
            }
        }
        return false;
    }

    private List<Supervisor> assignSupervisors(List<Invigilator> selectedInvigilators,
                                               Set<String> usedInvigilators,
                                               List<Room> selectedRooms,
                                               int shift) throws SQLException {
        log("[SUPERVISOR] Bat dau phan giamsat cho " + selectedRooms.size() + " phong");
        List<Invigilator> availableSupervisors = new ArrayList<>();
        for (Invigilator invigilator : selectedInvigilators) {
            String ma = invigilator.getMaGV();
            if (ma == null || ma.trim().isEmpty()) continue;
            ma = ma.trim();
            if (!usedInvigilators.contains(ma)) {
                availableSupervisors.add(invigilator);
            }
        }

        // Load room histories to classify exclusions
        List<RoomHistory> roomHistories = roomHistoryDAO.findAll();

        // Use unique selected count (by maGV) when computing expected remaining supervisors
        Set<String> selectedUniqueSet = new HashSet<>();
        for (Invigilator inv : selectedInvigilators) {
            String ma = inv.getMaGV();
            if (ma != null) selectedUniqueSet.add(ma.trim());
        }
        int expectedRemaining = selectedUniqueSet.size() - usedInvigilators.size();
        log("[SUPERVISOR] So can bo con lai co the giamsat (computed): " + availableSupervisors.size());
        log("[SUPERVISOR] So can bo con lai theo phep tinh: " + expectedRemaining);
        if (availableSupervisors.size() != expectedRemaining) {
            log("[SUPERVISOR-DEBUG] Mismatch detected: listing differences...");

            Set<String> expectedSet = new HashSet<>(selectedUniqueSet);
            // remove used by maGV
            for (String used : usedInvigilators) expectedSet.remove(used);

            Set<String> actualSet = new HashSet<>();
            for (Invigilator inv : availableSupervisors) {
                String ma = inv.getMaGV();
                if (ma != null) actualSet.add(ma.trim());
            }

            // compute missing by maGV
            Set<String> missing = new HashSet<>(expectedSet);
            missing.removeAll(actualSet);

            log("[SUPERVISOR-DEBUG] Missing count: " + missing.size());
            int c = 0;
            for (String m : missing) {
                log("[SUPERVISOR-DEBUG] Missing maGV: " + m);
                if (++c >= 20) break;
            }
        }

        // Further classification for debugging: why some selected invigilators are not in availableSupervisors
        Map<String, List<String>> reasonMap = new LinkedHashMap<>();
        reasonMap.put("USED_FOR_INVIGILATION", new ArrayList<>());
        reasonMap.put("ROOM_HISTORY_HIT", new ArrayList<>());
        reasonMap.put("OTHER", new ArrayList<>());

        Set<String> selectedMaSet = new HashSet<>(selectedUniqueSet);
        Set<String> availMaSet = new HashSet<>();
        for (Invigilator inv : availableSupervisors) if (inv.getMaGV() != null) availMaSet.add(inv.getMaGV().trim());

        Set<String> difference = new HashSet<>(selectedMaSet);
        difference.removeAll(availMaSet);

        for (String ma : difference) {
            if (usedInvigilators.contains(ma)) {
                reasonMap.get("USED_FOR_INVIGILATION").add(ma);
                continue;
            }

            boolean roomHit = false;
            for (Room r : selectedRooms) {
                if (hasBeenInRoom(roomHistories, ma, r.getPhongThi())) {
                    roomHit = true;
                    break;
                }
            }

            if (roomHit) {
                reasonMap.get("ROOM_HISTORY_HIT").add(ma);
            } else {
                reasonMap.get("OTHER").add(ma);
            }
        }

        for (Map.Entry<String, List<String>> e : reasonMap.entrySet()) {
            log("[SUPERVISOR-DEBUG-REASON] " + e.getKey() + " -> count=" + e.getValue().size());
            int i = 0;
            for (String s : e.getValue()) {
                log("[SUPERVISOR-DEBUG-REASON] " + e.getKey() + ": " + s);
                if (++i >= 20) break;
            }
        }

        // Also write full debug output to a timestamped file for later inspection
        String debugFile = "supervisor_debug_" + System.currentTimeMillis() + ".log";
        try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(debugFile, false)))) {
            pw.println("EXPECTED_REMAINING=" + expectedRemaining);
            pw.println("AVAILABLE_SUPERVISORS=" + availableSupervisors.size());
            pw.println();
            pw.println("--- MISSING IDS (maGV) ---");
            for (String ma : new java.util.ArrayList<>(difference)) {
                if (ma == null) ma = "UNKNOWN";
                pw.println(ma);
            }
            pw.println();
            for (Map.Entry<String, List<String>> e2 : reasonMap.entrySet()) {
                pw.println("--- " + e2.getKey() + " (count=" + e2.getValue().size() + ") ---");
                for (String id : e2.getValue()) {
                    pw.println(id);
                }
                pw.println();
            }
            pw.flush();
            log("[SUPERVISOR-DEBUG] Written full debug to: " + debugFile);
        } catch (IOException ioe) {
            log("[SUPERVISOR-DEBUG] Failed writing debug file: " + ioe.getMessage());
        }

        if (availableSupervisors.isEmpty() || selectedRooms.isEmpty()) {
            return Collections.emptyList();
        }

        List<List<Room>> roomBlocks = splitRoomsEvenly(selectedRooms, availableSupervisors.size());
        log("[SUPERVISOR] So cum phong duoc tao: " + roomBlocks.size());

        if (roomBlocks.isEmpty()) {
            return Collections.emptyList();
        }

        List<Supervisor> assignedSupervisors = new ArrayList<>();
        // Iterate over a fixed snapshot to avoid modifying the list while iterating
        int supCount = availableSupervisors.size();
        for (int i = 0; i < supCount; i++) {
            Invigilator supervisorInvigilator = availableSupervisors.get(i);
            List<Room> roomBlock = roomBlocks.get(i % roomBlocks.size());

            String fromRoom = roomBlock.get(0).getPhongThi();
            String toRoom = roomBlock.get(roomBlock.size() - 1).getPhongThi();
            log("[SUPERVISOR] Gan " + supervisorInvigilator.getMaGV() + " cho day phong " + fromRoom + " -> " + toRoom);
            Supervisor supervisor = new Supervisor(shift, supervisorInvigilator.getMaGV(), fromRoom, toRoom);
            assignedSupervisors.add(supervisor);

            try {
                supervisorDAO.insert(supervisor);
            } catch (SQLException e) {
                logInsertFailure("supervisors",
                        "shift=" + shift + ", ma_gv=" + maskCode(supervisorInvigilator.getMaGV()) +
                                ", from_room=" + fromRoom + ", to_room=" + toRoom, e);
                throw e;
            }

            for (Room room : roomBlock) {
                try {
                    roomHistoryDAO.insert(new RoomHistory(supervisorInvigilator.getMaGV(), room.getPhongThi(), shift));
                } catch (SQLException e) {
                    logInsertFailure("room_history",
                            "ma_gv=" + maskCode(supervisorInvigilator.getMaGV()) +
                                    ", phong_thi=" + room.getPhongThi() +
                                    ", shift=" + shift, e);
                    throw e;
                }
            }
        }

        return assignedSupervisors;
    }

    private List<List<Room>> splitRoomsEvenly(List<Room> selectedRooms, int bucketCount) {
        List<List<Room>> buckets = new ArrayList<>();
        if (bucketCount <= 0 || selectedRooms.isEmpty()) {
            return buckets;
        }

        int roomCount = selectedRooms.size();
        int baseSize = roomCount / bucketCount;
        int remainder = roomCount % bucketCount;

        int index = 0;
        for (int i = 0; i < bucketCount; i++) {
            int currentSize = baseSize + (i < remainder ? 1 : 0);
            if (currentSize <= 0) {
                break;
            }

            List<Room> bucket = new ArrayList<>();
            for (int j = 0; j < currentSize && index < roomCount; j++) {
                bucket.add(selectedRooms.get(index++));
            }
            if (!bucket.isEmpty()) {
                buckets.add(bucket);
            }
        }

        return buckets;
    }

    private int compareRoomCodes(String left, String right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }

        int leftNumber = extractTrailingNumber(left);
        int rightNumber = extractTrailingNumber(right);

        if (leftNumber != rightNumber) {
            return Integer.compare(leftNumber, rightNumber);
        }

        return left.compareToIgnoreCase(right);
    }

    private int extractTrailingNumber(String value) {
        StringBuilder digits = new StringBuilder();
        for (int i = value.length() - 1; i >= 0; i--) {
            char ch = value.charAt(i);
            if (!Character.isDigit(ch)) {
                break;
            }
            digits.insert(0, ch);
        }

        if (digits.length() == 0) {
            return Integer.MAX_VALUE;
        }

        try {
            return Integer.parseInt(digits.toString());
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }

    private boolean hasPairedBefore(List<PairHistory> pairHistories, String maGV1, String maGV2) {
        for (PairHistory ph : pairHistories) {
            if ((ph.getMaGV1().equals(maGV1) && ph.getMaGV2().equals(maGV2)) ||
                (ph.getMaGV1().equals(maGV2) && ph.getMaGV2().equals(maGV1))) {
                return true;
            }
        }
        return false;
    }

    private boolean hasPairedInAssignments(List<Assignment> assignments, String ma1, String ma2) {
        for (Assignment a : assignments) {
            if ((a.getMaGV1().equals(ma1) && a.getMaGV2().equals(ma2)) ||
                (a.getMaGV1().equals(ma2) && a.getMaGV2().equals(ma1))) {
                return true;
            }
        }
        return false;
    }

    private boolean hasBeenInRoomAssignments(List<Assignment> assignments, String maGV, String roomId) {
        for (Assignment a : assignments) {
            if (a.getPhongThi().equals(roomId) && (a.getMaGV1().equals(maGV) || a.getMaGV2().equals(maGV))) {
                return true;
            }
        }
        return false;
    }

    private boolean backtrackAssignRooms(
            int idx,
            List<Room> rooms,
            List<Invigilator> availableInvigilators,
            Set<String> usedInvigilators,
            List<PairHistory> pairHistories,
            List<RoomHistory> roomHistories,
            List<Assignment> assignmentsOut,
            int shift
    ) {
        if (idx >= rooms.size()) {
            return true; // assigned all rooms
        }

        Room room = rooms.get(idx);
        log("[ASSIGN-BT] Trying room " + room.getPhongThi() + " (" + (idx+1) + "/" + rooms.size() + ")");

        int n = availableInvigilators.size();
        for (int i = 0; i < n; i++) {
            Invigilator inv1 = availableInvigilators.get(i);
            String ma1 = inv1.getMaGV();
            if (ma1 == null || ma1.trim().isEmpty()) continue;
            ma1 = ma1.trim();
            if (usedInvigilators.contains(ma1)) continue;
            if (hasBeenInRoom(roomHistories, ma1, room.getPhongThi())) continue;
            if (hasBeenInRoomAssignments(assignmentsOut, ma1, room.getPhongThi())) continue;

            for (int j = i + 1; j < n; j++) {
                Invigilator inv2 = availableInvigilators.get(j);
                String ma2 = inv2.getMaGV();
                if (ma2 == null || ma2.trim().isEmpty()) continue;
                ma2 = ma2.trim();
                if (usedInvigilators.contains(ma2)) continue;
                if (hasBeenInRoom(roomHistories, ma2, room.getPhongThi())) continue;
                if (hasBeenInRoomAssignments(assignmentsOut, ma2, room.getPhongThi())) continue;

                // check pair history (DB + tentative assignments) keyed by maGV
                if (hasPairedBefore(pairHistories, ma1, ma2)) continue;
                if (hasPairedInAssignments(assignmentsOut, ma1, ma2)) continue;

                // Tentatively assign (store both maGV and optionally tt)
                Assignment asg = new Assignment(shift, room.getPhongThi(), ma1, ma2, inv1.getTt(), inv2.getTt());
                log("[ASSIGN-BT] Tentative assign room " + room.getPhongThi() + " -> GT1=" + ma1 + ", GT2=" + ma2);
                assignmentsOut.add(asg);
                usedInvigilators.add(ma1);
                usedInvigilators.add(ma2);

                // Recurse
                boolean ok = backtrackAssignRooms(idx + 1, rooms, availableInvigilators, usedInvigilators, pairHistories, roomHistories, assignmentsOut, shift);
                if (ok) {
                    return true;
                }

                // Backtrack
                log("[ASSIGN-BT] Backtrack room " + room.getPhongThi() + " <- GT1=" + ma1 + ", GT2=" + ma2);
                assignmentsOut.remove(assignmentsOut.size() - 1);
                usedInvigilators.remove(ma1);
                usedInvigilators.remove(ma2);
            }
        }

        return false;
    }

    private void validateInput(ScheduleInput input) throws IllegalArgumentException {
        if (input == null) {
            throw new IllegalArgumentException("ScheduleInput không được null");
        }
        if (input.getInvigilators() == null || input.getInvigilators().isEmpty()) {
            throw new IllegalArgumentException("Danh sách cán bộ không được trống");
        }
        if (input.getRooms() == null || input.getRooms().isEmpty()) {
            throw new IllegalArgumentException("Danh sách phòng thi không được trống");
        }
        if (input.getNumberOfRooms() <= 0) {
            throw new IllegalArgumentException("Số phòng phải lớn hơn 0");
        }
        if (input.getNumberOfInvigilators() <= 0) {
            throw new IllegalArgumentException("Số cán bộ phải lớn hơn 0");
        }
    }

    public void rollback() throws SQLException {
        assignmentDAO.deleteAll();
        supervisorDAO.deleteAll();
        pairHistoryDAO.deleteAll();
        roomHistoryDAO.deleteAll();
    }

    private void logInsertFailure(String tableName, String payload, SQLException e) {
        String prefix = "[DB-ERROR] table=" + tableName + " insert failed";
        System.err.println(prefix);
        log(prefix);
        String p = "[DB-ERROR] payload=" + payload;
        System.err.println(p);
        log(p);
        String s = "[DB-ERROR] sqlState=" + e.getSQLState();
        System.err.println(s);
        log(s);
        String c = "[DB-ERROR] errorCode=" + e.getErrorCode();
        System.err.println(c);
        log(c);
        String m = "[DB-ERROR] message=" + e.getMessage();
        System.err.println(m);
        log(m);
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
}
