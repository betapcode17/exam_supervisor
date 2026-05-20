package model.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import model.bean.Room;
import util.DatabaseConnection;

public class RoomDAO {

    private static final String INSERT_SQL =
            "INSERT INTO rooms " +
            "(stt, phong_thi, dia_diem) " +
            "VALUES (?, ?, ?)";

    private static final String UPDATE_SQL =
            "UPDATE rooms " +
            "SET phong_thi = ?, dia_diem = ? " +
            "WHERE stt = ?";

    private static final String DELETE_SQL =
            "DELETE FROM rooms WHERE stt = ?";

    private static final String SELECT_BY_ID_SQL =
            "SELECT * FROM rooms WHERE stt = ?";

        private static final String SELECT_BY_PHONG_THI_SQL =
            "SELECT * FROM rooms WHERE phong_thi = ?";

    private static final String SELECT_ALL_SQL =
            "SELECT * FROM rooms ORDER BY stt";

    public void insert(Room room) throws SQLException {

        Connection conn = DatabaseConnection.getInstance().getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(INSERT_SQL)) {

            pstmt.setInt(1, room.getStt());

            pstmt.setString(2, room.getPhongThi());

            pstmt.setString(3, room.getDiaDiem());

            pstmt.executeUpdate();

        } finally {
            DatabaseConnection.closeConnection(conn);
        }
    }

    public void update(Room room) throws SQLException {

        Connection conn = DatabaseConnection.getInstance().getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(UPDATE_SQL)) {

            pstmt.setString(1, room.getPhongThi());

            pstmt.setString(2, room.getDiaDiem());

            pstmt.setInt(3, room.getStt());

            pstmt.executeUpdate();

        } finally {
            DatabaseConnection.closeConnection(conn);
        }
    }

    public void delete(int stt) throws SQLException {

        Connection conn = DatabaseConnection.getInstance().getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(DELETE_SQL)) {

            pstmt.setInt(1, stt);

            pstmt.executeUpdate();

        } finally {
            DatabaseConnection.closeConnection(conn);
        }
    }

    public Room findById(int stt) throws SQLException {

        Connection conn = DatabaseConnection.getInstance().getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(SELECT_BY_ID_SQL)) {

            pstmt.setInt(1, stt);

            try (ResultSet rs = pstmt.executeQuery()) {

                if (rs.next()) {
                    return mapResultSetToRoom(rs);
                }
            }

        } finally {
            DatabaseConnection.closeConnection(conn);
        }

        return null;
    }

    public Room findByPhongThi(String phongThi) throws SQLException {

        Connection conn = DatabaseConnection.getInstance().getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(SELECT_BY_PHONG_THI_SQL)) {

            pstmt.setString(1, phongThi);

            try (ResultSet rs = pstmt.executeQuery()) {

                if (rs.next()) {
                    return mapResultSetToRoom(rs);
                }
            }

        } finally {
            DatabaseConnection.closeConnection(conn);
        }

        return null;
    }

    public List<Room> findAll() throws SQLException {

        Connection conn = DatabaseConnection.getInstance().getConnection();

        List<Room> rooms = new ArrayList<>();

        try (
                PreparedStatement pstmt = conn.prepareStatement(SELECT_ALL_SQL);
                ResultSet rs = pstmt.executeQuery()
        ) {

            while (rs.next()) {
                rooms.add(mapResultSetToRoom(rs));
            }

        } finally {
            DatabaseConnection.closeConnection(conn);
        }

        return rooms;
    }

    public void deleteAll() throws SQLException {

        Connection conn = DatabaseConnection.getInstance().getConnection();

        try (Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("DELETE FROM rooms");

        } finally {
            DatabaseConnection.closeConnection(conn);
        }
    }

    private Room mapResultSetToRoom(ResultSet rs)
            throws SQLException {

        Room room = new Room();

        room.setStt(rs.getInt("stt"));

        room.setPhongThi(rs.getString("phong_thi"));

        room.setDiaDiem(rs.getString("dia_diem"));

        return room;
    }
}