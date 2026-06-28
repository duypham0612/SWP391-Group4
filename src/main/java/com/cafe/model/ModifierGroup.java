package com.cafe.model;

/** catalog.ModifierGroup (Size, Đường, Sữa, Topping...) */
public class ModifierGroup {
    private int modifierGroupId;
    private String name;
    private boolean required;
    private int minSelect;
    private int maxSelect = 1;

    public int getModifierGroupId() { return modifierGroupId; }
    public void setModifierGroupId(int modifierGroupId) { this.modifierGroupId = modifierGroupId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }

    public int getMinSelect() { return minSelect; }
    public void setMinSelect(int minSelect) { this.minSelect = minSelect; }

    public int getMaxSelect() { return maxSelect; }
    public void setMaxSelect(int maxSelect) { this.maxSelect = maxSelect; }
}
