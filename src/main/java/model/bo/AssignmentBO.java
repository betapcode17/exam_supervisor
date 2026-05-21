package model.bo;

import java.sql.SQLException;
import java.util.ArrayList;
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
import model.dao.PairHistoryDAO;
import model.dao.RoomHistoryDAO;
import model.dao.SupervisorDAO;

public class AssignmentBO {
    private AssignmentDAO assignmentDAO;
    private SupervisorDAO supervisorDAO;
    private PairHistoryDAO pairHistoryDAO;
    private RoomHistoryDAO roomHistoryDAO;

    private Consumer<String> logger;

    public AssignmentBO() {
        this(null);
    }

    public AssignmentBO(Consumer<String> logger) {
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

            Integer countABoxed = maA == null || maA.isEmpty() ? null : maCounts.get(maA);
            Integer countBBoxed = maB == null || maB.isEmpty() ? null : maCounts.get(maB);
            int countA = countABoxed == null ? Integer.MAX_VALUE : countABoxed;
            int countB = countBBoxed == null ? Integer.MAX_VALUE : countBBoxed;

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

        // Build result without persisting to DB yet (DB persist happens later in ClientHandler after sending to client)
        for (Assignment asg : tentativeAssignments) {
            log("[ASSIGN-FINAL] Room " + asg.getPhongThi() + " -> GT1=" + asg.getMaGV1() + ", GT2=" + asg.getMaGV2());
            assignments.add(asg);

            // Mark as used (by maGV) for later supervisor assignment
            if (asg.getMaGV1() != null) usedInvigilators.add(asg.getMaGV1());
            if (asg.getMaGV2() != null) usedInvigilators.add(asg.getMaGV2());
        }
        
        // Remaining invigilators become supervisors (without persisting supervisor data yet)
        log("[ASSIGN] Da xong phan coi thi, bat dau phan giamsat");
        supervisors.addAll(assignSupervisorsWithoutPersist(
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

    public void persistAssignmentResult(AssignmentResult result, ScheduleInput input) throws SQLException {
        int shift = input.getShift();
        
        // Persist assignments and pair/room histories in batch
        List<Assignment> assignments = result.getAssignments();
        List<PairHistory> toPersistPairs = new ArrayList<>();
        List<RoomHistory> toPersistRooms = new ArrayList<>();
        
        for (Assignment asg : assignments) {
            toPersistPairs.add(new PairHistory(asg.getMaGV1(), asg.getMaGV2(), shift));
            toPersistRooms.add(new RoomHistory(asg.getMaGV1(), asg.getPhongThi(), shift));
            toPersistRooms.add(new RoomHistory(asg.getMaGV2(), asg.getPhongThi(), shift));
        }
        
        try {
            assignmentDAO.insertBatch(assignments);
            pairHistoryDAO.insertBatch(toPersistPairs);
            roomHistoryDAO.insertBatch(toPersistRooms);
        } catch (SQLException e) {
            logInsertFailure("batch_persist", "shift=" + shift, e);
            throw e;
        }
        
        // Persist supervisors and their room histories
        List<Supervisor> supervisors = result.getSupervisors();
        List<RoomHistory> supervisorRoomHistories = new ArrayList<>();
        for (Supervisor sup : supervisors) {
            List<Room> roomsInRange = input.getRooms().stream()
                .filter(r -> r.getPhongThi().compareTo(sup.getFromRoom()) >= 0 
                          && r.getPhongThi().compareTo(sup.getToRoom()) <= 0)
                .collect(java.util.stream.Collectors.toList());
            for (Room r : roomsInRange) {
                supervisorRoomHistories.add(new RoomHistory(sup.getMaGV(), r.getPhongThi(), shift));
            }
        }
        
        try {
            if (!supervisors.isEmpty()) supervisorDAO.insertBatch(supervisors);
            if (!supervisorRoomHistories.isEmpty()) roomHistoryDAO.insertBatch(supervisorRoomHistories);
        } catch (SQLException e) {
            logInsertFailure("supervisor_batch_persist", "shift=" + shift, e);
            throw e;
        }
        
        log("[PERSIST] Assignment and supervisor data persisted to database");
    }

    private List<Supervisor> assignSupervisorsWithoutPersist(List<Invigilator> selectedInvigilators,
                                               Set<String> usedInvigilators,
                                               List<Room> selectedRooms,
                                               int shift) {
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
        List<RoomHistory> roomHistories = new ArrayList<>();
        try {
            roomHistories = roomHistoryDAO.findAll();
        } catch (SQLException e) {
            log("[SUPERVISOR] Warning: Could not load room histories - " + e.getMessage());
        }

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

        // Omit heavy debug file writes in production runs; log concise summary instead
        log("[SUPERVISOR-DEBUG] EXPECTED_REMAINING=" + expectedRemaining + ", AVAILABLE_SUPERVISORS=" + availableSupervisors.size());
        log("[SUPERVISOR-DEBUG] Missing count: " + difference.size());

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
        }

        // Note: DB persistence for supervisors happens later in persistAssignmentResult()
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

    private boolean hasBeenInRoom(List<RoomHistory> roomHistories, String maGV, String roomId) {
        for (RoomHistory rh : roomHistories) {
            if (rh.getMaGV() != null && rh.getPhongThi() != null
                    && rh.getMaGV().trim().equals(maGV)
                    && rh.getPhongThi().trim().equals(roomId)) {
                return true;
            }
        }
        return false;
    }

    private boolean backtrackAssignRooms(
            List<Room> rooms,
            List<Invigilator> availableInvigilators,
            Set<String> usedInvigilators,
            List<PairHistory> pairHistories,
            List<RoomHistory> roomHistories,
            List<Assignment> assignmentsOut,
            int shift
    ) {
        // Greedy assignment to avoid exponential backtracking on large inputs.
        // Build quick-lookup maps for histories and past pairings to get O(1) checks.
        Map<String, Set<String>> pairedBefore = new LinkedHashMap<>();
        for (PairHistory ph : pairHistories) {
            String a = ph.getMaGV1();
            String b = ph.getMaGV2();
            if (a == null || b == null) continue;
            a = a.trim(); b = b.trim();
            pairedBefore.computeIfAbsent(a, k -> new HashSet<>()).add(b);
            pairedBefore.computeIfAbsent(b, k -> new HashSet<>()).add(a);
        }

        Map<String, Set<String>> roomHistoryMap = new LinkedHashMap<>();
        for (RoomHistory rh : roomHistories) {
            String ma = rh.getMaGV();
            if (ma == null) continue;
            ma = ma.trim();
            roomHistoryMap.computeIfAbsent(ma, k -> new HashSet<>()).add(rh.getPhongThi());
        }

        // Use an index-based scan over a snapshot list to avoid ConcurrentModificationException.
        List<Invigilator> candidates = new ArrayList<>(availableInvigilators);
        Set<String> currentPairs = new HashSet<>();
        Set<String> currentRoomUsage = new HashSet<>();

        for (Room room : rooms) {
            String roomId = room.getPhongThi();
            boolean assigned = false;

            int n = candidates.size();
            for (int i = 0; i < n && !assigned; i++) {
                Invigilator inv1 = candidates.get(i);
                String ma1 = inv1 == null ? null : inv1.getMaGV();
                if (ma1 == null || ma1.trim().isEmpty()) continue;
                ma1 = ma1.trim();
                if (usedInvigilators.contains(ma1)) continue;
                Set<String> roomsSeen1 = roomHistoryMap.get(ma1);
                if (roomsSeen1 != null && roomsSeen1.contains(roomId)) continue;
                if (currentRoomUsage.contains(roomId + "|" + ma1)) continue;

                for (int j = i + 1; j < n; j++) {
                    Invigilator inv2 = candidates.get(j);
                    String ma2 = inv2 == null ? null : inv2.getMaGV();
                    if (ma2 == null || ma2.trim().isEmpty()) continue;
                    ma2 = ma2.trim();
                    if (usedInvigilators.contains(ma2)) continue;
                    Set<String> roomsSeen2 = roomHistoryMap.get(ma2);
                    if (roomsSeen2 != null && roomsSeen2.contains(roomId)) continue;
                    if (currentRoomUsage.contains(roomId + "|" + ma2)) continue;

                    Set<String> pairedWithA = pairedBefore.get(ma1);
                    if (pairedWithA != null && pairedWithA.contains(ma2)) continue;
                    if (currentPairs.contains(pairKey(ma1, ma2))) continue;

                    // Assign this pair and mark them as used (do not remove from list while iterating)
                    if (inv1 == null || inv2 == null) {
                        continue;
                    }
                    Assignment asg = new Assignment(shift, roomId, ma1, ma2, inv1.getTt(), inv2.getTt());
                    assignmentsOut.add(asg);
                    usedInvigilators.add(ma1);
                    usedInvigilators.add(ma2);
                    currentPairs.add(pairKey(ma1, ma2));
                    currentRoomUsage.add(roomId + "|" + ma1);
                    currentRoomUsage.add(roomId + "|" + ma2);
                    assigned = true;
                    break;
                }
            }

            if (!assigned) {
                // Failed to find a valid pair for this room using greedy strategy
                return false;
            }
        }

        return true;
    }

    private String pairKey(String left, String right) {
        if (left == null || right == null) {
            return String.valueOf(left) + "|" + String.valueOf(right);
        }
        if (left.compareTo(right) <= 0) {
            return left + "|" + right;
        }
        return right + "|" + left;
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

}
