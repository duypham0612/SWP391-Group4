package com.cafe.service.manager;

import com.cafe.config.DBConnection;
import com.cafe.dao.manager.AttendanceDao;
import com.cafe.model.Attendance;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

/** M3 · AttendanceService — duyệt chấm công. */
public class AttendanceService {

    private final AttendanceDao dao = new AttendanceDao();

    public List<Attendance> getPendingAttendance(int branchId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) { return dao.findByStatus(c, branchId, "PENDING"); }
    }

    public List<Attendance> getAttendanceByStatus(int branchId, String status) throws SQLException {
        try (Connection c = DBConnection.getConnection()) { return dao.findByStatus(c, branchId, status); }
    }

    public Attendance getAttendance(int id) throws SQLException {
        try (Connection c = DBConnection.getConnection()) { return dao.findById(c, id); }
    }

    public void approveAttendance(int id, int approverId) throws SQLException {
        txVoid(c -> dao.updateStatus(c, id, "APPROVED", approverId));
    }

    public void rejectAttendance(int id, int approverId) throws SQLException {
        txVoid(c -> dao.updateStatus(c, id, "REJECTED", approverId));
    }

    /** Manager sửa giờ check-in/out tay. */
    public void updateAttendance(int id, LocalDateTime checkIn, LocalDateTime checkOut) throws SQLException {
        Timestamp ci = checkIn == null ? null : Timestamp.valueOf(checkIn);
        Timestamp co = checkOut == null ? null : Timestamp.valueOf(checkOut);
        txVoid(c -> dao.update(c, id, ci, co));
    }

    /** Số giờ làm của 1 bản ghi chấm công (đọc lại từ DB). */
    public double computeWorkHours(int attendanceId) throws SQLException {
        Attendance a = getAttendance(attendanceId);
        return a == null ? 0d : a.getWorkHours();
    }

    private interface V{ void run(Connection c) throws SQLException; }
    private void txVoid(V v) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try { v.run(c); c.commit(); }
            catch (SQLException e){ c.rollback(); throw e; } finally { c.setAutoCommit(true); }
        }
    }
}
