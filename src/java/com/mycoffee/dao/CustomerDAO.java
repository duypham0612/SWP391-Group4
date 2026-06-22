package com.mycoffee.dao;

import com.mycoffee.context.DBContext;
import com.mycoffee.model.Customer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * CustomerDAO — Lớp truy cập dữ liệu cho bảng Customers.
 *
 * Lưu ý: Việc INSERT bản ghi Customer lúc đăng ký được xử lý
 * ngay bên trong UserDAO.registerUser() bằng TRANSACTION để đảm bảo
 * tính toàn vẹn dữ liệu (tạo Users và Customers phải thành công cùng lúc).
 *
 * Các hàm bên đây phục vụ cho các tính năng sau đăng ký:
 * tra cứu điểm, nâng hạng thành viên, lịch sử tích điểm...
 */
public class CustomerDAO {

    /**
     * Lấy thông tin Customer theo CustomerID (= UserID).
     * Dùng để hiển thị trang "Điểm thưởng & Hạng thành viên" của khách hàng.
     */
    public Customer getCustomerById(int customerId) {
        String sql = "SELECT CustomerID, MemberRank, CurrentPoints FROM Customers WHERE CustomerID = ?";
        try (Connection conn = new DBContext().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Customer(
                        rs.getInt("CustomerID"),
                        rs.getString("MemberRank"),
                        rs.getInt("CurrentPoints")
                    );
                }
            }
        } catch (Exception e) {
            System.out.println("Loi ham getCustomerById trong CustomerDAO: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Cộng điểm cho khách hàng sau khi thanh toán thành công.
     * pointsToAdd = số điểm tính dựa trên giá trị hóa đơn (nghiệp vụ xử lý bên Service/Controller).
     *
     * @return true nếu cập nhật thành công
     */
    public boolean addPoints(int customerId, int pointsToAdd) {
        String sql = "UPDATE Customers SET CurrentPoints = CurrentPoints + ? WHERE CustomerID = ?";
        try (Connection conn = new DBContext().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, pointsToAdd);
            ps.setInt(2, customerId);
            int rows = ps.executeUpdate();
            return rows > 0;
        } catch (Exception e) {
            System.out.println("Loi ham addPoints trong CustomerDAO: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Cập nhật hạng thành viên của khách hàng.
     * Gọi hàm này sau khi hệ thống kiểm tra ngưỡng điểm để nâng hạng.
     * Ví dụ logic nâng hạng:
     *   0     - 499   điểm → Member
     *   500   - 1499  điểm → Silver
     *   1500  - 2999  điểm → Gold
     *   3000+ điểm        → Platinum
     *
     * @return true nếu cập nhật thành công
     */
    public boolean updateMemberRank(int customerId, String newRank) {
        String sql = "UPDATE Customers SET MemberRank = ? WHERE CustomerID = ?";
        try (Connection conn = new DBContext().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, newRank);
            ps.setInt(2, customerId);
            int rows = ps.executeUpdate();
            return rows > 0;
        } catch (Exception e) {
            System.out.println("Loi ham updateMemberRank trong CustomerDAO: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Tính toán và tự động nâng hạng dựa trên điểm hiện tại.
     * Gọi sau mỗi lần cộng điểm để cập nhật hạng nếu đủ điều kiện.
     */
    public void autoUpgradeRank(int customerId) {
        Customer customer = getCustomerById(customerId);
        if (customer == null) return;

        int points = customer.getCurrentPoints();
        String newRank;

        if (points >= 3000) {
            newRank = "Platinum";
        } else if (points >= 1500) {
            newRank = "Gold";
        } else if (points >= 500) {
            newRank = "Silver";
        } else {
            newRank = "Member";
        }

        if (!newRank.equals(customer.getMemberRank())) {
            updateMemberRank(customerId, newRank);
            System.out.println("Nang hang thanh vien CustomerID=" + customerId
                             + " len " + newRank + " (diem: " + points + ")");
        }
    }
}
