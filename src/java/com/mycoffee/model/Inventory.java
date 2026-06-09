package com.mycoffee.model;
import java.sql.Timestamp;

public class Inventory {
    private int branchId;
    private int ingredientId;
    private double quantity;
    private double minRequired;
    private Timestamp lastUpdated;
    
    // Thuộc tính bổ sung (Không có trong bảng nhưng dùng để hiển thị tên nguyên liệu/đơn vị lên web cho Manager nhìn)
    private String ingredientName;
    private String unit;

    public Inventory() {}

    // Duy tự sinh các Hàm khởi tạo (Constructor) và Getter/Setter nhé!
}