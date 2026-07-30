package com.cafe.service.admin;

import com.cafe.common.BusinessException;
import com.cafe.config.DBConnection;
import com.cafe.dao.admin.IngredientDao;
import com.cafe.model.Ingredient;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * A4 · IngredientService (đặc tả mục 4) — NƠI đặt cờ RAW/PREPPED.
 */
public class IngredientService {

    public static final List<String> SUPPORTED_UNITS =
            List.of("g", "kg", "ml", "L", "cái", "phần", "gói", "chai", "lon", "hộp");

    private static final Set<String> TYPES = Set.of("RAW", "PREPPED");
    private static final Pattern NAME_PATTERN =
            Pattern.compile("^[\\p{L}\\p{M}\\p{N}][\\p{L}\\p{M}\\p{N}\\s.,&'()/%+\\-]*$");
    private final IngredientDao dao = new IngredientDao();

    public List<Ingredient> getIngredientList() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) { return dao.findAll(conn); }
    }

    public List<Ingredient> getIngredientListByType(String type) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) { return dao.findByType(conn, type); }
    }

    public List<Ingredient> getActiveIngredientList() throws SQLException {
        return getIngredientList().stream().filter(Ingredient::isActive).toList();
    }

    public Ingredient getIngredient(int id) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) { return dao.findById(conn, id); }
    }

    public int createIngredient(Ingredient i) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                validateAndNormalize(conn, i);
                int id = dao.insert(conn, i);
                conn.commit();
                return id;
            }
            catch (SQLException e) { conn.rollback(); throw e; }
            catch (RuntimeException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    public void updateIngredient(Ingredient i) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (dao.findById(conn, i.getIngredientId()) == null) {
                    throw new BusinessException("Không tìm thấy nguyên liệu cần cập nhật.");
                }
                validateAndNormalize(conn, i);
                dao.update(conn, i);
                conn.commit();
            }
            catch (SQLException e) { conn.rollback(); throw e; }
            catch (RuntimeException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    public void deleteIngredient(int id) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try { dao.delete(conn, id); conn.commit(); }
            catch (SQLException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    private void validateAndNormalize(Connection conn, Ingredient ingredient) throws SQLException {
        String name = normalizeSpaces(ingredient.getName());
        if (name == null || name.length() < 2 || name.length() > 120) {
            throw new BusinessException("Tên nguyên liệu phải có từ 2 đến 120 ký tự.");
        }
        if (!NAME_PATTERN.matcher(name).matches()) {
            throw new BusinessException(
                    "Tên nguyên liệu chỉ được chứa chữ, số, khoảng trắng và dấu câu thông dụng.");
        }

        String unit = normalizeUnit(ingredient.getUnit());
        if (!SUPPORTED_UNITS.contains(unit)) {
            throw new BusinessException("Đơn vị nguyên liệu không hợp lệ. Vui lòng chọn trong danh sách.");
        }
        if (!TYPES.contains(ingredient.getIngredientType())) {
            throw new BusinessException("Loại nguyên liệu không hợp lệ.");
        }
        if ("PREPPED".equals(ingredient.getIngredientType())
                && (ingredient.getShelfLifeMinutes() == null
                || ingredient.getShelfLifeMinutes() < 60
                || ingredient.getShelfLifeMinutes() > 43200)) {
            throw new BusinessException(
                    "Thời hạn bảo quản của nguyên liệu pha sẵn phải từ 1 đến 720 giờ.");
        }
        if ("RAW".equals(ingredient.getIngredientType())) {
            ingredient.setShelfLifeMinutes(null);
        }
        if (dao.existsByName(conn, name, ingredient.getIngredientId())) {
            throw new BusinessException("Tên nguyên liệu đã tồn tại.");
        }

        ingredient.setName(name);
        ingredient.setUnit(unit);
    }

    private String normalizeSpaces(String value) {
        if (value == null) return null;
        return value.trim().replaceAll("\\s+", " ");
    }

    private String normalizeUnit(String value) {
        if (value == null) return "";
        String normalized = value.trim();
        if ("l".equalsIgnoreCase(normalized)) return "L";
        if ("ml".equalsIgnoreCase(normalized)) return "ml";
        if ("kg".equalsIgnoreCase(normalized)) return "kg";
        if ("g".equalsIgnoreCase(normalized)) return "g";
        return normalized.toLowerCase(Locale.forLanguageTag("vi"));
    }
}
