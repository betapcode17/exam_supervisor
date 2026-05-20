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
import model.bean.Supervisor;
import util.DatabaseConnection;

public class SupervisorDAO {
    private static final String INSERT_SQL = 
        "INSERT INTO supervisors (shift, gv_tt, from_room, to_room) VALUES (?, ?, ?, ?)";
    private static final String UPDATE_SQL = 
        "UPDATE supervisors SET shift = ?, gv_tt = ?, from_room = ?, to_room = ? WHERE id = ?";
    private static final String DELETE_SQL = 
        "DELETE FROM supervisors WHERE id = ?";
    private static final String SELECT_BY_ID_SQL = 
        "SELECT * FROM supervisors WHERE id = ?";
    private static final String SELECT_ALL_SQL = 
        "SELECT * FROM supervisors";
    private static final String SELECT_BY_SHIFT_SQL = 
        "SELECT * FROM supervisors WHERE shift = ?";

    public void insert(Supervisor supervisor) throws SQLException {
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(INSERT_SQL)) {
            InvigilatorDAO invigilatorDAO = new InvigilatorDAO();
            RoomDAO roomDAO = new RoomDAO();
            Invigilator inv = invigilatorDAO.findByMaGV(supervisor.getMaGV());
            Room fromRoom = roomDAO.findByPhongThi(supervisor.getFromRoom());
            Room toRoom = roomDAO.findByPhongThi(supervisor.getToRoom());

            if (inv == null) {
                throw new SQLException("Không tìm thấy cán bộ: " + supervisor.getMaGV());
            }
            if (fromRoom == null) {
                throw new SQLException("Không tìm thấy phòng nguồn: " + supervisor.getFromRoom());
            }
            if (toRoom == null) {
                throw new SQLException("Không tìm thấy phòng đích: " + supervisor.getToRoom());
            }

            pstmt.setInt(1, supervisor.getShift());
            pstmt.setInt(2, inv.getTt());
            pstmt.setInt(3, fromRoom.getStt());
            pstmt.setInt(4, toRoom.getStt());
            pstmt.executeUpdate();
        } finally {
            DatabaseConnection.closeConnection(conn);
        }
    }

    public void update(Supervisor supervisor) throws SQLException {
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(UPDATE_SQL)) {
            InvigilatorDAO invigilatorDAO = new InvigilatorDAO();
            RoomDAO roomDAO = new RoomDAO();
            Invigilator inv = invigilatorDAO.findByMaGV(supervisor.getMaGV());
            Room fromRoom = roomDAO.findByPhongThi(supervisor.getFromRoom());
            Room toRoom = roomDAO.findByPhongThi(supervisor.getToRoom());

            if (inv == null) {
                throw new SQLException("Không tìm thấy cán bộ: " + supervisor.getMaGV());
            }
            if (fromRoom == null) {
                throw new SQLException("Không tìm thấy phòng nguồn: " + supervisor.getFromRoom());
            }
            if (toRoom == null) {
                throw new SQLException("Không tìm thấy phòng đích: " + supervisor.getToRoom());
            }

            pstmt.setInt(1, supervisor.getShift());
            pstmt.setInt(2, inv.getTt());
            pstmt.setInt(3, fromRoom.getStt());
            pstmt.setInt(4, toRoom.getStt());
            pstmt.setInt(5, supervisor.getId());
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

    public Supervisor findById(Integer id) throws SQLException {
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(SELECT_BY_ID_SQL)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToSupervisor(rs);
                }
            }
        } finally {
            DatabaseConnection.closeConnection(conn);
        }
        return null;
    }

    public List<Supervisor> findAll() throws SQLException {
        Connection conn = DatabaseConnection.getInstance().getConnection();
        List<Supervisor> supervisors = new ArrayList<>();
        try (PreparedStatement pstmt = conn.prepareStatement(SELECT_ALL_SQL);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                supervisors.add(mapResultSetToSupervisor(rs));
            }
        } finally {
            DatabaseConnection.closeConnection(conn);
        }
        return supervisors;
    }

    public List<Supervisor> findByShift(Integer shift) throws SQLException {
        Connection conn = DatabaseConnection.getInstance().getConnection();
        List<Supervisor> supervisors = new ArrayList<>();
        try (PreparedStatement pstmt = conn.prepareStatement(SELECT_BY_SHIFT_SQL)) {
            pstmt.setInt(1, shift);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    supervisors.add(mapResultSetToSupervisor(rs));
                }
            }
        } finally {
            DatabaseConnection.closeConnection(conn);
        }
        return supervisors;
    }

    public void deleteAll() throws SQLException {
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM supervisors");
        } finally {
            DatabaseConnection.closeConnection(conn);
        }
    }

    private Supervisor mapResultSetToSupervisor(ResultSet rs) throws SQLException {
        InvigilatorDAO invigilatorDAO = new InvigilatorDAO();
        RoomDAO roomDAO = new RoomDAO();

        Supervisor supervisor = new Supervisor();
        supervisor.setId(rs.getInt("id"));
        supervisor.setShift(rs.getInt("shift"));
        Invigilator inv = invigilatorDAO.findById(rs.getInt("gv_tt"));
        Room fromRoom = roomDAO.findById(rs.getInt("from_room"));
        Room toRoom = roomDAO.findById(rs.getInt("to_room"));
        supervisor.setMaGV(inv != null ? inv.getMaGV() : null);
        supervisor.setFromRoom(fromRoom != null ? fromRoom.getPhongThi() : null);
        supervisor.setToRoom(toRoom != null ? toRoom.getPhongThi() : null);
        return supervisor;
    }
}
