package com.cafe.service.manager;

import com.cafe.common.BusinessException;
import com.cafe.config.DBConnection;
import com.cafe.config.Tx;
import com.cafe.dao.manager.SupplierDao;
import com.cafe.model.Supplier;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/** M6 · SupplierService (đặc tả mục 5). */
public class SupplierService {

    private final SupplierDao dao;

    public SupplierService() { this(new SupplierDao()); }
    public SupplierService(SupplierDao dao) { this.dao = java.util.Objects.requireNonNull(dao); }

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
        if (supplier == null) throw new BusinessException("Thông tin nhà cung cấp là bắt buộc.");
        String name = clean(supplier.getName());
        String phone = clean(supplier.getPhone());
        String address = clean(supplier.getAddress());
        if (name == null) throw new BusinessException("Tên nhà cung cấp không được để trống.");
        if (phone == null) throw new BusinessException("Số điện thoại không được để trống.");
        if (!phone.matches("0\\d{9}")) {
            throw new BusinessException("Số điện thoại không hợp lệ. Số điện thoại phải gồm đúng 10 chữ số và bắt đầu bằng 0.");
        }
        if (address == null) throw new BusinessException("Địa chỉ không được để trống.");
        supplier.setName(name);
        supplier.setPhone(phone);
        supplier.setAddress(address);
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim().replaceAll("\\s+", " ");
    }

    // Bản tx riêng của file này đã bị gỡ: nó CHỈ bắt SQLException, nên nếu lambda ném
    // RuntimeException (BusinessException) giữa chừng thì không rollback, rồi
    // finally setAutoCommit(true) lại COMMIT phần đã ghi dở. Hiện chưa gây hại vì mọi kiểm tra
    // của service này chạy trước khi vào tx, nhưng đó là bẫy chờ người sau thêm kiểm tra vào trong.
    private <T> T tx(Tx.Block<T> fn) throws SQLException { return Tx.call(fn); }
    private void txVoid(Tx.VoidBlock v) throws SQLException { Tx.run(v); }
}
