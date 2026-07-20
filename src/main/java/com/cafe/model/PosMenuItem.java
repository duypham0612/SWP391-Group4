package com.cafe.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** One item on the POS/QR menu. */
public class PosMenuItem {
    private int productId;
    private String name;
    private BigDecimal price;
    private String imageUrl;
    private boolean sizeEnabled = true;
    private BigDecimal sizeSDelta = BigDecimal.ZERO;
    private BigDecimal sizeMDelta = BigDecimal.ZERO;
    private BigDecimal sizeLDelta = BigDecimal.ZERO;
    private List<Group> groups = new ArrayList<>();

    public int getProductId() { return productId; }
    public void setProductId(int v) { this.productId = v; }

    public String getName() { return name; }
    public void setName(String v) { this.name = v; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String v) { this.imageUrl = v; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal v) { this.price = v; }

    public boolean isSizeEnabled() { return sizeEnabled; }
    public void setSizeEnabled(boolean v) { this.sizeEnabled = v; }

    public BigDecimal getSizeSDelta() { return sizeSDelta; }
    public void setSizeSDelta(BigDecimal v) { this.sizeSDelta = v == null ? BigDecimal.ZERO : v; }

    public BigDecimal getSizeMDelta() { return sizeMDelta; }
    public void setSizeMDelta(BigDecimal v) { this.sizeMDelta = v == null ? BigDecimal.ZERO : v; }

    public BigDecimal getSizeLDelta() { return sizeLDelta; }
    public void setSizeLDelta(BigDecimal v) { this.sizeLDelta = v == null ? BigDecimal.ZERO : v; }

    public List<Group> getGroups() { return groups; }
    public void setGroups(List<Group> v) { this.groups = v; }

    /** Legacy modifier group shape, retained for older admin/recipe code paths. */
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
