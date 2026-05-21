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
import model.bean.PairHistory;
import util.DatabaseConnection;

public class PairHistoryDAO {
    private static final String INSERT_SQL = 
        "INSERT INTO pair_history (gv1_tt, gv2_tt, shift) VALUES (?, ?, ?)";
    private static final String SELECT_ALL_SQL = 
        "SELECT * FROM pair_history";
    private static final String SELECT_BY_PAIR_SQL = 
        "SELECT * FROM pair_history WHERE (gv1_tt = ? AND gv2_tt = ?) OR (gv1_tt = ? AND gv2_tt = ?)";
    private static final String DELETE_ALL_SQL = 
        "DELETE FROM pair_history";

    public void insert(PairHistory pairHistory) throws SQLException {
        // Delegate to batch path for single insert to reuse logic
        List<PairHistory> list = new ArrayList<>(1);
        list.add(pairHistory);
        insertBatch(list);
    }

    // Simple cache for maGV -> tt to avoid repeated DB lookups
    private final Map<String, Integer> maToTtCache = new HashMap<>();

    private Integer getTtForMaGV(Connection conn, String maGV) throws SQLException {
        if (maGV == null) return null;
        maGV = maGV.trim();
        if (maToTtCache.containsKey(maGV)) return maToTtCache.get(maGV);
        InvigilatorDAO invigilatorDAO = new InvigilatorDAO();
        Invigilator inv = invigilatorDAO.findByMaGV(maGV);
        if (inv == null) return null;
        maToTtCache.put(maGV, inv.getTt());
        return inv.getTt();
    }

    public void insertBatch(List<PairHistory> pairHistories) throws SQLException {
        if (pairHistories == null || pairHistories.isEmpty()) return;
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(INSERT_SQL)) {
            conn.setAutoCommit(false);
            for (PairHistory ph : pairHistories) {
                Integer t1 = getTtForMaGV(conn, ph.getMaGV1());
                Integer t2 = getTtForMaGV(conn, ph.getMaGV2());
                if (t1 == null || t2 == null) {
                    conn.rollback();
                    throw new SQLException("Không tìm thấy cán bộ cho pair: " + ph.getMaGV1() + ", " + ph.getMaGV2());
                }
                pstmt.setInt(1, t1);
                pstmt.setInt(2, t2);
                pstmt.setInt(3, ph.getShift());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
            conn.commit();
        } catch (SQLException ex) {
            throw ex;
        } finally {
            DatabaseConnection.closeConnection(conn);
            maToTtCache.clear();
        }
    }

    public List<PairHistory> findAll() throws SQLException {
        Connection conn = DatabaseConnection.getInstance().getConnection();
        List<PairHistory> pairHistories = new ArrayList<>();
        try (PreparedStatement pstmt = conn.prepareStatement(SELECT_ALL_SQL);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                pairHistories.add(mapResultSetToPairHistory(rs));
            }
        } finally {
            DatabaseConnection.closeConnection(conn);
        }
        return pairHistories;
    }

    public List<PairHistory> findByPair(String maGV1, String maGV2) throws SQLException {
        Connection conn = DatabaseConnection.getInstance().getConnection();
        List<PairHistory> pairHistories = new ArrayList<>();
        try (PreparedStatement pstmt = conn.prepareStatement(SELECT_BY_PAIR_SQL)) {
            InvigilatorDAO invigilatorDAO = new InvigilatorDAO();
            Invigilator inv1 = invigilatorDAO.findByMaGV(maGV1);
            Invigilator inv2 = invigilatorDAO.findByMaGV(maGV2);

            if (inv1 == null || inv2 == null) {
                return pairHistories;
            }

            pstmt.setInt(1, inv1.getTt());
            pstmt.setInt(2, inv2.getTt());
            pstmt.setInt(3, inv2.getTt());
            pstmt.setInt(4, inv1.getTt());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    pairHistories.add(mapResultSetToPairHistory(rs));
                }
            }
        } finally {
            DatabaseConnection.closeConnection(conn);
        }
        return pairHistories;
    }

    public void deleteAll() throws SQLException {
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(DELETE_ALL_SQL);
        } finally {
            DatabaseConnection.closeConnection(conn);
        }
    }

    private PairHistory mapResultSetToPairHistory(ResultSet rs) throws SQLException {
        InvigilatorDAO invigilatorDAO = new InvigilatorDAO();

        PairHistory pairHistory = new PairHistory();
        pairHistory.setId(rs.getInt("id"));
        Invigilator inv1 = invigilatorDAO.findById(rs.getInt("gv1_tt"));
        Invigilator inv2 = invigilatorDAO.findById(rs.getInt("gv2_tt"));
        pairHistory.setMaGV1(inv1 != null ? inv1.getMaGV() : null);
        pairHistory.setMaGV2(inv2 != null ? inv2.getMaGV() : null);
        pairHistory.setShift(rs.getInt("shift"));
        return pairHistory;
    }
}
