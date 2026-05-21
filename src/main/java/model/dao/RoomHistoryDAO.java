package model.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import model.bean.Invigilator;
import model.bean.Room;
import model.bean.RoomHistory;
import util.DatabaseConnection;

public class RoomHistoryDAO {
    private static final String INSERT_SQL = 
        "INSERT INTO room_history (gv_tt, room_stt, shift) VALUES (?, ?, ?)";
    private static final String SELECT_ALL_SQL = 
        "SELECT * FROM room_history";
    private static final String SELECT_BY_GV_ROOM_SQL = 
        "SELECT * FROM room_history WHERE gv_tt = ? AND room_stt = ?";
    private static final String DELETE_ALL_SQL = 
        "DELETE FROM room_history";

    public void insert(RoomHistory roomHistory) throws SQLException {
        List<RoomHistory> list = new ArrayList<>(1);
        list.add(roomHistory);
        insertBatch(list);
    }

    private final Map<String, Integer> maToTtCache = new HashMap<>();
    private final Map<String, Integer> roomToSttCache = new HashMap<>();

    private Integer getTtForMaGV(String maGV) throws SQLException {
        if (maGV == null) return null;
        maGV = maGV.trim();
        if (maToTtCache.containsKey(maGV)) return maToTtCache.get(maGV);
        InvigilatorDAO invigilatorDAO = new InvigilatorDAO();
        Invigilator inv = invigilatorDAO.findByMaGV(maGV);
        if (inv == null) return null;
        maToTtCache.put(maGV, inv.getTt());
        return inv.getTt();
    }

    private Integer getSttForRoom(String phongThi) throws SQLException {
        if (phongThi == null) return null;
        phongThi = phongThi.trim();
        if (roomToSttCache.containsKey(phongThi)) return roomToSttCache.get(phongThi);
        RoomDAO roomDAO = new RoomDAO();
        Room room = roomDAO.findByPhongThi(phongThi);
        if (room == null) return null;
        roomToSttCache.put(phongThi, room.getStt());
        return room.getStt();
    }

    public void insertBatch(List<RoomHistory> roomHistories) throws SQLException {
        if (roomHistories == null || roomHistories.isEmpty()) return;
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(INSERT_SQL)) {
            conn.setAutoCommit(false);
            for (RoomHistory rh : roomHistories) {
                Integer t = getTtForMaGV(rh.getMaGV());
                Integer s = getSttForRoom(rh.getPhongThi());
                if (t == null || s == null) {
                    conn.rollback();
                    throw new SQLException("Không tìm thấy cán bộ/phòng: " + rh.getMaGV() + ", " + rh.getPhongThi());
                }
                pstmt.setInt(1, t);
                pstmt.setInt(2, s);
                pstmt.setInt(3, rh.getShift());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
            conn.commit();
        } finally {
            DatabaseConnection.closeConnection(conn);
            maToTtCache.clear();
            roomToSttCache.clear();
        }
    }

    public List<RoomHistory> findAll() throws SQLException {
        Connection conn = DatabaseConnection.getInstance().getConnection();
        List<RoomHistory> roomHistories = new ArrayList<>();
        try (PreparedStatement pstmt = conn.prepareStatement(SELECT_ALL_SQL);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                roomHistories.add(mapResultSetToRoomHistory(rs));
            }
        } finally {
            DatabaseConnection.closeConnection(conn);
        }
        return roomHistories;
    }

    public List<RoomHistory> findByGVAndRoom(String maGV, String phongThi) throws SQLException {
        Connection conn = DatabaseConnection.getInstance().getConnection();
        List<RoomHistory> roomHistories = new ArrayList<>();
        try (PreparedStatement pstmt = conn.prepareStatement(SELECT_BY_GV_ROOM_SQL)) {
            InvigilatorDAO invigilatorDAO = new InvigilatorDAO();
            RoomDAO roomDAO = new RoomDAO();
            Invigilator inv = invigilatorDAO.findByMaGV(maGV);
            Room room = roomDAO.findByPhongThi(phongThi);

            if (inv == null || room == null) {
                return roomHistories;
            }

            pstmt.setInt(1, inv.getTt());
            pstmt.setInt(2, room.getStt());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    roomHistories.add(mapResultSetToRoomHistory(rs));
                }
            }
        } finally {
            DatabaseConnection.closeConnection(conn);
        }
        return roomHistories;
    }

    public void deleteAll() throws SQLException {
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(DELETE_ALL_SQL);
        } finally {
            DatabaseConnection.closeConnection(conn);
        }
    }

    private RoomHistory mapResultSetToRoomHistory(ResultSet rs) throws SQLException {
        InvigilatorDAO invigilatorDAO = new InvigilatorDAO();
        RoomDAO roomDAO = new RoomDAO();

        RoomHistory roomHistory = new RoomHistory();
        roomHistory.setId(rs.getInt("id"));
        Invigilator inv = invigilatorDAO.findById(rs.getInt("gv_tt"));
        Room room = roomDAO.findById(rs.getInt("room_stt"));
        roomHistory.setMaGV(inv != null ? inv.getMaGV() : null);
        roomHistory.setPhongThi(room != null ? room.getPhongThi() : null);
        roomHistory.setShift(rs.getInt("shift"));
        return roomHistory;
    }
}
