package com.mycoffee.service;

import com.mycoffee.dao.ProductDAO;
import com.mycoffee.model.Product;
import java.util.List;

public class ProductService {
    private final ProductDAO productDAO = new ProductDAO();

    public List<Product> getAllAvailableProducts() {
        return productDAO.getAllAvailableProducts();
    }

    public List<Product> getHiddenProducts() {
        return productDAO.getHiddenProducts();
    }

    public boolean addProduct(Product p) {
        if (p.getBasePrice() < 0) return false;
        return productDAO.addProduct(p);
    }

    public boolean updateProduct(Product p) {
        if (p.getBasePrice() < 0) return false;
        return productDAO.updateProduct(p);
    }

    public boolean deleteProduct(int productId) {
        return productDAO.deleteProduct(productId);
    }

    public boolean restoreProduct(int productId) {
        return productDAO.restoreProduct(productId);
    }
}
