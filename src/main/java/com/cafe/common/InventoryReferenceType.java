package com.cafe.common;

/** Các loại chứng từ hợp lệ được sổ cái tồn kho tham chiếu. */
public enum InventoryReferenceType {
    STOCK_RECEIPT_LINE,
    ORDER_ITEM,
    PREP_BATCH,
    WASTE_ENTRY,
    STOCK_ADJUSTMENT
}
