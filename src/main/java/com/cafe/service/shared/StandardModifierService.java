package com.cafe.service.shared;

import com.cafe.config.DBConnection;
import com.cafe.dao.shared.ProductChoiceDao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

/** Keeps fixed product choices consistent without changing the finalized schema. */
public final class StandardModifierService {
    private final ProductChoiceDao productChoiceDao;

    public StandardModifierService() {
        this(new ProductChoiceDao());
    }

    StandardModifierService(ProductChoiceDao productChoiceDao) {
        this.productChoiceDao = Objects.requireNonNull(productChoiceDao);
    }

    public int synchronizeAllProducts() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int productCount = productChoiceDao.saveStandardChoicesForAllProducts(conn);
                conn.commit();
                return productCount;
            } catch (SQLException | RuntimeException error) {
                conn.rollback();
                throw error;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }
}
