package com.cafe.service.admin;

import com.cafe.common.BusinessException;
import com.cafe.config.DBConnection;
import com.cafe.config.Tx;
import com.cafe.dao.admin.ProductDao;
import com.cafe.dao.admin.BranchDao;
import com.cafe.model.Branch;
import com.cafe.model.Product;

import java.sql.Connection;
import java.sql.SQLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Quản trị trang Home công khai (Admin): chọn món hiển thị + thứ tự + nội dung hero.
 * Đọc qua DAO, ghi trong transaction (đúng quy ước: tx sống ở Service).
 */
public class HomeAdminService {

    public static final int MAX_HOME_PRODUCTS = 500;
    public static final int MAX_HOME_SORT_ORDER = 1_000_000;

    private final ProductDao productDao;
    private final BranchDao branchDao;

    public HomeAdminService() { this(new ProductDao(), new BranchDao()); }
    public HomeAdminService(ProductDao productDao, BranchDao branchDao) {
        this.productDao = java.util.Objects.requireNonNull(productDao);
        this.branchDao = java.util.Objects.requireNonNull(branchDao);
    }

    /** Danh sách sản phẩm đang bán (gồm cả món đang ẩn) cho màn quản trị Home. */
    public List<Product> getProductsForAdmin() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return productDao.findActiveForHomeAdmin(conn);
        }
    }

    public List<Branch> getBranches() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return branchDao.findAll(conn);
        }
    }

    /** Hero thuộc chi nhánh được chọn; mặc định là chi nhánh active đầu tiên theo BranchId. */
    public Branch getHomeBranch(Integer branchId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            if (branchId != null && branchId > 0) {
                Branch selected = branchDao.findById(conn, branchId);
                if (selected == null) {
                    throw new BusinessException("Không tìm thấy chi nhánh cần chỉnh sửa trang Home.");
                }
                return selected;
            }
            Branch active = branchDao.findFirstActive(conn);
            if (active != null) return active;
            List<Branch> branches = branchDao.findAll(conn);
            return branches.isEmpty() ? null : branches.get(0);
        }
    }

    /**
     * Lưu hiển thị + thứ tự Home cho NHIỀU sản phẩm trong 1 transaction (nút "Lưu tất cả").
     * 3 mảng song song theo cùng chỉ số; thứ tự âm được ép về 0.
     */
    public void saveProductHomeBatch(int[] ids, boolean[] shows, int[] orders) throws SQLException {
        validateHomeProductBatch(ids, shows, orders);
        Tx.run(conn -> {
            for (int i = 0; i < ids.length; i++) {
                if (productDao.updateHomeDisplay(conn, ids[i], shows[i], orders[i]) != 1) {
                    throw new BusinessException(
                            "Sản phẩm đã ngừng hoạt động hoặc không còn tồn tại. Vui lòng tải lại trang.");
                }
            }
        });
    }

    /** Lưu nội dung hero trang Home. */
    public void saveContent(Branch branch) throws SQLException {
        validateAndNormalizeContent(branch);
        Tx.run(conn -> {
            if (branchDao.updateHero(conn, branch) != 1) {
                throw new BusinessException("Không tìm thấy chi nhánh cần cập nhật hero.");
            }
        });
    }

    static void validateHomeProductBatch(int[] ids, boolean[] shows, int[] orders) {
        if (ids == null || shows == null || orders == null || ids.length == 0) {
            throw new BusinessException("Không tìm thấy danh sách sản phẩm cần lưu.");
        }
        if (ids.length > MAX_HOME_PRODUCTS) {
            throw new BusinessException("Mỗi lần chỉ được lưu tối đa " + MAX_HOME_PRODUCTS + " sản phẩm.");
        }
        if (shows.length != ids.length || orders.length != ids.length) {
            throw new BusinessException("Dữ liệu hiển thị sản phẩm không đồng nhất. Vui lòng tải lại trang.");
        }
        Set<Integer> uniqueIds = new HashSet<>();
        for (int i = 0; i < ids.length; i++) {
            if (ids[i] <= 0 || !uniqueIds.add(ids[i])) {
                throw new BusinessException("Danh sách sản phẩm chứa mã không hợp lệ hoặc bị trùng.");
            }
            if (orders[i] < 0 || orders[i] > MAX_HOME_SORT_ORDER) {
                throw new BusinessException(
                        "Thứ tự hiển thị phải từ 0 đến " + MAX_HOME_SORT_ORDER + ".");
            }
        }
    }

    static void validateAndNormalizeContent(Branch branch) {
        if (branch == null || branch.getBranchId() <= 0) {
            throw new BusinessException("Vui lòng chọn chi nhánh cần cập nhật trang Home.");
        }
        branch.setHeroEyebrow(trimToNull(branch.getHeroEyebrow()));
        branch.setHeroTitle(trimToNull(branch.getHeroTitle()));
        branch.setHeroSubtitle(trimToNull(branch.getHeroSubtitle()));
        branch.setHeroImageUrl(trimToNull(branch.getHeroImageUrl()));

        if (branch.getHeroTitle() == null) {
            throw new BusinessException("Tiêu đề trang Home không được để trống.");
        }
        requireMaxLength(branch.getHeroEyebrow(), 150, "Dòng giới thiệu");
        requireMaxLength(branch.getHeroTitle(), 200, "Tiêu đề trang Home");
        requireMaxLength(branch.getHeroSubtitle(), 500, "Mô tả trang Home");
        requireMaxLength(branch.getHeroImageUrl(), 500, "Đường dẫn ảnh");
        validateImageUrl(branch.getHeroImageUrl());
    }

    private static void validateImageUrl(String value) {
        if (value == null) return;
        if (!StandardCharsets.US_ASCII.newEncoder().canEncode(value)
                || value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0) {
            throw new BusinessException("Đường dẫn ảnh chứa ký tự không hợp lệ.");
        }
        if (value.startsWith("/assets/") && !value.contains("..") && value.indexOf('\\') < 0) {
            return;
        }
        try {
            URI uri = new URI(value);
            if ("https".equalsIgnoreCase(uri.getScheme())
                    && uri.getHost() != null && uri.getUserInfo() == null) {
                return;
            }
        } catch (URISyntaxException ignored) {
            // Thông báo nghiệp vụ thống nhất ở dưới.
        }
        throw new BusinessException(
                "Ảnh phải là liên kết HTTPS hoặc đường dẫn nội bộ bắt đầu bằng /assets/.");
    }

    private static void requireMaxLength(String value, int maxLength, String label) {
        if (value != null && value.length() > maxLength) {
            throw new BusinessException(label + " tối đa " + maxLength + " ký tự.");
        }
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
