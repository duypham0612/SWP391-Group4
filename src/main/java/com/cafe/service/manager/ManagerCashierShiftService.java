package com.cafe.service.manager;

import com.cafe.common.BusinessException;
import com.cafe.common.EventPublisher;
import com.cafe.common.EventType;
import com.cafe.config.DBConnection;
import com.cafe.dao.cashier.CashierShiftDao;
import com.cafe.model.CashierShift;
import com.cafe.service.cashier.CashierCashReconciliation;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/** Manager xử lý các két thu ngân bị bỏ quên mà không tự thay đổi bản chấm công. */
public class ManagerCashierShiftService {

    private static final int MAX_REASON_LENGTH = 255;

    private final CashierShiftDao shiftDao = new CashierShiftDao();

    public List<CashierShift> getOpenShifts(int branchId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            List<CashierShift> shifts = shiftDao.findOpenByBranch(c, branchId);
            for (CashierShift shift : shifts) shiftDao.fillReport(c, shift);
            return shifts;
        }
    }

    /**
     * Đóng két bỏ quên bằng số tiền Manager thực đếm. Chênh lệch không bị chặn vì đây là
     * đường phục hồi; expected/actual/variance và lý do được ghi audit trong cùng transaction.
     */
    public ForceCloseResult forceClose(int branchId, int managerId, int shiftId,
                                       BigDecimal actualClosingCash, String reason)
            throws SQLException {
        CashierCashReconciliation.requireValidMoney(actualClosingCash, "Tiền mặt thực đếm");
        String cleanReason = normalizeReason(reason);

        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                shiftDao.acquireBranchOpenLock(c, branchId);
                CashierShift shift = shiftDao.findOpenByIdForUpdate(c, shiftId, branchId);
                if (shift == null) {
                    throw new BusinessException("Ca thu ngân không còn mở hoặc không thuộc chi nhánh hiện tại.");
                }

                BigDecimal cashRevenue = shiftDao.sumPaidCashForClose(c, shiftId);
                BigDecimal expected = CashierCashReconciliation.expectedClosingCash(
                        shift.getOpeningCash(), cashRevenue);
                BigDecimal variance = actualClosingCash.subtract(expected);

                if (shiftDao.close(c, shiftId, actualClosingCash) != 1) {
                    throw new BusinessException("Ca thu ngân vừa được kết bởi thao tác khác.");
                }

                String payload = "{\"cashierShiftId\":" + shiftId
                        + ",\"cashierId\":" + shift.getCashierId()
                        + ",\"closedBy\":" + managerId
                        + ",\"expectedCash\":" + expected.toPlainString()
                        + ",\"actualCash\":" + actualClosingCash.toPlainString()
                        + ",\"variance\":" + variance.toPlainString()
                        + ",\"reason\":\"" + jsonEscape(cleanReason) + "\"}";
                EventPublisher.publish(c, EventType.CASHIER_SHIFT_FORCE_CLOSED,
                        String.valueOf(shiftId), branchId, payload);

                c.commit();
                return new ForceCloseResult(shiftId, expected, actualClosingCash, variance);
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    private String normalizeReason(String reason) {
        String clean = reason == null ? "" : reason.trim();
        if (clean.isEmpty()) throw new BusinessException("Phải nhập lý do khi Quản lý kết ca hộ.");
        if (clean.length() > MAX_REASON_LENGTH) {
            throw new BusinessException("Lý do kết ca hộ không được vượt quá 255 ký tự.");
        }
        return clean;
    }

    private String jsonEscape(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", " ")
                .replace("\n", " ");
    }

    public record ForceCloseResult(int shiftId, BigDecimal expectedCash,
                                   BigDecimal actualCash, BigDecimal variance) {
    }
}
