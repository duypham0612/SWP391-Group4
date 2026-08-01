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

    private static BillLine item(String status) {
        BillLine item = new BillLine();
        item.setStatus(status);
        return item;
    }
}
