package com.cafe.model;

/** DTO tương thích cho quan hệ catalog.ModifierGroup.ProductId. */
public class ProductModifierGroup {
    private int productId;
    private int modifierGroupId;
    private String groupName; // join
    private int sortOrder;

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public int getModifierGroupId() { return modifierGroupId; }
    public void setModifierGroupId(int modifierGroupId) { this.modifierGroupId = modifierGroupId; }

    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
