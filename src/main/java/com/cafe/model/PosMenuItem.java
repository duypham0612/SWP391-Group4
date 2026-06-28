package com.cafe.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Một món trên màn POS: giá hiệu lực + các nhóm modifier áp dụng. */
public class PosMenuItem {
    private int productId;
    private String name;
    private BigDecimal price;          // localPrice nếu có, ngược lại basePrice
    private List<Group> groups = new ArrayList<>();

    public int getProductId() { return productId; }
    public void setProductId(int v) { this.productId = v; }

    public String getName() { return name; }
    public void setName(String v) { this.name = v; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal v) { this.price = v; }

    public List<Group> getGroups() { return groups; }
    public void setGroups(List<Group> v) { this.groups = v; }

    /** Nhóm modifier + options. */
    public static class Group {
        private int groupId;
        private String name;
        private boolean required;
        private int minSelect;
        private int maxSelect;
        private List<ModifierOption> options = new ArrayList<>();

        public int getGroupId() { return groupId; }
        public void setGroupId(int v) { this.groupId = v; }
        public String getName() { return name; }
        public void setName(String v) { this.name = v; }
        public boolean isRequired() { return required; }
        public void setRequired(boolean v) { this.required = v; }
        public int getMinSelect() { return minSelect; }
        public void setMinSelect(int v) { this.minSelect = v; }
        public int getMaxSelect() { return maxSelect; }
        public void setMaxSelect(int v) { this.maxSelect = v; }
        public List<ModifierOption> getOptions() { return options; }
        public void setOptions(List<ModifierOption> v) { this.options = v; }
    }
}
