package com.mycoffee.context;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBContext {

    private final String serverName = "localhost";
    private final String dbName = "MyCoffeeHouse"; 
    private final String portNumber = "1433";    
    private final String userID = "sa";         
    private final String password = "sa";   

    public Connection getConnection() throws Exception {
        String url = "jdbc:sqlserver://" + serverName + ":" + portNumber + ";databaseName=" + dbName + ";encrypt=true;trustServerCertificate=true;";
        Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        return DriverManager.getConnection(url, userID, password);
    }

    // Hàm phụ dùng để đóng kết nối nhanh khi dùng xong
    public static void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                System.out.println("Lỗi khi đóng kết nối: " + e.getMessage());
            }
        }
    }

    // Hàm Main dùng để bấm CHẠY THỬ kết nối xem thành công hay thất bại
    public static void main(String[] args) {
        try {
            DBContext db = new DBContext();
            java.sql.Connection conn = db.getConnection();
            if (conn != null && !conn.isClosed()) {
                System.out.println("KET NOI THANH CONG: Du an MyCoffeeHouse da ket noi Database thanh cong!");
                closeConnection(conn);
            }
        } catch (Exception e) {
            System.out.println("KET NOI THAT BAI: Kiem tra lai tai khoan hoac mat khau SQL!");
            System.out.println("Chi tiet loi: " + e.getMessage());
            e.printStackTrace();
        }
    }
}