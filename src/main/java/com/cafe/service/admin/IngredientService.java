package com.cafe.service.admin;

import com.cafe.common.BusinessException;
import com.cafe.config.DBConnection;
import com.cafe.dao.admin.IngredientDao;
import com.cafe.dao.shared.IngredientUnitDao;
import com.cafe.model.Ingredient;
import com.cafe.model.InventoryUnitChoice;

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
    private static final Pattern PURCHASE_UNIT_PATTERN =
            Pattern.compile("^[\\p{L}\\p{M}\\p{N}][\\p{L}\\p{M}\\p{N}\\s./()\\-]*$");
    private static final java.math.BigDecimal MAX_PURCHASE_FACTOR =
            new java.math.BigDecimal("1000000");
    private final IngredientDao dao;
    private final IngredientUnitDao unitDao;

    public IngredientService() { this(new IngredientDao(), new IngredientUnitDao()); }
    public IngredientService(IngredientDao dao, IngredientUnitDao unitDao) {
        this.dao = java.util.Objects.requireNonNull(dao);
        this.unitDao = java.util.Objects.requireNonNull(unitDao);
    }

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
        if (id <= 0) throw new BusinessException("Mã nguyên liệu không hợp lệ.");
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
            catch (SQLException e) {
                conn.rollback();
                if (e.getErrorCode() == 2601 || e.getErrorCode() == 2627)
                    throw new BusinessException("Tên và đơn vị nguyên liệu đã tồn tại.");
                throw e;
            }
            catch (RuntimeException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    public void updateIngredient(Ingredient i) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Ingredient existing = dao.findById(conn, i.getIngredientId());
                if (existing == null) {
                    throw new BusinessException("Không tìm thấy nguyên liệu cần cập nhật.");
                }
                // PrepYieldQty được quản lý ở màn công thức, không để form nguyên liệu xoá nhầm.
                i.setPrepYieldQty(existing.getPrepYieldQty());
                validateAndNormalize(conn, i);
                if (!existing.getUnit().equalsIgnoreCase(i.getUnit()) && dao.hasInventoryHistory(conn,i.getIngredientId())) {
                    throw new BusinessException(
                            "Không thể đổi đơn vị gốc khi nguyên liệu đã có tồn kho/giao dịch; hãy tạo nguyên liệu mới.");
                }
                if (!existing.getIngredientType().equals(i.getIngredientType())
                        && dao.hasTypeSensitiveUsage(conn, i.getIngredientId())) {
                    throw new BusinessException(
                            "Không thể đổi loại nguyên liệu khi đã có công thức hoặc dữ liệu pha sẵn; hãy tạo nguyên liệu mới.");
                }
                dao.update(conn, i);
                conn.commit();
            }
            catch (SQLException e) {
                conn.rollback();
                if (e.getErrorCode() == 2601 || e.getErrorCode() == 2627)
                    throw new BusinessException("Tên và đơn vị nguyên liệu đã tồn tại.");
                throw e;
            }
            catch (RuntimeException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    public List<InventoryUnitChoice> getUnitChoices(int ingredientId)throws SQLException{
        try(Connection conn=DBConnection.getConnection()){return unitDao.findByIngredient(conn,ingredientId);}
    }

    public java.util.Map<Integer,List<InventoryUnitChoice>> getActiveUnitChoicesByIngredient()throws SQLException{
        try(Connection conn=DBConnection.getConnection()){
            java.util.Map<Integer,List<InventoryUnitChoice>> out=new java.util.LinkedHashMap<>();
            for(InventoryUnitChoice c:unitDao.findAllActive(conn))
                out.computeIfAbsent(c.getIngredientId(),ignored->new java.util.ArrayList<>()).add(c);
            return out;
        }
    }

    public void deleteIngredient(int id) throws SQLException {
        if (id <= 0) throw new BusinessException("Mã nguyên liệu không hợp lệ.");
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (dao.delete(conn, id) != 1) {
                    throw new BusinessException("Không tìm thấy nguyên liệu cần ẩn.");
                }
                conn.commit();
            }
            catch (SQLException e) { conn.rollback(); throw e; }
            catch (RuntimeException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    private void validateAndNormalize(Connection conn, Ingredient ingredient) throws SQLException {
        validateAndNormalizeFields(ingredient);
        if (dao.existsByNameAndUnit(conn, ingredient.getName(), ingredient.getUnit(), ingredient.getIngredientId())) {
            throw new BusinessException("Tên và đơn vị nguyên liệu đã tồn tại.");
        }
    }

    static void validateAndNormalizeFields(Ingredient ingredient) {
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
        String purchaseUnit = normalizeSpaces(ingredient.getPurchaseUnitName());
        java.math.BigDecimal purchaseFactor = ingredient.getPurchaseFactorToBase();
        if ((purchaseUnit == null || purchaseUnit.isBlank()) && purchaseFactor == null) {
            ingredient.setPurchaseUnitName(null);
            ingredient.setPurchaseFactorToBase(null);
        } else {
            if (purchaseUnit == null || purchaseUnit.isBlank() || purchaseFactor == null)
                throw new BusinessException("Đơn vị mua và hệ số quy đổi phải được nhập cùng nhau.");
            purchaseUnit = normalizeConversionUnit(purchaseUnit);
            validateConversionFactor(purchaseFactor);
            if (purchaseUnit.equalsIgnoreCase(unit))
                throw new BusinessException("Đơn vị mua phải khác đơn vị gốc.");
            ingredient.setPurchaseUnitName(purchaseUnit);
            ingredient.setPurchaseFactorToBase(purchaseFactor.stripTrailingZeros());
        }

        ingredient.setName(name);
        ingredient.setUnit(unit);
    }

    private static String normalizeConversionUnit(String value){
        String normalized=normalizeSpaces(value);
        if(normalized==null||normalized.isBlank()||normalized.length()>20)
            throw new BusinessException("Đơn vị nhập hàng phải có từ 1 đến 20 ký tự.");
        if (!PURCHASE_UNIT_PATTERN.matcher(normalized).matches())
            throw new BusinessException("Đơn vị nhập hàng chỉ được chứa chữ, số, khoảng trắng, dấu chấm, gạch nối hoặc dấu gạch chéo.");
        return normalized;
    }

    private static void validateConversionFactor(java.math.BigDecimal factor){
        if(factor==null||factor.compareTo(java.math.BigDecimal.ONE)<=0
                ||factor.stripTrailingZeros().scale()>0||factor.compareTo(MAX_PURCHASE_FACTOR)>0)
            throw new BusinessException("Số lượng quy đổi phải là số nguyên lớn hơn 1 và không vượt quá 1.000.000.");
    }

    private static String normalizeSpaces(String value) {
        if (value == null) return null;
        return value.trim().replaceAll("\\s+", " ");
    }

    private static String normalizeUnit(String value) {
        if (value == null) return "";
        String normalized = value.trim();
        if ("l".equalsIgnoreCase(normalized)) return "L";
        if ("ml".equalsIgnoreCase(normalized)) return "ml";
        if ("kg".equalsIgnoreCase(normalized)) return "kg";
        if ("g".equalsIgnoreCase(normalized)) return "g";
        return normalized.toLowerCase(Locale.forLanguageTag("vi"));
    }
}
