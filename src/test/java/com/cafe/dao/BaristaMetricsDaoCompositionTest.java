package com.cafe.dao;

import com.cafe.dao.barista.PrepBatchDao;
import com.cafe.dao.barista.WasteEventDao;
import com.cafe.dao.shared.OrderItemQueryDao;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BaristaMetricsDaoCompositionTest {

    @Test
    void order_item_query_dao_maps_preparation_metrics() throws Exception {
        List<String> sqlSeen = new ArrayList<>();
        Connection conn = connection(sqlSeen, Map.of(
                "MyMakingCups", 2,
                "MyCompletedCups", 7,
                "MyAvgPrepSeconds", 95L,
                "WaitingCups", 4,
                "MakingCups", 3,
                "ReadyCups", 5,
                "BlockedCups", 1));

        OrderItemQueryDao.PreparationMetrics metrics = new OrderItemQueryDao().loadPreparationMetrics(
                conn, 8, 21, LocalDateTime.of(2026, 7, 26, 0, 0));

        assertEquals(2, metrics.myMakingCups());
        assertEquals(7, metrics.myCompletedCups());
        assertEquals(95, metrics.myAveragePreparationSeconds());
        assertEquals(4, metrics.branchWaitingCups());
        assertEquals(3, metrics.branchMakingCups());
        assertEquals(5, metrics.branchReadyCups());
        assertEquals(1, metrics.branchBlockedCups());
        assertTrue(sqlSeen.get(0).contains("FROM sales.OrderItem"));
        assertTrue(sqlSeen.get(0).contains("o.Status='ACTIVE' AND oi.Status='WAITING'"));
    }

    @Test
    void waste_event_dao_maps_event_metrics() throws Exception {
        List<String> sqlSeen = new ArrayList<>();
        Connection conn = connection(sqlSeen, Map.of(
                "MyRemakes", 1,
                "MyWastes", 3,
                "BranchRemakes", 6));

        WasteEventDao.EventMetrics metrics = new WasteEventDao().loadMetrics(
                conn, 8, 21, LocalDateTime.of(2026, 7, 26, 0, 0));

        assertEquals(1, metrics.myRemakes());
        assertEquals(3, metrics.myIngredientWastes());
        assertEquals(6, metrics.branchRemakes());
        assertTrue(sqlSeen.get(0).contains("FROM inventory.WasteEntry"));
    }

    @Test
    void prep_batch_dao_owns_expired_count() throws Exception {
        List<String> sqlSeen = new ArrayList<>();
        Connection conn = connection(sqlSeen, Map.of(1, 4));

        assertEquals(4, new PrepBatchDao().countExpiredActive(conn, 8));
        assertTrue(sqlSeen.get(0).contains("FROM inventory.PrepBatch"));
    }

    private static Connection connection(List<String> sqlSeen, Map<?, ? extends Number> row) {
        boolean[] first = {true};
        ResultSet resultSet = (ResultSet) Proxy.newProxyInstance(
                BaristaMetricsDaoCompositionTest.class.getClassLoader(),
                new Class<?>[]{ResultSet.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("next")) {
                        boolean value = first[0];
                        first[0] = false;
                        return value;
                    }
                    if (method.getName().equals("getInt")) {
                        Number value = row.get(args[0]);
                        return value == null ? 0 : value.intValue();
                    }
                    if (method.getName().equals("getLong")) {
                        Number value = row.get(args[0]);
                        return value == null ? 0L : value.longValue();
                    }
                    return defaultValue(method.getReturnType());
                });
        PreparedStatement statement = (PreparedStatement) Proxy.newProxyInstance(
                BaristaMetricsDaoCompositionTest.class.getClassLoader(),
                new Class<?>[]{PreparedStatement.class},
                (proxy, method, args) -> method.getName().equals("executeQuery")
                        ? resultSet : defaultValue(method.getReturnType()));
        return (Connection) Proxy.newProxyInstance(
                BaristaMetricsDaoCompositionTest.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("prepareStatement")) {
                        sqlSeen.add((String) args[0]);
                        return statement;
                    }
                    return defaultValue(method.getReturnType());
                });
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0F;
        if (type == double.class) return 0D;
        if (type == char.class) return '\0';
        return null;
    }
}
