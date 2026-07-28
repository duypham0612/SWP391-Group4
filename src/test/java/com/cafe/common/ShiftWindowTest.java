package com.cafe.common;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Cửa sổ ca còn chấm công được — logic thuần, không đụng DB. */
class ShiftWindowTest {

    private static final LocalDate HOM_QUA = LocalDate.of(2026, 7, 24);
    private static final LocalDate HOM_NAY = LocalDate.of(2026, 7, 25);
    private static final LocalTime DEM_START = LocalTime.of(22, 0);
    private static final LocalTime DEM_END = LocalTime.of(6, 0);

    @Test
    void ca_dem_khi_gio_ket_thuc_khong_sau_gio_bat_dau() {
        assertTrue(ShiftWindow.isOvernight(DEM_START, DEM_END));
        assertTrue(ShiftWindow.isOvernight(LocalTime.of(8, 0), LocalTime.of(8, 0)));
        assertFalse(ShiftWindow.isOvernight(LocalTime.of(7, 0), LocalTime.of(12, 0)));
    }

    @Test
    void ca_dem_ket_thuc_sang_ngay_hom_sau() {
        assertEquals(LocalDateTime.of(HOM_NAY, DEM_END), ShiftWindow.scheduledEnd(HOM_QUA, DEM_START, DEM_END));
        assertEquals(LocalDateTime.of(HOM_QUA, LocalTime.of(12, 0)),
                ShiftWindow.scheduledEnd(HOM_QUA, LocalTime.of(7, 0), LocalTime.of(12, 0)));
    }

    @Test
    void ca_trong_ngay_luon_cham_cong_duoc() {
        assertTrue(ShiftWindow.isClockable(HOM_NAY, LocalTime.of(7, 0), LocalTime.of(12, 0),
                HOM_NAY, LocalDateTime.of(HOM_NAY, LocalTime.of(20, 0)), Duration.ZERO));
    }

    @Test
    void ca_dem_hom_qua_van_cham_cong_duoc_luc_ba_gio_sang() {
        assertTrue(ShiftWindow.isClockable(HOM_QUA, DEM_START, DEM_END,
                HOM_NAY, LocalDateTime.of(HOM_NAY, LocalTime.of(3, 0)), Duration.ZERO));
    }

    @Test
    void ca_dem_da_qua_gio_chi_tan_ca_duoc_trong_bien_tre() {
        LocalDateTime bayGio = LocalDateTime.of(HOM_NAY, LocalTime.of(7, 0));
        assertFalse(ShiftWindow.isClockable(HOM_QUA, DEM_START, DEM_END, HOM_NAY, bayGio, Duration.ZERO));
        assertTrue(ShiftWindow.isClockable(HOM_QUA, DEM_START, DEM_END, HOM_NAY, bayGio, ShiftWindow.CLOCK_OUT_GRACE));
    }

    @Test
    void het_bien_tre_thi_ca_dem_cu_roi_ra_khong_treo_truc_ca() {
        assertFalse(ShiftWindow.isClockable(HOM_QUA, DEM_START, DEM_END,
                HOM_NAY, LocalDateTime.of(HOM_NAY, LocalTime.of(13, 0)), ShiftWindow.CLOCK_OUT_GRACE));
    }

    @Test
    void ca_thuong_hom_qua_khong_keo_sang_hom_nay() {
        assertFalse(ShiftWindow.isClockable(HOM_QUA, LocalTime.of(7, 0), LocalTime.of(12, 0),
                HOM_NAY, LocalDateTime.of(HOM_NAY, LocalTime.of(3, 0)), ShiftWindow.CLOCK_OUT_GRACE));
    }

    @Test
    void ca_cu_hon_mot_ngay_khong_duoc_tinh() {
        assertFalse(ShiftWindow.isClockable(HOM_QUA.minusDays(1), DEM_START, DEM_END,
                HOM_NAY, LocalDateTime.of(HOM_NAY, LocalTime.of(3, 0)), ShiftWindow.CLOCK_OUT_GRACE));
    }
}
