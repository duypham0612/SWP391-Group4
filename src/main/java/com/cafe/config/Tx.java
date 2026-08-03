package com.cafe.config;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Chạy một khối lệnh trong đúng một giao dịch JDBC: commit nếu xuôi, rollback nếu ném lỗi, và chạy
 * lại khi SQL Server chọn nó làm nạn nhân deadlock.
 *
 * <p><b>Vì sao tồn tại:</b> trước đây mỗi service tự viết lại khuôn
 * {@code setAutoCommit(false) / commit / rollback / finally setAutoCommit(true)} — 83 chỗ trong 23
 * file. Chép tay nhiều lần như vậy có hai hậu quả đã xảy ra thật:
 * <ul>
 *   <li>Vòng thử lại deadlock từng được đặt ở {@code OrderRepository.tx}, nhưng
 *       {@code placeOrder} tự quản giao dịch riêng nên KHÔNG đi qua đó — bản sửa hụt mục tiêu mà
 *       CI vẫn xanh vài lần vì deadlock là cuộc đua.</li>
 *   <li>Chỉ cần một chỗ quên bắt {@code RuntimeException} là {@code setAutoCommit(true)} ở finally
 *       lại commit phần đã ghi dở — xem giải thích ở {@link #call}.</li>
 * </ul>
 *
 * <p>Nằm ở {@code config} chứ không phải {@code common} vì luật ArchUnit
 * {@code common_must_not_depend_on_web_or_jdbc} cấm {@code common} chạm {@code java.sql}. Đặt ở
 * {@code service} thì slice này lại thành phụ thuộc chéo giữa các gói service.
 */
public final class Tx {

    /** Mã lỗi SQL Server cho "giao dịch bị chọn làm nạn nhân deadlock". */
    private static final int SQL_SERVER_DEADLOCK_VICTIM = 1205;
    private static final int MAX_ATTEMPTS = 3;

    private Tx() {}

    public interface Block<T> { T run(Connection conn) throws SQLException; }

    public interface VoidBlock { void run(Connection conn) throws SQLException; }

    /**
     * Chạy {@code block} trong một giao dịch, commit nếu xuôi và rollback nếu ném lỗi.
     *
     * <p>BusinessException/IllegalArgumentException là RuntimeException nên PHẢI rollback cùng chỗ
     * với SQLException: bỏ sót nó thì {@code setAutoCommit(true)} ở finally lại commit phần đã ghi
     * dở (hợp đồng JDBC: đổi auto-commit mode giữa transaction sẽ commit transaction đó). Trước đây
     * món chưa có công thức vẫn sang READY rồi mới ném lỗi ở bước trừ kho → READY mà không trừ kho.
     *
     * <p>Riêng nạn nhân deadlock thì chạy lại — xem {@link #isDeadlockVictim}.
     */
    public static <T> T call(Block<T> block) throws SQLException {
        for (int attempt = 1; ; attempt++) {
            try (Connection conn = DBConnection.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    T result = block.run(conn);
                    conn.commit();
                    return result;
                } catch (SQLException | RuntimeException e) {
                    conn.rollback();
                    if (attempt >= MAX_ATTEMPTS || !isDeadlockVictim(e)) throw e;
                } finally {
                    conn.setAutoCommit(true);
                }
            }
            // Chỉ tới đây khi là nạn nhân deadlock và còn lượt: nghỉ ngắn rồi chạy lại TỪ ĐẦU
            // với connection mới.
            backOff(attempt);
        }
    }

    /** Như {@link #call} nhưng không trả kết quả. */
    public static void run(VoidBlock block) throws SQLException {
        call(conn -> { block.run(conn); return null; });
    }

    /**
     * Chạy lại cả giao dịch khi SQL Server chọn nó làm nạn nhân deadlock (lỗi 1205).
     *
     * <p>Phải chạy lại từ đầu bằng connection mới, KHÔNG thử lại trong cùng connection như vòng lặp
     * cấp mã pickup ở {@code OrderDao.insert}: 1205 huỷ nguyên giao dịch chứ không chỉ câu lệnh, nên
     * mọi thao tác đã ghi trước đó trong giao dịch cũng mất theo. Vòng lặp cũ chỉ bắt trùng khoá
     * (2601/2627) — lỗi cấp câu lệnh, giao dịch còn sống nên thử lại tại chỗ mới hợp lệ.
     *
     * <p>Chạy lại an toàn vì mọi {@code block} đều dựng lại trạng thái của nó bên trong lambda và
     * rollback đã xoá sạch phần ghi dở. Đường sinh deadlock đã biết: cấp mã pickup quét dải
     * {@code SELECT MAX(...)} lấy khoá S rồi INSERT nâng lên X.
     */
    private static boolean isDeadlockVictim(Exception error) {
        for (Throwable t = error; t != null; t = t.getCause()) {
            if (t instanceof SQLException sql) {
                for (SQLException s = sql; s != null; s = s.getNextException()) {
                    if (s.getErrorCode() == SQL_SERVER_DEADLOCK_VICTIM) return true;
                }
            }
        }
        return false;
    }

    /** Nghỉ lệch nhau giữa các luồng để lượt sau không va lại đúng như lượt trước. */
    private static void backOff(int attempt) {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextLong(10L * attempt, 40L * attempt));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
