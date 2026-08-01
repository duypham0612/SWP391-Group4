package com.cafe.service.admin;

import com.cafe.common.BusinessException;
import com.cafe.config.DBConnection;
import com.cafe.dao.admin.IngredientDao;
import com.cafe.dao.admin.ProductDao;
import com.cafe.dao.shared.RecipeDao;
import com.cafe.model.Ingredient;
import com.cafe.model.Recipe;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Quản lý định mức chung cho sản phẩm và nguyên liệu pha sẵn. */
public class RecipeService {
    private static final BigDecimal MAX_QUANTITY = new BigDecimal("999999999");
    private final RecipeDao recipeDao;
    private final IngredientDao ingredientDao;
    private final ProductDao productDao;

    public RecipeService() { this(new RecipeDao(), new IngredientDao(), new ProductDao()); }
    public RecipeService(RecipeDao recipeDao, IngredientDao ingredientDao, ProductDao productDao) {
        this.recipeDao = java.util.Objects.requireNonNull(recipeDao);
        this.ingredientDao = java.util.Objects.requireNonNull(ingredientDao);
        this.productDao = java.util.Objects.requireNonNull(productDao);
    }

    public record RecipeLineInput(int ingredientId, BigDecimal quantity) {}

    public List<Recipe> getProductRecipe(int productId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return recipeDao.findByOwner(conn, Recipe.OWNER_PRODUCT, productId);
        }
    }

    public List<Recipe> getPrepRecipe(int preppedIngredientId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return recipeDao.findByOwner(conn, Recipe.OWNER_PREPPED, preppedIngredientId);
        }
    }

    public int addRecipeLines(int productId, List<RecipeLineInput> inputs) throws SQLException {
        if (productId <= 0) throw new BusinessException("Sản phẩm không hợp lệ.");
        validateInputCount(inputs);
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (productDao.findById(conn, productId) == null)
                    throw new BusinessException("Không tìm thấy sản phẩm cần tạo công thức.");
                validateLines(conn, Recipe.OWNER_PRODUCT, productId, inputs, null);
                insertLines(conn, Recipe.OWNER_PRODUCT, productId, inputs);
                conn.commit();
                return inputs.size();
            } catch (SQLException e) {
                conn.rollback();
                throw translateRecipeError(e);
            } catch (RuntimeException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    public void updateRecipeLine(int productId, int lineId, BigDecimal quantity) throws SQLException {
        updateLine(Recipe.OWNER_PRODUCT, productId, lineId, quantity);
    }

    public void removeRecipeLine(int productId, int lineId) throws SQLException {
        removeLine(Recipe.OWNER_PRODUCT, productId, lineId);
    }

    public int addPrepRecipeLines(int preppedIngredientId, BigDecimal yieldQty,
                                  List<RecipeLineInput> inputs) throws SQLException {
        if (preppedIngredientId <= 0)
            throw new BusinessException("Nguyên liệu pha sẵn không hợp lệ.");
        validateYield(yieldQty);
        validateInputCount(inputs);
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Ingredient prepped = requirePrepped(conn, preppedIngredientId);
                validateLines(conn, Recipe.OWNER_PREPPED, preppedIngredientId, inputs, "RAW");
                if (ingredientDao.updatePrepYield(conn, preppedIngredientId, yieldQty) != 1)
                    throw new BusinessException("Công thức vừa được thay đổi. Vui lòng tải lại.");
                insertLines(conn, Recipe.OWNER_PREPPED, preppedIngredientId, inputs);
                conn.commit();
                return inputs.size();
            } catch (SQLException e) {
                conn.rollback();
                throw translateRecipeError(e);
            } catch (RuntimeException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    public void updatePrepRecipeYield(int preppedIngredientId, BigDecimal yieldQty) throws SQLException {
        validateYield(yieldQty);
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                requirePrepped(conn, preppedIngredientId);
                if (ingredientDao.updatePrepYield(conn, preppedIngredientId, yieldQty) != 1)
                    throw new BusinessException("Công thức vừa được thay đổi. Vui lòng tải lại.");
                conn.commit();
            } catch (SQLException e) { conn.rollback(); throw translateRecipeError(e); }
            catch (RuntimeException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    public void updatePrepRecipeLine(int preppedIngredientId, int lineId, BigDecimal quantity)
            throws SQLException {
        updateLine(Recipe.OWNER_PREPPED, preppedIngredientId, lineId, quantity);
    }

    public void removePrepRecipeLine(int preppedIngredientId, int lineId) throws SQLException {
        removeLine(Recipe.OWNER_PREPPED, preppedIngredientId, lineId);
    }

    private void updateLine(String ownerType, int ownerId, int lineId, BigDecimal quantity)
            throws SQLException {
        validateQuantity(quantity);
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (recipeDao.update(conn, lineId, ownerType, ownerId, quantity) != 1)
                    throw new BusinessException("Không tìm thấy nguyên liệu trong công thức này.");
                conn.commit();
            } catch (SQLException e) { conn.rollback(); throw translateRecipeError(e); }
            catch (RuntimeException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    private void removeLine(String ownerType, int ownerId, int lineId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (recipeDao.delete(conn, lineId, ownerType, ownerId) != 1)
                    throw new BusinessException("Không tìm thấy nguyên liệu trong công thức này.");
                conn.commit();
            } catch (SQLException e) { conn.rollback(); throw translateRecipeError(e); }
            catch (RuntimeException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    private Ingredient requirePrepped(Connection conn, int ingredientId) throws SQLException {
        Ingredient ingredient = ingredientDao.findById(conn, ingredientId);
        if (ingredient == null || !ingredient.isActive()
                || !"PREPPED".equals(ingredient.getIngredientType()))
            throw new BusinessException("Không tìm thấy nguyên liệu pha sẵn cần cập nhật.");
        return ingredient;
    }

    private void validateLines(Connection conn, String ownerType, int ownerId,
                               List<RecipeLineInput> inputs, String requiredIngredientType)
            throws SQLException {
        Set<Integer> existingIds = new HashSet<>();
        for (Recipe line : recipeDao.findByOwner(conn, ownerType, ownerId))
            existingIds.add(line.getIngredientId());
        Set<Integer> submittedIds = new HashSet<>();
        for (RecipeLineInput input : inputs) {
            Ingredient ingredient = ingredientDao.findById(conn, input.ingredientId());
            if (ingredient == null || !ingredient.isActive()
                    || requiredIngredientType != null
                    && !requiredIngredientType.equals(ingredient.getIngredientType()))
                throw new BusinessException(requiredIngredientType == null
                        ? "Nguyên liệu đã chọn không tồn tại hoặc đã ngừng hoạt động."
                        : "Nguyên liệu thô đã chọn không tồn tại hoặc đã ngừng hoạt động.");
            if (!submittedIds.add(input.ingredientId()))
                throw new BusinessException("Không được chọn trùng nguyên liệu trong cùng một công thức.");
            if (existingIds.contains(input.ingredientId()))
                throw new BusinessException("Nguyên liệu \"" + ingredient.getName()
                        + "\" đã có trong công thức.");
            validateQuantity(input.quantity());
        }
    }

    private void insertLines(Connection conn, String ownerType, int ownerId,
                             List<RecipeLineInput> inputs) throws SQLException {
        for (RecipeLineInput input : inputs) {
            Recipe line = new Recipe();
            line.setOwnerType(ownerType);
            line.setOwnerId(ownerId);
            line.setIngredientId(input.ingredientId());
            line.setQuantity(input.quantity());
            recipeDao.insert(conn, line);
        }
    }

    private void validateInputCount(List<RecipeLineInput> inputs) {
        if (inputs == null || inputs.isEmpty())
            throw new BusinessException("Vui lòng chọn ít nhất một nguyên liệu.");
        if (inputs.size() > 100)
            throw new BusinessException("Mỗi lần chỉ được thêm tối đa 100 nguyên liệu.");
    }

    private void validateQuantity(BigDecimal quantity) {
        if (quantity == null || quantity.compareTo(BigDecimal.ONE) <= 0)
            throw new BusinessException("Định mức nguyên liệu phải là số nguyên lớn hơn 1.");
        if (quantity.stripTrailingZeros().scale() > 0)
            throw new BusinessException("Định mức nguyên liệu không được có phần thập phân.");
        if (quantity.compareTo(MAX_QUANTITY) > 0)
            throw new BusinessException("Định mức nguyên liệu vượt quá giới hạn cho phép.");
    }

    private void validateYield(BigDecimal yieldQty) {
        if (yieldQty == null || yieldQty.signum() <= 0)
            throw new BusinessException("Sản lượng một mẻ phải lớn hơn 0.");
        if (yieldQty.compareTo(new BigDecimal("999999999.999")) > 0)
            throw new BusinessException("Sản lượng một mẻ vượt quá giới hạn cho phép.");
        if (yieldQty.stripTrailingZeros().scale() > 3)
            throw new BusinessException("Sản lượng một mẻ chỉ được có tối đa 3 chữ số thập phân.");
    }

    /** THROW từ trigger Recipe dùng mã lỗi user-defined của SQL Server. */
    private SQLException translateRecipeError(SQLException error) {
        for (SQLException current = error; current != null; current = current.getNextException()) {
            if (current.getErrorCode() >= 50000) {
                throw new BusinessException(
                        "Công thức không hợp lệ: sản phẩm/tuỳ chọn hoặc loại nguyên liệu không đúng.");
            }
            if (current.getErrorCode() == 2601 || current.getErrorCode() == 2627)
                throw new BusinessException("Nguyên liệu này đã có trong công thức.");
        }
        return error;
    }
}
