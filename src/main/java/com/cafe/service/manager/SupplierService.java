package com.cafe.service.manager;

import com.cafe.common.BusinessException;
import com.cafe.config.DBConnection;
import com.cafe.dao.manager.SupplierDao;
import com.cafe.model.Supplier;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/** M6 · SupplierService (đặc tả mục 5). */
public class SupplierService {

    private final SupplierDao dao = new SupplierDao();

    public List<Supplier> getSupplierList() throws SQLException {
        try (Connection c = DBConnection.getConnection()) { return dao.findAll(c); }
    }
    public List<Supplier> getSupplierListActive() throws SQLException {
        try (Connection c = DBConnection.getConnection()) { return dao.findAllActive(c); }
    }
    public Supplier getSupplier(int id) throws SQLException {
        try (Connection c = DBConnection.getConnection()) { return dao.findById(c, id); }
    }
    public int createSupplier(Supplier s) throws SQLException {
        validate(s);
        return tx(c -> dao.insert(c, s));
    }
    public void updateSupplier(Supplier s) throws SQLException {
        validate(s);
        txVoid(c -> dao.update(c, s));
    }
    public void setSupplierActive(int id, boolean active) throws SQLException { txVoid(c -> dao.updateActive(c, id, active)); }

    /** Đảo trạng thái active (đọc + flip trong 1 tx) — bật/tắt 2 chiều. */
    public void toggleActive(int id) throws SQLException {
        txVoid(c -> { Supplier s = dao.findById(c, id); if (s != null) dao.updateActive(c, id, !s.isActive()); });
    }

    static void validate(Supplier supplier) {
        String phone = supplier == null ? null : supplier.getPhone();
        if (phone == null || !phone.matches("0\\d{9}")) {
            throw new BusinessException("Số điện thoại không hợp lệ. Số điện thoại phải gồm đúng 10 chữ số và bắt đầu bằng 0.");
        }
    }

    private interface Fn<T>{ T run(Connection c) throws SQLException; }
    private interface V{ void run(Connection c) throws SQLException; }
    private <T> T tx(Fn<T> fn) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try { T r = fn.run(c); c.commit(); return r; }
            catch (SQLException e){ c.rollback(); throw e; } finally { c.setAutoCommit(true); }
        }
    }
    private void txVoid(V v) throws SQLException { tx(c -> { v.run(c); return null; }); }
}
