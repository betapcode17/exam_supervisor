package model.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import model.bean.Assignment;
import model.bean.Invigilator;
import model.bean.Room;
import util.DatabaseConnection;

public class AssignmentDAO {
    private static final String INSERT_SQL = 
        "INSERT INTO assignments (shift, room_stt, gv1_tt, gv2_tt) VALUES (?, ?, ?, ?)";
    private static final String UPDATE_SQL = 
        "UPDATE assignments SET shift = ?, room_stt = ?, gv1_tt = ?, gv2_tt = ? WHERE id = ?";
    private static final String DELETE_SQL = 
        "DELETE FROM assignments WHERE id = ?";
    private static final String SELECT_BY_ID_SQL = 
        "SELECT * FROM assignments WHERE id = ?";
    private static final String SELECT_ALL_SQL = 
        "SELECT * FROM assignments";
    private static final String SELECT_BY_SHIFT_SQL = 
        "SELECT * FROM assignments WHERE shift = ?";

    public void insert(Assignment assignment) throws SQLException {
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(INSERT_SQL)) {
            RoomDAO roomDAO = new RoomDAO();
            InvigilatorDAO invigilatorDAO = new InvigilatorDAO();

            pstmt.setInt(1, assignment.getShift());

            Room room = roomDAO.findByPhongThi(assignment.getPhongThi());
            Invigilator inv1 = invigilatorDAO.findByMaGV(assignment.getMaGV1());
            Invigilator inv2 = invigilatorDAO.findByMaGV(assignment.getMaGV2());

            if (room == null) {
                throw new SQLException("Không tìm thấy phòng thi: " + assignment.getPhongThi());
            }
            if (inv1 == null) {
                throw new SQLException("Không tìm thấy cán bộ: " + assignment.getMaGV1());
            }
            if (inv2 == null) {
                throw new SQLException("Không tìm thấy cán bộ: " + assignment.getMaGV2());
            }

            pstmt.setInt(2, room.getStt());
            pstmt.setInt(3, inv1.getTt());
            pstmt.setInt(4, inv2.getTt());
            pstmt.executeUpdate();
        } finally {
            DatabaseConnection.closeConnection(conn);
        }
    }

    public void insertBatch(List<Assignment> assignments) throws SQLException {
        if (assignments == null || assignments.isEmpty()) return;
        Connection conn = DatabaseConnection.getInstance().getConnection();
        String sql = INSERT_SQL;
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            // simple caches
            java.util.Map<String, Integer> maToTt = new java.util.HashMap<>();
            java.util.Map<String, Integer> roomToStt = new java.util.HashMap<>();

            for (Assignment assignment : assignments) {
                pstmt.setInt(1, assignment.getShift());

                String roomCode = assignment.getPhongThi();
                Integer roomStt = roomToStt.get(roomCode);
                if (roomStt == null) {
                    RoomDAO roomDAO = new RoomDAO();
                    Room r = roomDAO.findByPhongThi(roomCode);
                    if (r == null) {
                        conn.rollback();
                        throw new SQLException("Không tìm thấy phòng thi: " + roomCode);
                    }
                    roomStt = r.getStt();
                    roomToStt.put(roomCode, roomStt);
                }

                String ma1 = assignment.getMaGV1();
                Integer tt1 = maToTt.get(ma1);
                if (tt1 == null) {
                    InvigilatorDAO invDAO = new InvigilatorDAO();
                    Invigilator inv1 = invDAO.findByMaGV(ma1);
                    if (inv1 == null) { conn.rollback(); throw new SQLException("Không tìm thấy cán bộ: " + ma1); }
                    tt1 = inv1.getTt();
                    maToTt.put(ma1, tt1);
                }

                String ma2 = assignment.getMaGV2();
                Integer tt2 = maToTt.get(ma2);
                if (tt2 == null) {
                    InvigilatorDAO invDAO = new InvigilatorDAO();
                    Invigilator inv2 = invDAO.findByMaGV(ma2);
                    if (inv2 == null) { conn.rollback(); throw new SQLException("Không tìm thấy cán bộ: " + ma2); }
                    tt2 = inv2.getTt();
                    maToTt.put(ma2, tt2);
                }

                pstmt.setInt(2, roomStt);
                pstmt.setInt(3, tt1);
                pstmt.setInt(4, tt2);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
            conn.commit();
        } finally {
            DatabaseConnection.closeConnection(conn);
        }
    }

    public void update(Assignment assignment) throws SQLException {
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(UPDATE_SQL)) {
            RoomDAO roomDAO = new RoomDAO();
            InvigilatorDAO invigilatorDAO = new InvigilatorDAO();

            pstmt.setInt(1, assignment.getShift());

            Room room = roomDAO.findByPhongThi(assignment.getPhongThi());
            Invigilator inv1 = invigilatorDAO.findByMaGV(assignment.getMaGV1());
            Invigilator inv2 = invigilatorDAO.findByMaGV(assignment.getMaGV2());

            if (room == null) {
                throw new SQLException("Không tìm thấy phòng thi: " + assignment.getPhongThi());
            }
            if (inv1 == null) {
                throw new SQLException("Không tìm thấy cán bộ: " + assignment.getMaGV1());
            }
            if (inv2 == null) {
                throw new SQLException("Không tìm thấy cán bộ: " + assignment.getMaGV2());
            }

            pstmt.setInt(2, room.getStt());
            pstmt.setInt(3, inv1.getTt());
            pstmt.setInt(4, inv2.getTt());
            pstmt.setInt(5, assignment.getId());
            pstmt.executeUpdate();
        } finally {
            DatabaseConnection.closeConnection(conn);
        }
    }

    public void delete(Integer id) throws SQLException {
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(DELETE_SQL)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } finally {
            DatabaseConnection.closeConnection(conn);
        }
    }

    public Assignment findById(Integer id) throws SQLException {
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(SELECT_BY_ID_SQL)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToAssignment(rs);
                }
            }
        } finally {
            DatabaseConnection.closeConnection(conn);
        }
        return null;
    }

    public List<Assignment> findAll() throws SQLException {
        Connection conn = DatabaseConnection.getInstance().getConnection();
        List<Assignment> assignments = new ArrayList<>();
        try (PreparedStatement pstmt = conn.prepareStatement(SELECT_ALL_SQL);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                assignments.add(mapResultSetToAssignment(rs));
            }
        } finally {
            DatabaseConnection.closeConnection(conn);
        }
        return assignments;
    }

    public List<Assignment> findByShift(Integer shift) throws SQLException {
        Connection conn = DatabaseConnection.getInstance().getConnection();
        List<Assignment> assignments = new ArrayList<>();
        try (PreparedStatement pstmt = conn.prepareStatement(SELECT_BY_SHIFT_SQL)) {
            pstmt.setInt(1, shift);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    assignments.add(mapResultSetToAssignment(rs));
                }
            }
        } finally {
            DatabaseConnection.closeConnection(conn);
        }
        return assignments;
    }

    public void deleteAll() throws SQLException {
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM assignments");
        } finally {
            DatabaseConnection.closeConnection(conn);
        }
    }

    private Assignment mapResultSetToAssignment(ResultSet rs) throws SQLException {
        RoomDAO roomDAO = new RoomDAO();
        InvigilatorDAO invigilatorDAO = new InvigilatorDAO();

        Assignment assignment = new Assignment();
        assignment.setId(rs.getInt("id"));
        assignment.setShift(rs.getInt("shift"));

        Room room = roomDAO.findById(rs.getInt("room_stt"));
        Invigilator inv1 = invigilatorDAO.findById(rs.getInt("gv1_tt"));
        Invigilator inv2 = invigilatorDAO.findById(rs.getInt("gv2_tt"));

        assignment.setPhongThi(room != null ? room.getPhongThi() : null);
        assignment.setMaGV1(inv1 != null ? inv1.getMaGV() : null);
        assignment.setMaGV2(inv2 != null ? inv2.getMaGV() : null);
        return assignment;
    }
}
