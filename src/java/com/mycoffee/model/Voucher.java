package com.mycoffee.model;

import java.sql.Timestamp;
import java.text.NumberFormat;
import java.util.Locale;

public class Voucher {
    private int voucherID;
    private String voucherCode;
    private double discountValue;
    private boolean isPercentage;
    private double minOrderValue;
    private Timestamp startDate;
    private Timestamp endDate;
    private boolean isActive;

    // Constructors
    public Voucher() {}

    // Getters and Setters cho tất cả các trường
    // ... (bạn tự tạo Getter/Setter mặc định) ...

    public int getVoucherID() { return voucherID; }
    public void setVoucherID(int voucherID) { this.voucherID = voucherID; }
    public String getVoucherCode() { return voucherCode; }
    public void setVoucherCode(String voucherCode) { this.voucherCode = voucherCode; }
    public double getDiscountValue() { return discountValue; }
    public void setDiscountValue(double discountValue) { this.discountValue = discountValue; }
    public boolean isIsPercentage() { return isPercentage; }
    public void setIsPercentage(boolean isPercentage) { this.isPercentage = isPercentage; }
    public double getMinOrderValue() { return minOrderValue; }
    public void setMinOrderValue(double minOrderValue) { this.minOrderValue = minOrderValue; }
    public Timestamp getStartDate() { return startDate; }
    public void setStartDate(Timestamp startDate) { this.startDate = startDate; }
    public Timestamp getEndDate() { return endDate; }
    public void setEndDate(Timestamp endDate) { this.endDate = endDate; }
    public boolean isIsActive() { return isActive; }
    public void setIsActive(boolean isActive) { this.isActive = isActive; }

    // --- CÁC HÀM BỔ TRỢ ĐỂ HIỂN THỊ LÊN GIAO DIỆN CHÍNH XÁC ---
    
    // Tự sinh nội dung cột "LOẠI" (Ví dụ: Giảm 20% hoặc Giảm 50.000đ)
    public String getVoucherTypeLabel() {
        if (isPercentage) {
            return "Giảm " + (int)discountValue + "%";
        } else {
            NumberFormat nv = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            return "Giảm " + nv.format(discountValue).replace("₫", "đ");
        }
    }

    // Tự sinh nội dung dòng mô tả phụ dựa trên điều kiện đơn hàng tối thiểu
    public String getDescription() {
        if (minOrderValue <= 0) {
            return "Áp dụng cho mọi đơn hàng";
        }
        NumberFormat nv = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        return "Áp dụng cho đơn từ " + nv.format(minOrderValue).replace("₫", "đ");
    }

    // Tính toán trạng thái động theo thời gian thực (Real-time) để hiển thị badge
    public String getStatusLabel() {
        if (!isActive) return "Bản nháp";
        long now = System.currentTimeMillis();
        if (now < startDate.getTime()) return "Sắp diễn ra";
        if (now >= startDate.getTime() && now <= endDate.getTime()) return "Đang chạy";
        return "Đã kết thúc";
    }
}
