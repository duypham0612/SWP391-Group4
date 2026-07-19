package com.cafe.common;

/** Loại giao dịch tồn kho — khớp ràng buộc CK_Txn_Type ở inventory.InventoryTransaction. */
public enum TxnType {
    RECEIPT,   // nhập kho (+)
    DEDUCT,    // trừ khi pha món (-)
    WASTE,     // hao hụt (-)
    PREP_IN,   // pha sẵn: cộng PREPPED (+)
    PREP_OUT,  // pha sẵn: trừ RAW (-)
    ADJUST     // điều chỉnh sau kiểm kê (+/-)
}
