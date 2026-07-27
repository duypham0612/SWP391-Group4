package com.cafe.service.manager;

import com.cafe.common.BusinessException;
import com.cafe.model.Supplier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SupplierServiceTest {

    @Test
    void acceptsExactlyTenDigitsStartingWithZero() {
        assertDoesNotThrow(() -> SupplierService.validate(supplier("0338025819")));
    }

    @Test
    void rejectsWrongPrefixLengthAndNonDigits() {
        assertThrows(BusinessException.class, () -> SupplierService.validate(supplier("1338025819")));
        assertThrows(BusinessException.class, () -> SupplierService.validate(supplier("033802581")));
        assertThrows(BusinessException.class, () -> SupplierService.validate(supplier("03380258190")));
        assertThrows(BusinessException.class, () -> SupplierService.validate(supplier("03A8025819")));
    }

    private Supplier supplier(String phone) {
        Supplier supplier = new Supplier();
        supplier.setPhone(phone);
        return supplier;
    }
}
