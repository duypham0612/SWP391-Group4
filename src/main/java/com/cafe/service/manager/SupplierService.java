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
        return tx(c -> { if (dao.existsByName(c, s.getName(), 0)) throw new BusinessException("Tên nhà cung cấp đã tồn tại."); return dao.insert(c, s); });
    }
    public void updateSupplier(Supplier s) throws SQLException {
        validate(s);
        txVoid(c -> {
            if (dao.findById(c, s.getSupplierId()) == null) throw new BusinessException("Nhà cung cấp không tồn tại.");
            if (dao.existsByName(c, s.getName(), s.getSupplierId())) throw new BusinessException("Tên nhà cung cấp đã tồn tại.");
            dao.update(c, s);
        });
    }
    public void setSupplierActive(int id, boolean active) throws SQLException { txVoid(c -> dao.updateActive(c, id, active)); }

    /** Đảo trạng thái active (đọc + flip trong 1 tx) — bật/tắt 2 chiều. */
    public void toggleActive(int id) throws SQLException {
        txVoid(c -> { Supplier s = dao.findById(c, id); if (s == null) throw new BusinessException("Nhà cung cấp không tồn tại."); dao.updateActive(c, id, !s.isActive()); });
    }

    static void validate(Supplier s) {
        if (s == null || s.getName() == null || s.getName().isBlank()) throw new BusinessException("Tên nhà cung cấp không được để trống.");
        if (s.getName().trim().length() > 150) throw new BusinessException("Tên nhà cung cấp không được vượt quá 150 ký tự.");
        if (s.getPhone() == null || s.getPhone().isBlank()) throw new BusinessException("Số điện thoại không được để trống.");
        String phone = s.getPhone().trim();
        if (!phone.matches("0\\d{9,10}")) throw new BusinessException("Số điện thoại phải bắt đầu bằng 0 và có 10 hoặc 11 chữ số.");
        if (s.getAddress() == null || s.getAddress().isBlank()) throw new BusinessException("Địa chỉ không được để trống.");
        if (s.getAddress().trim().length() > 255) throw new BusinessException("Địa chỉ không được vượt quá 255 ký tự.");
        s.setName(s.getName().trim()); s.setPhone(phone); s.setAddress(s.getAddress().trim());
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
