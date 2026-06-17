package com.mycoffee.dao;

import com.mycoffee.context.DBContext;
import com.mycoffee.model.Shift;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ShiftDAO {
    // Hàm này giúp lấy 3 ca (Sáng, Chiều, Tối) đổ vào thanh Chọn ở giao diện
    public List<Shift> getAllShifts() {
        List<Shift> list = new ArrayList<>();
        String sql = "SELECT [ShiftID], [ShiftName], [StartTime], [EndTime] FROM [Shifts]";
        try {
            Connection conn = new DBContext().getConnection(); // Hoặc DBContext.getConnection() tùy dự án của bạn
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Shift s = new Shift();
                s.setShiftId(rs.getInt("ShiftID"));
                s.setShiftName(rs.getString("ShiftName"));
                s.setStartTime(rs.getTime("StartTime"));
                s.setEndTime(rs.getTime("EndTime"));
                list.add(s);
            }
            rs.close();
            ps.close();
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }
}
