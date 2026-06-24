package com.mycoffee.dao;

import com.mycoffee.context.DBContext;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class FeedbackDAO {

    public boolean addFeedback(int orderId, int rating, String comment) {
        String sql = "INSERT INTO Feedbacks (OrderID, Rating, Comment, CreatedAt) VALUES (?, ?, ?, GETDATE())";
        try (Connection conn = new DBContext().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ps.setInt(2, rating);
            ps.setString(3, comment);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
