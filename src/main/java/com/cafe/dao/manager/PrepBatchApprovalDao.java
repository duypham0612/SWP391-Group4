package com.cafe.dao.manager;

import com.cafe.dao.barista.PrepBatchDao;
import com.cafe.model.PrepBatch;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Duyệt mẻ pha sẵn — thao tác của QUẢN LÝ, không phải của barista.
 *
 * <p>Tách khỏi {@link PrepBatchDao} theo ranh giới QUYỀN HẠN chứ không phải theo chức năng: barista
 * tạo mẻ, quản lý duyệt. Gộp chung một file thì một thay đổi ở luật duyệt trông giống hệt một thay
 * đổi ở luồng tạo mẻ khi review, mà hai thứ đó có hậu quả rất khác nhau.
 *
 * <p>Hai đường duyệt KHÁC NHAU, đừng lẫn:
 * <ul>
 *   <li><b>Tiền kiểm</b> ({@link #findPendingApproval} → {@link #approve}/{@link #reject}) — mẻ bất
 *       thường bị CHẶN ở PENDING, chưa cộng tồn; duyệt xong mới thành ACTIVE.</li>
 *   <li><b>Hậu kiểm</b> ({@link #findUnreviewedActive} → {@link #markReviewed}) — mẻ thường đã ACTIVE
 *       và ĐÃ cộng tồn rồi; đóng dấu "đã xem" KHÔNG đụng tới kho.</li>
 * </ul>
 *
 * <p>Cả ba câu UPDATE đều nguyên tử: điều kiện trạng thái nằm ngay trong WHERE nên hai quản lý bấm
 * cùng lúc thì chỉ một người nhận được 1 dòng, người kia nhận 0 và caller báo xung đột.
 */
public class PrepBatchApprovalDao {

    /** Mẻ chờ duyệt vì bất thường — cũ nhất lên trước (barista chờ lâu nhất được xử lý trước). */
    public List<PrepBatch> findPendingApproval(Connection conn, int branchId) throws SQLException {
        List<PrepBatch> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                PrepBatchDao.SELECT + "WHERE pb.BranchId=? AND pb.Status='PENDING' ORDER BY pb.MadeAt ASC")) {
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(PrepBatchDao.map(rs)); }
        }
        return out;
    }

    /** Hàng đợi hậu kiểm KHÔNG chặn — mẻ thường Manager chưa "đã xem". Mới nhất trước. */
    public List<PrepBatch> findUnreviewedActive(Connection conn, int branchId) throws SQLException {
        List<PrepBatch> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                PrepBatchDao.SELECT + "WHERE pb.BranchId=? AND pb.Status='ACTIVE' AND pb.RequiresApproval=0 "
                       + "AND pb.ReviewedAt IS NULL ORDER BY pb.MadeAt DESC")) {
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(PrepBatchDao.map(rs)); }
        }
        return out;
    }

    /** Duyệt mẻ PENDING → ACTIVE. Atomic: UPDATE chỉ khớp đúng 1 dòng đang PENDING, tự làm row-lock. */
    public int approve(Connection conn, int prepBatchId, int branchId, int reviewerId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE inventory.PrepBatch SET Status='ACTIVE', ReviewedAt=SYSUTCDATETIME(), ReviewedBy=? "
                        + "WHERE PrepBatchId=? AND BranchId=? AND Status='PENDING'")) {
            ps.setInt(1, reviewerId);
            ps.setInt(2, prepBatchId);
            ps.setInt(3, branchId);
            return ps.executeUpdate();
        }
    }

    /** Từ chối mẻ PENDING → REJECTED. Atomic, cùng điều kiện với approve. */
    public int reject(Connection conn, int prepBatchId, int branchId, int reviewerId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE inventory.PrepBatch SET Status='REJECTED', ReviewedAt=SYSUTCDATETIME(), ReviewedBy=? "
                        + "WHERE PrepBatchId=? AND BranchId=? AND Status='PENDING'")) {
            ps.setInt(1, reviewerId);
            ps.setInt(2, prepBatchId);
            ps.setInt(3, branchId);
            return ps.executeUpdate();
        }
    }

    /** Hậu kiểm: đóng dấu "đã xem" cho mẻ ACTIVE thường — KHÔNG đổi tồn kho. */
    public int markReviewed(Connection conn, int prepBatchId, int branchId, int reviewerId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE inventory.PrepBatch SET ReviewedAt=SYSUTCDATETIME(), ReviewedBy=? "
                        + "WHERE PrepBatchId=? AND BranchId=? AND Status='ACTIVE' AND ReviewedAt IS NULL")) {
            ps.setInt(1, reviewerId);
            ps.setInt(2, prepBatchId);
            ps.setInt(3, branchId);
            return ps.executeUpdate();
        }
    }
}
