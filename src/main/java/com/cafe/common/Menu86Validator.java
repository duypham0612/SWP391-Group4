package com.cafe.common;

import java.time.LocalDateTime;

/**
 * ★ Validate form "Báo tạm hết món" — logic thuần (không DB) để dễ unit-test.
 *
 * <p>Ba ràng buộc bắt buộc: <b>lý do</b> (chọn từ {@link Reason86}) + <b>ghi chú</b>
 * (bắt buộc khi lý do là "Khác") + <b>dự kiến có lại</b> (phải ở tương lai, trong khung cho phép).
 * Chặn ở server vì {@code required} phía HTML bỏ qua được dễ dàng.
 *
 * <p>Thứ tự kiểm: lý do → ghi chú → thời gian. Sai nhiều chỗ thì báo lỗi đầu tiên theo thứ tự này,
 * để thông báo ổn định (không đổi theo thứ tự trường trên form).
 *
 * <p>{@code now} truyền vào thay vì gọi {@code LocalDateTime.now()} bên trong — test mới kiểm được biên.
 */
public final class Menu86Validator {

    private Menu86Validator() { }

    /** Form đã sạch: lý do là enum, ghi chú đã trim (rỗng → null), thời gian đã kiểm. */
    public static final class Validated {
        private final Reason86 reason;
        private final String note;
        private final LocalDateTime backInEta;

        private Validated(Reason86 reason, String note, LocalDateTime backInEta) {
            this.reason = reason;
            this.note = note;
            this.backInEta = backInEta;
        }

        public Reason86 getReason() { return reason; }
        /** Ghi chú đã trim; {@code null} khi barista không ghi gì. */
        public String getNote() { return note; }
        public LocalDateTime getBackInEta() { return backInEta; }
    }

    /**
     * @param reasonCode mã lý do từ form (vd "INGREDIENT_OUT")
     * @param note       ghi chú (chip bấm nhanh nối chuỗi, hoặc gõ tay); có thể null
     * @param backInEta  dự kiến có lại
     * @param now        mốc thời gian hiện tại
     * @throws BusinessException khi vi phạm — message đã là chữ hiển thị được cho barista
     */
    public static Validated validate(String reasonCode, String note,
                                     LocalDateTime backInEta, LocalDateTime now) {

        Reason86 reason = Reason86.fromCode(reasonCode);
        if (reason == null) {
            throw new BusinessException("Vui lòng chọn lý do tạm hết món.");
        }

        // Chuẩn hoá NFC: bàn phím tiếng Việt macOS gõ ra dạng tổ hợp ("ầ" = 2 code point),
        // Unikey/Windows gõ ra dạng dựng sẵn (1 code point). Không gom về một dạng thì
        // cùng một câu lại đếm khác nhau tuỳ máy, và bản tổ hợp còn tràn cột NVARCHAR.
        String cleanNote = note == null ? ""
                : java.text.Normalizer.normalize(note.trim(), java.text.Normalizer.Form.NFC);

        // Tối thiểu: đếm code point — đúng số ký tự barista NHÌN THẤY.
        int visibleLen = cleanNote.codePointCount(0, cleanNote.length());
        if (reason == Reason86.OTHER && visibleLen < Constants.MENU86_OTHER_NOTE_MIN_CHARS) {
            throw new BusinessException("Chọn lý do \"Khác\" thì phải ghi rõ, tối thiểu "
                    + Constants.MENU86_OTHER_NOTE_MIN_CHARS + " ký tự.");
        }
        // Tối đa: đếm đơn vị UTF-16 — đúng cách NVARCHAR(255) đong chỗ (emoji ăn 2 đơn vị).
        if (cleanNote.length() > Constants.MENU86_NOTE_MAX_CHARS) {
            throw new BusinessException("Ghi chú tối đa "
                    + Constants.MENU86_NOTE_MAX_CHARS + " ký tự.");
        }

        if (backInEta == null) {
            throw new BusinessException("Vui lòng chọn thời gian dự kiến có lại.");
        }
        if (now == null) {
            throw new IllegalArgumentException("now must not be null");
        }
        if (!backInEta.isAfter(now)) {
            throw new BusinessException("Thời gian dự kiến có lại phải ở tương lai.");
        }
        if (backInEta.isBefore(now.plusMinutes(Constants.MENU86_ETA_MIN_MINUTES))) {
            throw new BusinessException("Thời gian dự kiến có lại phải cách hiện tại ít nhất "
                    + Constants.MENU86_ETA_MIN_MINUTES + " phút.");
        }
        if (backInEta.isAfter(now.plusDays(Constants.MENU86_ETA_MAX_DAYS))) {
            throw new BusinessException("Thời gian dự kiến có lại không được quá "
                    + Constants.MENU86_ETA_MAX_DAYS + " ngày.");
        }

        return new Validated(reason, cleanNote.isEmpty() ? null : cleanNote, backInEta);
    }
}
