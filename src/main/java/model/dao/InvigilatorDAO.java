package model.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import model.bean.Invigilator;
import util.DatabaseConnection;

public class InvigilatorDAO {

    private static final String INSERT_SQL =
            "INSERT INTO invigilators " +
            "(tt, ma_gv, ho_ten, ngay_sinh, don_vi_cong_tac) " +
            "VALUES (?, ?, ?, ?, ?)";

    private static final String UPDATE_SQL =
            "UPDATE invigilators " +
            "SET ma_gv = ?, ho_ten = ?, ngay_sinh = ?, don_vi_cong_tac = ? " +
            "WHERE tt = ?";

    private static final String DELETE_SQL =
            "DELETE FROM invigilators WHERE tt = ?";

    private static final String SELECT_BY_ID_SQL =
            "SELECT * FROM invigilators WHERE tt = ?";

        private static final String SELECT_BY_MA_GV_SQL =
            "SELECT * FROM invigilators WHERE ma_gv = ?";

    private static final String SELECT_ALL_SQL =
            "SELECT * FROM invigilators ORDER BY tt";

    public void insert(Invigilator invigilator) throws SQLException {

        Connection conn = DatabaseConnection.getInstance().getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(INSERT_SQL)) {

            pstmt.setInt(1, invigilator.getTt());

            pstmt.setString(2, invigilator.getMaGV());

            pstmt.setString(3, invigilator.getHoTen());

            if (invigilator.getNgaySinh() != null) {
                pstmt.setDate(
                        4,
                        new java.sql.Date(
                                invigilator.getNgaySinh().getTime()
                        )
                );
            } else {
                pstmt.setNull(4, Types.DATE);
            }

            pstmt.setString(5, invigilator.getDonViCongTac());

            pstmt.executeUpdate();

        } finally {
            DatabaseConnection.closeConnection(conn);
        }
    }

    public void update(Invigilator invigilator) throws SQLException {

        Connection conn = DatabaseConnection.getInstance().getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(UPDATE_SQL)) {

            pstmt.setString(1, invigilator.getMaGV());

            pstmt.setString(2, invigilator.getHoTen());

            if (invigilator.getNgaySinh() != null) {
                pstmt.setDate(
                        3,
                        new java.sql.Date(
                                invigilator.getNgaySinh().getTime()
                        )
                );
            } else {
                pstmt.setNull(3, Types.DATE);
            }

            pstmt.setString(4, invigilator.getDonViCongTac());

            pstmt.setInt(5, invigilator.getTt());

            pstmt.executeUpdate();

        } finally {
            DatabaseConnection.closeConnection(conn);
        }
    }

    public void delete(int tt) throws SQLException {

        Connection conn = DatabaseConnection.getInstance().getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(DELETE_SQL)) {

            pstmt.setInt(1, tt);

            pstmt.executeUpdate();

        } finally {
            DatabaseConnection.closeConnection(conn);
        }
    }

    public Invigilator findById(int tt) throws SQLException {

        Connection conn = DatabaseConnection.getInstance().getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(SELECT_BY_ID_SQL)) {

            pstmt.setInt(1, tt);

            try (ResultSet rs = pstmt.executeQuery()) {

                if (rs.next()) {
                    return mapResultSetToInvigilator(rs);
                }
            }

        } finally {
            DatabaseConnection.closeConnection(conn);
        }

        return null;
    }

    public Invigilator findByMaGV(String maGV) throws SQLException {

        Connection conn = DatabaseConnection.getInstance().getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(SELECT_BY_MA_GV_SQL)) {

            pstmt.setString(1, maGV);

            try (ResultSet rs = pstmt.executeQuery()) {

                if (rs.next()) {
                    return mapResultSetToInvigilator(rs);
                }
            }

        } finally {
            DatabaseConnection.closeConnection(conn);
        }

        return null;
    }

    public List<Invigilator> findAll() throws SQLException {

        Connection conn = DatabaseConnection.getInstance().getConnection();

        List<Invigilator> invigilators = new ArrayList<>();

        try (
                PreparedStatement pstmt = conn.prepareStatement(SELECT_ALL_SQL);
                ResultSet rs = pstmt.executeQuery()
        ) {

            while (rs.next()) {
                invigilators.add(mapResultSetToInvigilator(rs));
            }

        } finally {
            DatabaseConnection.closeConnection(conn);
        }

        return invigilators;
    }

    public void deleteAll() throws SQLException {

        Connection conn = DatabaseConnection.getInstance().getConnection();

        try (Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("DELETE FROM invigilators");

        } finally {
            DatabaseConnection.closeConnection(conn);
        }
    }

    private Invigilator mapResultSetToInvigilator(ResultSet rs)
            throws SQLException {

        Invigilator invigilator = new Invigilator();

        invigilator.setTt(rs.getInt("tt"));

        invigilator.setMaGV(rs.getString("ma_gv"));

        invigilator.setHoTen(rs.getString("ho_ten"));

        Date ngaySinh = rs.getDate("ngay_sinh");

        if (ngaySinh != null) {
            invigilator.setNgaySinh(
                    new java.util.Date(ngaySinh.getTime())
            );
        }

        invigilator.setDonViCongTac(
                rs.getString("don_vi_cong_tac")
        );

        return invigilator;
    }
}