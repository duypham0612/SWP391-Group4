package com.cafe.common;

/** Server-side validation for cashier-managed dining tables. */
public final class DiningTableValidator {

    public static final int MAX_CAPACITY = 30;

    private DiningTableValidator() { }

    public static String normalizeTableNumber(String value) {
        String normalized = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        if (normalized.isEmpty()) throw new IllegalArgumentException("Tên bàn không được để trống.");
        if (normalized.length() > 20) throw new IllegalArgumentException("Tên bàn tối đa 20 ký tự.");
        return normalized;
    }

    public static int requireCapacity(int capacity) {
        if (capacity < 1 || capacity > MAX_CAPACITY) {
            throw new IllegalArgumentException("Sức chứa bàn phải từ 1 đến " + MAX_CAPACITY + " người.");
        }
        return capacity;
    }

    public static void requireDifferentTables(int sourceTableId, int destinationTableId) {
        if (sourceTableId <= 0 || destinationTableId <= 0 || sourceTableId == destinationTableId) {
            throw new IllegalArgumentException("Hãy chọn hai bàn khác nhau để ghép.");
        }
    }
}
