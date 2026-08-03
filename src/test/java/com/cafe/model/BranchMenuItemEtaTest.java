package com.cafe.model;

import org.junit.jupiter.api.Test;
import com.cafe.web.viewmodel.ViewFormatter;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BranchMenuItemEtaTest {

    @Test
    void formats_stored_utc_eta_as_vietnam_time() {
        BranchMenuItem item = new BranchMenuItem();
        item.setBackInEta(LocalDateTime.of(2026, 8, 1, 2, 45));

        assertEquals("01/08 09:45", new ViewFormatter().shortUtc(item.getBackInEta()));
    }

    @Test
    void empty_eta_has_empty_display() {
        assertEquals("", new ViewFormatter().shortUtc(new BranchMenuItem().getBackInEta()));
    }
}
