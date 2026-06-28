package com.cafe.model;

import java.math.BigDecimal;

/** catalog.ModifierOption (Size L, Oat milk, Extra shot...) */
public class ModifierOption {
    private int modifierOptionId;
    private int modifierGroupId;
    private String name;
    private BigDecimal priceDelta = BigDecimal.ZERO;
    private boolean active = true;

    public int getModifierOptionId() { return modifierOptionId; }
    public void setModifierOptionId(int modifierOptionId) { this.modifierOptionId = modifierOptionId; }

    public int getModifierGroupId() { return modifierGroupId; }
    public void setModifierGroupId(int modifierGroupId) { this.modifierGroupId = modifierGroupId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getPriceDelta() { return priceDelta; }
    public void setPriceDelta(BigDecimal priceDelta) { this.priceDelta = priceDelta; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
