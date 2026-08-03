package com.cafe.common;

import java.math.BigDecimal;
import java.util.List;

/** Fixed Size, Sugar and Ice choices shared by Admin, POS and QR ordering. */
public final class StandardModifierPolicy {
    public static final String SIZE_S = "Size S";
    public static final String SIZE_M = "Size M";
    public static final String SIZE_L = "Size L";

    public static final String NORMAL = "B\u00ecnh th\u01b0\u1eddng";

    public static final BigDecimal FREE = new BigDecimal("0.00");
    public static final BigDecimal SIZE_M_DELTA = new BigDecimal("6000.00");
    public static final BigDecimal SIZE_L_DELTA = new BigDecimal("10000.00");

    public static final List<String> SIZE_OPTIONS = List.of(SIZE_S, SIZE_M, SIZE_L);
    public static final List<String> SUGAR_OPTIONS = List.of(
            "Kh\u00f4ng \u0111\u01b0\u1eddng", "\u00cdt \u0111\u01b0\u1eddng", NORMAL, "Nhi\u1ec1u \u0111\u01b0\u1eddng");
    public static final List<String> ICE_OPTIONS = List.of(
            "Kh\u00f4ng \u0111\u00e1", "\u00cdt \u0111\u00e1", NORMAL, "Nhi\u1ec1u \u0111\u00e1");

    private StandardModifierPolicy() { }

    public static BigDecimal priceDelta(String groupName, String optionName) {
        if (ModifierGroupNames.isSize(groupName)) {
            if (SIZE_M.equals(optionName)) return SIZE_M_DELTA;
            if (SIZE_L.equals(optionName)) return SIZE_L_DELTA;
        }
        return FREE;
    }

    public static boolean isDefault(String groupName, String optionName) {
        return ModifierGroupNames.isSize(groupName) && SIZE_S.equals(optionName)
                || (ModifierGroupNames.SUGAR.equals(groupName)
                    || ModifierGroupNames.ICE.equals(groupName)) && NORMAL.equals(optionName);
    }
}
