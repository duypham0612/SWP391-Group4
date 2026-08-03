package com.cafe.service.cashier;

import com.cafe.common.*;
import com.cafe.config.Tx;
import com.cafe.model.*;

import java.sql.SQLException;
import java.util.List;

/** Huỷ bill và nhả các dòng món trong cùng transaction. */
public final class BillVoidService {
    private final BillingRepository repository;

    public BillVoidService() { this(new BillingRepository()); }
    BillVoidService(BillingRepository repository) {
        this.repository = java.util.Objects.requireNonNull(repository);
    }

    /** Huỷ bill chưa thanh toán KÈM LÝ DO — ghi log qua ops.OutboxEvent trong cùng tx. */
    public boolean voidBill(int billId, int branchId, String reason, Integer userId) throws SQLException {
        return Tx.call(c -> {
            Bill b = repository.billDao.findById(c, billId);
            if (b == null || b.getBranchId() != branchId) {
                return false;
            }
            // Ghi lại danh sách món TRƯỚC khi nhả, để event vẫn truy vết được bill đã huỷ gồm gì.
            List<BillLine> released = repository.billLineDao.findByBill(c, billId);
            int r = repository.billDao.markVoid(c, billId);
            if (r > 0) {
                // Nhả cặp BillId/BilledAmount trong CÙNG transaction để dòng có thể lên bill mới.
                repository.billLineDao.deleteByBill(c, billId);

                String safeReason = reason == null ? "" : reason.replace("\"", "'");
                StringBuilder items = new StringBuilder("[");
                for (int i = 0; i < released.size(); i++) {
                    if (i > 0) items.append(',');
                    items.append(released.get(i).getOrderItemId());
                }
                items.append(']');
                repository.outboxEventDao.insert(c, EventType.BILL_VOIDED, String.valueOf(billId), b.getBranchId(),
                        "{\"billId\":" + billId + ",\"by\":" + userId + ",\"reason\":\"" + safeReason + "\""
                        + ",\"releasedOrderItemIds\":" + items + "}");
            }
            return r > 0;
        });
    }


}
