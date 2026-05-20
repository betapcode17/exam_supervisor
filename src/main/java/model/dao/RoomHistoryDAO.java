package model.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

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
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(INSERT_SQL)) {
            InvigilatorDAO invigilatorDAO = new InvigilatorDAO();
            RoomDAO roomDAO = new RoomDAO();
            Invigilator inv = invigilatorDAO.findByMaGV(roomHistory.getMaGV());
            Room room = roomDAO.findByPhongThi(roomHistory.getPhongThi());

            if (inv == null) {
                throw new SQLException("Không tìm thấy cán bộ: " + roomHistory.getMaGV());
            }
            if (room == null) {
                throw new SQLException("Không tìm thấy phòng thi: " + roomHistory.getPhongThi());
            }

            pstmt.setInt(1, inv.getTt());
            pstmt.setInt(2, room.getStt());
            pstmt.setInt(3, roomHistory.getShift());
            pstmt.executeUpdate();
        } finally {
            DatabaseConnection.closeConnection(conn);
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
