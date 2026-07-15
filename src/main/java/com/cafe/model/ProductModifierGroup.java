package com.cafe.model;

/** catalog.ProductModifierGroup — gán 1 nhóm modifier cho 1 product. */
public class ProductModifierGroup {
    private int productId;
    private int modifierGroupId;
    private String groupName; // join

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public int getModifierGroupId() { return modifierGroupId; }
    public void setModifierGroupId(int modifierGroupId) { this.modifierGroupId = modifierGroupId; }

    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
}
