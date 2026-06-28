package com.cafe.model;

import java.math.BigDecimal;

/** sales.OrderItemModifier — option khách chọn cho 1 dòng đơn (đầu vào auto-deduct). */
public class OrderItemModifier {
    private int orderItemModifierId;
    private int orderItemId;
    private int modifierOptionId;
    private BigDecimal priceDelta;

    // join
    private String optionName;

    public int getOrderItemModifierId() { return orderItemModifierId; }
    public void setOrderItemModifierId(int v) { this.orderItemModifierId = v; }

    public int getOrderItemId() { return orderItemId; }
    public void setOrderItemId(int v) { this.orderItemId = v; }

    public int getModifierOptionId() { return modifierOptionId; }
    public void setModifierOptionId(int v) { this.modifierOptionId = v; }

    public BigDecimal getPriceDelta() { return priceDelta; }
    public void setPriceDelta(BigDecimal v) { this.priceDelta = v; }

    public String getOptionName() { return optionName; }
    public void setOptionName(String v) { this.optionName = v; }
}
