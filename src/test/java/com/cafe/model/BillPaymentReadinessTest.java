package com.cafe.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BillPaymentReadinessTest {

    @Test
    void allowsPaymentOnlyWhenEveryBilledItemWasServed() {
        Bill bill = new Bill();
        bill.setItems(List.of(item("SERVED"), item("SERVED")));
        assertTrue(bill.isReadyForPayment());

        bill.setItems(List.of(item("SERVED"), item("PICKED_UP")));
        assertFalse(bill.isReadyForPayment());

        bill.setItems(List.of(item("READY")));
        assertFalse(bill.isReadyForPayment());

        bill.setItems(List.of());
        assertFalse(bill.isReadyForPayment());
    }

    private static BillItem item(String status) {
        BillItem item = new BillItem();
        item.setStatus(status);
        return item;
    }
}
