package com.cafe.common;

/**
 * ★ Quy tắc giữ nguyên liệu khi làm lại món — logic THUẦN, không DB → unit-test trước.
 *
 * Mỗi lần làm lại ghi MỘT dòng WASTE bằng đúng định mức một lượt pha. Dòng đó thuộc về lượt nào
 * phụ thuộc việc lượt VỪA BỎ đã được ghi sổ hay chưa:
 * <ul>
 *   <li>đã ghi sổ — bỏ từ READY (đã có DEDUCT lúc bấm Xong), hoặc lượt trước đó cũng là làm lại
 *       và đã giữ chỗ sẵn → dòng WASTE này giữ chỗ cho lượt pha KẾ TIẾP, lần bấm Xong sau
 *       KHÔNG trừ nữa (cờ = true).</li>
 *   <li>chưa ghi sổ — bỏ từ MAKING khi chưa hề trừ gì → dòng WASTE này chính là sổ của lượt vừa
 *       bỏ, lượt pha lại vẫn phải trừ bình thường (cờ = false).</li>
 * </ul>
 *
 * Bất biến: tổng lượng ghi sổ luôn bằng số lượt pha thực tế đã dùng nguyên liệu. Trước đây cờ
 * được bật vô điều kiện nên làm lại từ MAKING bị trừ thiếu đúng một lượt pha.
 */
public final class RemakeReservation {
    private RemakeReservation() {}

    /**
     * @param fromReady       lượt bỏ đang ở READY (đã trừ kho) thay vì MAKING
     * @param alreadyReserved cờ RemakeInventoryReserved TRƯỚC lần làm lại này
     * @return true nếu dòng WASTE vừa ghi giữ chỗ cho lượt pha kế tiếp
     */
    public static boolean reservesNextPour(boolean fromReady, boolean alreadyReserved) {
        return fromReady || alreadyReserved;
    }
}
