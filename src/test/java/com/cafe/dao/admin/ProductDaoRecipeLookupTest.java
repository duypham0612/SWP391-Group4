package com.cafe.dao.admin;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regression test: productId phải tuân theo đúng phạm vi lọc của Recipe Lookup. */
class ProductDaoRecipeLookupTest {

    @Test
    void detail_query_keeps_active_branch_and_filter_scope() throws Exception {
        List<String> bindings = new ArrayList<>();
        List<String> sqlSeen = new ArrayList<>();
        ResultSet resultSet = (ResultSet) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[]{ResultSet.class},
                (proxy, method, args) -> method.getName().equals("next") ? false : defaultValue(method.getReturnType()));
        PreparedStatement statement = (PreparedStatement) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[]{PreparedStatement.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("setInt")) {
                        bindings.add(args[0] + ":int:" + args[1]);
                    } else if (method.getName().equals("setString")) {
                        bindings.add(args[0] + ":string:" + args[1]);
                    } else if (method.getName().equals("executeQuery")) {
                        return resultSet;
                    }
                    return defaultValue(method.getReturnType());
                });
        Connection connection = (Connection) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("prepareStatement")) {
                        sqlSeen.add((String) args[0]);
                        return statement;
                    }
                    return defaultValue(method.getReturnType());
                });

        assertNull(new ProductDao().findForRecipeLookupById(
                connection, 42, "Cold Brew", 3, "HAS", 7));

        String sql = sqlSeen.get(0);
        assertTrue(sql.contains("p.ProductId = ? AND p.IsActive = 1"));
        assertTrue(sql.contains("p.Name COLLATE"));
        assertTrue(sql.contains("p.CategoryId = ?"));
        assertTrue(sql.contains("EXISTS (SELECT 1 FROM catalog.ProductRecipe"));
        assertTrue(sql.contains("catalog.BranchMenu bm"));
        assertEquals(List.of(
                "1:int:42", "2:string:%Cold Brew%", "3:int:3", "4:int:7"), bindings);
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
