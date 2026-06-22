package com.mycoffee.dao;

import com.mycoffee.context.DBContext;
import com.mycoffee.model.Voucher;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class VoucherDAO extends DBContext {

    // --- CẬP NHẬT MỚI: KIỂM TRA TRÙNG MÃ CODE TRONG DATABASE ---
    public boolean checkExistCode(String code) {
        String sql = "SELECT COUNT(*) FROM [Vouchers] WHERE UPPER(LTRIM(RTRIM([VoucherCode]))) = ?";
        try (Connection conn = getConnection(); 
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, code.trim().toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0; // Trả về true nếu số lượng tìm thấy > 0 (tức là đã bị trùng)
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // --- CẬP NHẬT 1: TRUY VẤN PHÂN TRANG KẾT HỢP BỘ LỌC TRẠNG THÁI ---
    public List<Voucher> getVouchersWithPagination(String status, int offset, int size) {
        List<Voucher> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM [Vouchers] WHERE 1=1 ");

        // Append điều kiện lọc dựa theo biến status truyền từ Controller
        buildStatusFilterQuery(sql, status);

        // Sắp xếp và thực hiện Phân trang (Cú pháp chuẩn SQL Server)
        sql.append(" ORDER BY [VoucherID] DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");

        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            ps.setInt(1, offset);
            ps.setInt(2, size);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToVoucher(rs));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    // --- CẬP NHẬT 2: TRUY VẤN PHÂN TRANG KHI ĐANG TÌM KIẾM (REALTIME SEARCH + FILTER) ---
    public List<Voucher> searchVouchersWithPagination(String keyword, String status, int offset, int size) {
        List<Voucher> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM [Vouchers] WHERE [VoucherCode] LIKE ? ");

        // Vừa tìm kiếm vừa giữ nguyên bộ lọc tab trạng thái đang chọn
        buildStatusFilterQuery(sql, status);

        sql.append(" ORDER BY [VoucherID] DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");

        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            ps.setString(1, "%" + keyword.trim() + "%");
            ps.setInt(2, offset);
            ps.setInt(3, size);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToVoucher(rs));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    // --- HÀM TRỢ GIÚP 1: TỰ ĐỘNG BUILDS PHẦN WHERE CHO STATUS KHÁC NHAU ---
    private void buildStatusFilterQuery(StringBuilder sql, String status) {
        if (status == null) {
            return;
        }
        switch (status.toLowerCase()) {
            case "active":
                // Đang kích hoạt và đang trong giai đoạn thời gian áp dụng
                sql.append(" AND [IsActive] = 1 AND GETDATE() BETWEEN [StartDate] AND [EndDate] ");
                break;
            case "ended":
                // Đã quá hạn kết thúc, hoặc cấu trúc voucher đã bị tắt chủ động khi đang chạy hoặc quá hạn
                sql.append(" AND (GETDATE() > [EndDate] OR ([IsActive] = 0 AND [StartDate] <= GETDATE())) ");
                break;
            case "draft":
                // Được lưu dạng bản nháp tắt kích hoạt, hoặc mã bật kích hoạt nhưng ngày bắt đầu chưa tới (Sắp diễn ra)
                sql.append(" AND ([IsActive] = 0 OR GETDATE() < [StartDate]) ");
                break;
            case "all":
            default:
                // Không append thêm điều kiện, lấy toàn bộ bản ghi
                break;
        }
    }

    // --- HÀM TRỢ GIÚP 2: MAP DỮ LIỆU TỪ RESULTSET SANG OBJECT ĐỂ TRÁNH LẶP CODE ---
    private Voucher mapResultSetToVoucher(ResultSet rs) throws Exception {
        Voucher v = new Voucher();
        v.setVoucherID(rs.getInt("VoucherID"));
        v.setVoucherCode(rs.getString("VoucherCode"));
        v.setDiscountValue(rs.getDouble("DiscountValue"));
        v.setIsPercentage(rs.getBoolean("IsPercentage"));
        v.setMinOrderValue(rs.getDouble("MinOrderValue"));
        v.setStartDate(rs.getTimestamp("StartDate"));
        v.setEndDate(rs.getTimestamp("EndDate"));
        v.setIsActive(rs.getBoolean("IsActive"));
        return v;
    }

    // Lấy toàn bộ danh sách voucher (Giữ lại nếu các module cũ khác cần gọi)
    public List<Voucher> getAllVouchers() {
        List<Voucher> list = new ArrayList<>();
        String sql = "SELECT * FROM [Vouchers] ORDER BY [StartDate] DESC";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToVoucher(rs));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    // TÌM KIẾM VOUCHER THEO MÃ CODE (Giữ lại phục vụ các tính năng không phân trang)
    public List<Voucher> searchVouchers(String keyword) {
        List<Voucher> list = new ArrayList<>();
        String sql = "SELECT * FROM [Vouchers] WHERE [VoucherCode] LIKE ? ORDER BY [StartDate] DESC";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + keyword.trim() + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToVoucher(rs));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    // CHI TIẾT 1 VOUCHER THEO ID (Phục vụ gọi thông tin sửa đổi)
    public Voucher getVoucherById(int id) {
        String sql = "SELECT * FROM [Vouchers] WHERE [VoucherID] = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToVoucher(rs);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // Thống kê nhanh số lượng phục vụ 4 thẻ trên UI
    public double[] getVoucherStats() {
        double[] stats = new double[4];
        String sql = "SELECT "
                + "COUNT(DISTINCT CASE WHEN v.[IsActive] = 1 AND GETDATE() BETWEEN v.[StartDate] AND v.[EndDate] THEN v.[VoucherID] END) AS ActiveCount, "
                + "COUNT(DISTINCT CASE WHEN v.[IsActive] = 0 OR GETDATE() > v.[EndDate] THEN v.[VoucherID] END) AS EndedCount, "
                + "COUNT(o.[OrderID]) AS TotalUses, "
                + "ISNULL(AVG(o.[ROI]), 0) AS AvgROI "
                + "FROM [Vouchers] v "
                + "LEFT JOIN [Orders] o ON v.[VoucherCode] = o.[VoucherCode]";

        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                stats[0] = rs.getDouble("ActiveCount");
                stats[1] = rs.getDouble("EndedCount");
                stats[2] = rs.getDouble("TotalUses");
                stats[3] = rs.getDouble("AvgROI");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return stats;
    }

    // THÊM MỚI VOUCHER VÀO DATABASE
    public boolean addVoucher(Voucher voucher) {
        String sql = "INSERT INTO [Vouchers] ([VoucherCode], [DiscountValue], [IsPercentage], [MinOrderValue], [StartDate], [EndDate], [IsActive]) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, voucher.getVoucherCode().toUpperCase().trim());
            ps.setDouble(2, voucher.getDiscountValue());
            ps.setBoolean(3, voucher.isIsPercentage());
            ps.setDouble(4, voucher.getMinOrderValue());
            ps.setTimestamp(5, voucher.getStartDate());
            ps.setTimestamp(6, voucher.getEndDate());
            ps.setBoolean(7, voucher.isIsActive());
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // CẬP NHẬT THÔNG TIN VOUCHER CHỈNH SỬA
    public boolean updateVoucher(Voucher voucher) {
        String sql = "UPDATE [Vouchers] SET [VoucherCode]=?, [DiscountValue]=?, [IsPercentage]=?, [MinOrderValue]=?, [StartDate]=?, [EndDate]=?, [IsActive]=? WHERE [VoucherID]=?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, voucher.getVoucherCode().toUpperCase().trim());
            ps.setDouble(2, voucher.getDiscountValue());
            ps.setBoolean(3, voucher.isIsPercentage());
            ps.setDouble(4, voucher.getMinOrderValue());
            ps.setTimestamp(5, voucher.getStartDate());
            ps.setTimestamp(6, voucher.getEndDate());
            ps.setBoolean(7, voucher.isIsActive());
            ps.setInt(8, voucher.getVoucherID());
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // ĐỔI TRẠNG THÁI HOẠT ĐỘNG NHANH (Phục vụ nút Dừng Kích Hoạt)
    public boolean changeStatus(int id, boolean isActive) {
        String sql = "UPDATE [Vouchers] SET [IsActive] = ? WHERE [VoucherID] = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, isActive);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // XÓA BỎ VOUCHER RA KHỎI ĐỒ ÁN
    public boolean deleteVoucher(int id) {
        String sql = "DELETE FROM [Vouchers] WHERE [VoucherID] = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
