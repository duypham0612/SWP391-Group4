package com.cafe.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BaristaOpsSnapshotTest {
    @Test
    void formats_average_preparation_time_without_ranking() {
        BaristaOpsSnapshot snapshot = new BaristaOpsSnapshot();
        assertEquals("Chưa có dữ liệu", snapshot.getMyAveragePreparationDisplay());
        snapshot.setMyAveragePreparationSeconds(125);
        assertEquals("2 phút 5 giây", snapshot.getMyAveragePreparationDisplay());
    }

    @Test
    void clamps_negative_aggregate_values() {
        BaristaOpsSnapshot snapshot = new BaristaOpsSnapshot();
        snapshot.setMyMakingCups(-1);
        snapshot.setBranchBlockedCups(-3);
        assertEquals(0, snapshot.getMyMakingCups());
        assertEquals(0, snapshot.getBranchBlockedCups());
    }
}
