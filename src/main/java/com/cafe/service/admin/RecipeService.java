package com.cafe.service.admin;

import com.cafe.common.BusinessException;
import com.cafe.config.DBConnection;
import com.cafe.dao.admin.IngredientDao;
import com.cafe.dao.admin.ProductDao;
import com.cafe.dao.shared.PrepRecipeDao;
import com.cafe.dao.shared.ProductRecipeDao;
import com.cafe.model.Ingredient;
import com.cafe.model.PrepRecipe;
import com.cafe.model.ProductRecipe;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A4 · RecipeService — định mức nguyên liệu cho sản phẩm và nguyên liệu pha sẵn.
 */
public class RecipeService {

    private static final BigDecimal MAX_QUANTITY = new BigDecimal("999999999");
    private final ProductRecipeDao productRecipeDao = new ProductRecipeDao();
    private final PrepRecipeDao prepRecipeDao = new PrepRecipeDao();
    private final IngredientDao ingredientDao = new IngredientDao();
    private final ProductDao productDao = new ProductDao();

    public record RecipeLineInput(int ingredientId, BigDecimal quantity) {}

    public List<ProductRecipe> getProductRecipe(int productId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return productRecipeDao.findByProduct(conn, productId);
        }
    }

    public int addRecipeLines(int productId, List<RecipeLineInput> inputs) throws SQLException {
        if (productId <= 0) throw new BusinessException("Sản phẩm không hợp lệ.");
        if (inputs == null || inputs.isEmpty()) {
            throw new BusinessException("Vui lòng chọn ít nhất một nguyên liệu.");
        }
        if (inputs.size() > 100) {
            throw new BusinessException("Mỗi lần chỉ được thêm tối đa 100 nguyên liệu.");
        }

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (productDao.findById(conn, productId) == null) {
                    throw new BusinessException("Không tìm thấy sản phẩm cần tạo công thức.");
                }
                Set<Integer> existingIds = new HashSet<>();
                for (ProductRecipe line : productRecipeDao.findByProduct(conn, productId)) {
                    existingIds.add(line.getIngredientId());
                }

                Set<Integer> submittedIds = new HashSet<>();
                for (RecipeLineInput input : inputs) {
                    Ingredient ingredient = ingredientDao.findById(conn, input.ingredientId());
                    if (ingredient == null || !ingredient.isActive()) {
                        throw new BusinessException("Nguyên liệu đã chọn không tồn tại hoặc đã ngừng hoạt động.");
                    }
                    if (!submittedIds.add(input.ingredientId())) {
                        throw new BusinessException("Không được chọn trùng nguyên liệu trong cùng một công thức.");
                    }
                    if (existingIds.contains(input.ingredientId())) {
                        throw new BusinessException(
                                "Nguyên liệu \"" + ingredient.getName() + "\" đã có trong công thức.");
                    }
                    validateQuantity(input.quantity());
                }

                for (RecipeLineInput input : inputs) {
                    ProductRecipe recipe = new ProductRecipe();
                    recipe.setProductId(productId);
                    recipe.setIngredientId(input.ingredientId());
                    recipe.setQuantity(input.quantity());
                    productRecipeDao.insert(conn, recipe);
                }
                conn.commit();
                return inputs.size();
            }
            catch (SQLException e) { conn.rollback(); throw e; }
            catch (RuntimeException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    public void updateRecipeLine(int productId, int lineId, BigDecimal qty) throws SQLException {
        validateQuantity(qty);
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (productRecipeDao.update(conn, lineId, productId, qty) == 0) {
                    throw new BusinessException("Không tìm thấy nguyên liệu trong công thức này.");
                }
                conn.commit();
            }
            catch (SQLException e) { conn.rollback(); throw e; }
            catch (RuntimeException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    public void removeRecipeLine(int productId, int lineId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (productRecipeDao.delete(conn, lineId, productId) == 0) {
                    throw new BusinessException("Không tìm thấy nguyên liệu trong công thức này.");
                }
                conn.commit();
            }
            catch (SQLException e) { conn.rollback(); throw e; }
            catch (RuntimeException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    public List<PrepRecipe> getPrepRecipe(int preppedIngredientId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return prepRecipeDao.findByPrepped(conn, preppedIngredientId);
        }
    }

    public int addPrepRecipeLines(int preppedIngredientId, List<RecipeLineInput> inputs)
            throws SQLException {
        if (preppedIngredientId <= 0) {
            throw new BusinessException("Nguyên liệu pha sẵn không hợp lệ.");
        }
        validateInputCount(inputs);

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Ingredient prepped = ingredientDao.findById(conn, preppedIngredientId);
                if (prepped == null || !prepped.isActive()
                        || !"PREPPED".equals(prepped.getIngredientType())) {
                    throw new BusinessException(
                            "Không tìm thấy nguyên liệu pha sẵn cần tạo công thức.");
                }

                Set<Integer> existingIds = new HashSet<>();
                for (PrepRecipe line : prepRecipeDao.findByPrepped(conn, preppedIngredientId)) {
                    existingIds.add(line.getRawIngredientId());
                }

                Set<Integer> submittedIds = new HashSet<>();
                for (RecipeLineInput input : inputs) {
                    Ingredient raw = ingredientDao.findById(conn, input.ingredientId());
                    if (raw == null || !raw.isActive() || !"RAW".equals(raw.getIngredientType())) {
                        throw new BusinessException(
                                "Nguyên liệu thô đã chọn không tồn tại hoặc đã ngừng hoạt động.");
                    }
                    if (!submittedIds.add(input.ingredientId())) {
                        throw new BusinessException(
                                "Không được chọn trùng nguyên liệu trong cùng một công thức.");
                    }
                    if (existingIds.contains(input.ingredientId())) {
                        throw new BusinessException(
                                "Nguyên liệu \"" + raw.getName() + "\" đã có trong công thức.");
                    }
                    validateQuantity(input.quantity());
                }

                for (RecipeLineInput input : inputs) {
                    PrepRecipe recipe = new PrepRecipe();
                    recipe.setPreppedIngredientId(preppedIngredientId);
                    recipe.setRawIngredientId(input.ingredientId());
                    recipe.setQuantity(input.quantity());
                    recipe.setYieldQty(BigDecimal.ONE);
                    prepRecipeDao.insert(conn, recipe);
                }
                conn.commit();
                return inputs.size();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } catch (RuntimeException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public void updatePrepRecipeLine(int preppedIngredientId, int lineId,
                                     BigDecimal quantityPerUnit) throws SQLException {
        validateQuantity(quantityPerUnit);
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (prepRecipeDao.updateQuantity(
                        conn, lineId, preppedIngredientId, quantityPerUnit) == 0) {
                    throw new BusinessException("Không tìm thấy nguyên liệu trong công thức này.");
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } catch (RuntimeException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public void removePrepRecipeLine(int preppedIngredientId, int lineId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (prepRecipeDao.delete(conn, lineId, preppedIngredientId) == 0) {
                    throw new BusinessException("Không tìm thấy nguyên liệu trong công thức này.");
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } catch (RuntimeException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    private void validateInputCount(List<RecipeLineInput> inputs) {
        if (inputs == null || inputs.isEmpty()) {
            throw new BusinessException("Vui lòng chọn ít nhất một nguyên liệu.");
        }
        if (inputs.size() > 100) {
            throw new BusinessException("Mỗi lần chỉ được thêm tối đa 100 nguyên liệu.");
        }
    }

    private void validateQuantity(BigDecimal quantity) {
        if (quantity == null || quantity.compareTo(BigDecimal.ONE) <= 0) {
            throw new BusinessException("Định mức nguyên liệu phải là số nguyên lớn hơn 1.");
        }
        if (quantity.stripTrailingZeros().scale() > 0) {
            throw new BusinessException("Định mức nguyên liệu không được có phần thập phân.");
        }
        if (quantity.compareTo(MAX_QUANTITY) > 0) {
            throw new BusinessException("Định mức nguyên liệu vượt quá giới hạn cho phép.");
        }
    }
}
