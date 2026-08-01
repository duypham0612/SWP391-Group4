package com.cafe.service.admin;

import com.cafe.common.BusinessException;
import com.cafe.config.DBConnection;
import com.cafe.dao.admin.IngredientDao;
import com.cafe.dao.shared.IngredientUnitConversionDao;
import com.cafe.model.Ingredient;
import com.cafe.model.IngredientUnitConversion;

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
    private final IngredientDao dao;
    private final IngredientUnitConversionDao conversionDao;

    public IngredientService() { this(new IngredientDao(), new IngredientUnitConversionDao()); }
    public IngredientService(IngredientDao dao, IngredientUnitConversionDao conversionDao) {
        this.dao = java.util.Objects.requireNonNull(dao);
        this.conversionDao = java.util.Objects.requireNonNull(conversionDao);
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
                validateAndNormalize(conn, i);
                if (!existing.getUnit().equalsIgnoreCase(i.getUnit()) && dao.hasInventoryHistory(conn,i.getIngredientId())) {
                    throw new BusinessException(
                            "Không thể đổi đơn vị gốc khi nguyên liệu đã có tồn kho/giao dịch; hãy tạo nguyên liệu mới.");
                }
                dao.update(conn, i);
                if (!existing.getUnit().equals(i.getUnit())) {
                    conversionDao.renameBase(conn,i.getIngredientId(),i.getUnit(),null);
                }
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

    public List<IngredientUnitConversion> getUnitConversions(int ingredientId,boolean activeOnly)throws SQLException{
        try(Connection conn=DBConnection.getConnection()){return conversionDao.findByIngredient(conn,ingredientId,activeOnly);}
    }

    public java.util.Map<Integer,List<IngredientUnitConversion>> getActiveUnitConversionsByIngredient()throws SQLException{
        try(Connection conn=DBConnection.getConnection()){
            java.util.Map<Integer,List<IngredientUnitConversion>> out=new java.util.LinkedHashMap<>();
            for(IngredientUnitConversion c:conversionDao.findAllActive(conn))
                out.computeIfAbsent(c.getIngredientId(),ignored->new java.util.ArrayList<>()).add(c);
            return out;
        }
    }

    public int addUnitConversion(int ingredientId,String unitName,java.math.BigDecimal factor,int userId)throws SQLException{
        String cleanUnit=normalizeConversionUnit(unitName);validateConversionFactor(factor);
        try(Connection conn=DBConnection.getConnection()){
            conn.setAutoCommit(false);
            try{
                if(dao.findById(conn,ingredientId)==null)throw new BusinessException("Nguyên liệu không tồn tại.");
                int id=conversionDao.insert(conn,ingredientId,cleanUnit,factor,false,true,userId);conn.commit();return id;
            }catch(SQLException e){conn.rollback();if(e.getErrorCode()==2601||e.getErrorCode()==2627)throw new BusinessException("Đơn vị quy đổi đã tồn tại cho nguyên liệu này.");throw e;}
            catch(RuntimeException e){conn.rollback();throw e;}finally{conn.setAutoCommit(true);}
        }
    }

    public void updateUnitConversion(int id,int ingredientId,String unitName,java.math.BigDecimal factor,
                                     boolean active,int userId)throws SQLException{
        String cleanUnit=normalizeConversionUnit(unitName);validateConversionFactor(factor);
        try(Connection conn=DBConnection.getConnection()){
            conn.setAutoCommit(false);try{
                if(conversionDao.update(conn,id,ingredientId,cleanUnit,factor,active,userId)!=1)
                    throw new BusinessException("Không tìm thấy đơn vị quy đổi hoặc đây là đơn vị gốc.");
                conn.commit();
            }catch(SQLException|RuntimeException e){conn.rollback();throw e;}finally{conn.setAutoCommit(true);}
        }
    }

    public void deactivateUnitConversion(int id,int ingredientId,int userId)throws SQLException{
        try(Connection conn=DBConnection.getConnection()){
            conn.setAutoCommit(false);try{
                if(conversionDao.deactivate(conn,id,ingredientId,userId)!=1)
                    throw new BusinessException("Không thể tắt đơn vị gốc hoặc đơn vị không tồn tại.");
                conn.commit();
            }catch(SQLException|RuntimeException e){conn.rollback();throw e;}finally{conn.setAutoCommit(true);}
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
        if (dao.existsByNameAndUnit(conn, name, unit, ingredient.getIngredientId())) {
            throw new BusinessException("Tên và đơn vị nguyên liệu đã tồn tại.");
        }

        ingredient.setName(name);
        ingredient.setUnit(unit);
    }

    private String normalizeConversionUnit(String value){
        String normalized=normalizeSpaces(value);
        if(normalized==null||normalized.isBlank()||normalized.length()>20)
            throw new BusinessException("Tên đơn vị quy đổi phải có từ 1 đến 20 ký tự.");
        return normalized;
    }

    private void validateConversionFactor(java.math.BigDecimal factor){
        if(factor==null||factor.signum()<=0||factor.scale()>6)
            throw new BusinessException("Hệ số quy đổi phải lớn hơn 0 và có tối đa 6 chữ số thập phân.");
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
