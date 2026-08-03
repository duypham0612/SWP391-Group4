package com.cafe.web.viewmodel;

import com.cafe.common.BusinessDay;
import com.cafe.common.Constants;
import com.cafe.common.QuantityFormat;
import com.cafe.common.Reason86;
import com.cafe.common.StandardModifierPolicy;
import com.cafe.model.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Locale;
import java.util.StringJoiner;

/**
 * Presenter dùng chung cho JSP. Lớp này là biên ViewModel: domain chỉ cung cấp dữ liệu thô,
 * còn tiếng Việt, múi giờ, format số và CSS token đều nằm ở tầng web.
 */
public final class ViewFormatter {
    private static final Locale VI = Locale.forLanguageTag("vi-VN");
    private static final DateTimeFormatter SHORT_UTC = DateTimeFormatter.ofPattern("dd/MM HH:mm");
    private static final DateTimeFormatter TIME_DATE_UTC = DateTimeFormatter.ofPattern("HH:mm dd/MM");
    private static final DateTimeFormatter DAY_MONTH = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    public String plain(BigDecimal value) { return QuantityFormat.plain(value); }
    public String grouped(BigDecimal value) { return QuantityFormat.groupedVi(value); }
    public String integer(BigDecimal value) {
        return value == null ? "" : value.setScale(0, RoundingMode.DOWN).toPlainString();
    }
    public String money(BigDecimal value) { return QuantityFormat.groupedVi(value); }
    public boolean modifierDefault(String groupName, String optionName) {
        return StandardModifierPolicy.isDefault(groupName, optionName);
    }

    public String shortUtc(LocalDateTime value) { return utc(value, SHORT_UTC); }
    public String timeDateUtc(LocalDateTime value) { return utc(value, TIME_DATE_UTC); }
    public String fullUtc(LocalDateTime value) { return BusinessDay.fmtFullDateTimeVn(value); }
    public String dateTimeUtc(LocalDateTime value) { return BusinessDay.fmtDateTimeVn(value); }
    public String timeUtc(LocalDateTime value) { return BusinessDay.fmtTimeVn(value); }
    public String localDate(LocalDate value) { return value == null ? "" : value.format(DAY_MONTH); }
    public String timeRange(LocalTime start, LocalTime end) {
        return start == null || end == null ? "" : start.format(TIME) + " - " + end.format(TIME);
    }
    public String oneDecimal(double value) { return String.format(Locale.US, "%.1f", value); }

    public String menuReason(String reason) {
        Reason86 value = Reason86.fromCode(reason);
        return value == null ? "" : value.label();
    }
    public String menuStatus(String status) {
        if ("PENDING".equals(status)) return "Chờ quản lý duyệt";
        if ("APPROVED".equals(status)) return "Đã duyệt tạm hết";
        if ("REJECTED".equals(status)) return "Đã từ chối";
        if ("RESOLVED".equals(status)) return "Đã mở bán lại";
        return "";
    }

    public String attendanceState(MonthlyAttendanceRow row) {
        if (row == null) return "";
        if (row.isAbsent()) return "Vắng";
        if (row.isOpen()) return "Chưa tan ca";
        if ("APPROVED".equals(row.getStatus())) return "Đã duyệt";
        if ("REJECTED".equals(row.getStatus())) return "Bị từ chối";
        return "Chờ duyệt";
    }
    public String attendanceBadge(MonthlyAttendanceRow row) {
        if (row == null || row.isAbsent()) return "badge-served";
        if (row.isOpen()) return "badge-waiting";
        if ("APPROVED".equals(row.getStatus())) return "badge-ready";
        if ("REJECTED".equals(row.getStatus())) return "badge-cancelled";
        return "badge-waiting";
    }
    public String attendanceHours(MonthlyAttendanceRow row) {
        return row == null || row.isAbsent() || row.isOpen() ? "-" : oneDecimal(row.getWorkHours());
    }

    public String shiftStatus(ShiftClockStatus status) {
        if (status == null || !status.isHasAssignment()) return "Hôm nay bạn chưa được xếp ca.";
        if (status.isCanClockIn()) return "Chưa vào ca.";
        if (status.isCanClockOut()) return "Đang trong ca từ " + timeUtc(status.getCheckInAt()) + ".";
        if (status.isClockedOut()) return "Đã tan ca.";
        return "";
    }

    public String wasteType(String type) {
        if ("SPILL".equals(type)) return "Hao đổ/rơi";
        if ("EXPIRED".equals(type)) return "Hỏng / hết hạn";
        if ("REMAKE".equals(type)) return "Làm lại món";
        return "Khác";
    }
    public String wasteCost(WasteEventItem item) {
        if (item == null || item.getLineCost() == null) return "Chưa có giá";
        return NumberFormat.getNumberInstance(VI).format(
                item.getLineCost().setScale(0, RoundingMode.HALF_UP)) + " đ";
    }
    public String costBasis(String basis) {
        if ("SNAPSHOT".equals(basis)) return "Đã chốt";
        if ("UNAVAILABLE".equals(basis)) return "Chưa có giá";
        return "Ước tính dữ liệu cũ";
    }
    public String wasteSource(String source) { return "KDS".equals(source) ? "KDS" : "Thủ công"; }
    public String reviewType(String type) {
        if ("SOFT_NEGATIVE".equals(type)) return "Tồn âm nhẹ";
        if ("HARD_NEGATIVE".equals(type)) return "Tồn âm vượt ngưỡng";
        if ("LATE_CORRECTION".equals(type)) return "Đính chính trễ";
        if ("MANAGER_VOID".equals(type)) return "Quản lý huỷ dòng";
        return "Cần kiểm tra";
    }
    public String auditAction(String type) {
        if ("CREATE".equals(type)) return "Ghi mới";
        if ("UPDATE".equals(type)) return "Sửa số lượng";
        if ("VOID".equals(type)) return "Huỷ dòng";
        if ("REVIEW".equals(type)) return "Đối soát";
        return type == null ? "" : type;
    }
    public String auditChange(String before, String after) {
        String from = numberText(before);
        String to = numberText(after);
        if (!from.isEmpty() && !to.isEmpty()) return from + " → " + to;
        return !to.isEmpty() ? to : from;
    }

    public String orderType(String type) { return "TAKEAWAY".equals(type) ? "Mang đi" : "Tại bàn"; }
    public String stockMessage(PosMenuItem item) {
        if (item == null) return "";
        if ("EIGHTY_SIX".equals(item.getAvailabilityState())) return "Tạm ngừng bán";
        if (ProductStockStatus.OUT.equals(item.getAvailabilityState())) {
            return "Hết " + ingredientNames(item.getOutIngredients());
        }
        if (ProductStockStatus.LOW.equals(item.getAvailabilityState())) {
            return "Sắp hết " + ingredientNames(item.getLowIngredients());
        }
        return "";
    }

    private static String ingredientNames(Collection<String> names) {
        return names == null || names.isEmpty() ? "nguyên liệu" : String.join(", ", names);
    }

    public String destination(OrderGroupInfo info) {
        if (info == null) return "";
        return info.getTableNumber() == null || info.getTableNumber().isBlank()
                ? orderType(info.getOrderType()) : info.getTableNumber();
    }
    public String pickupCodes(Collection<String> codes) {
        StringJoiner joiner = new StringJoiner(" · ");
        if (codes != null) for (String code : codes) if (code != null && !code.isBlank()) joiner.add(code);
        return joiner.toString();
    }
    public String durationMinutes(Integer seconds) {
        if (seconds == null) return "";
        int minutes = Math.max(0, seconds) / 60;
        if (minutes < 120) return minutes + " phút";
        int hours = minutes / 60;
        int rest = minutes % 60;
        return rest == 0 ? hours + " tiếng" : hours + " tiếng " + rest + " phút";
    }
    public String serveTier(Integer seconds) {
        if (seconds == null) return "ok";
        if (seconds >= Constants.PICKUP_CRIT_SECONDS) return "crit";
        if (seconds >= Constants.PICKUP_WARN_SECONDS) return "warn";
        return "ok";
    }
    public String slaTier(OrderItem item) {
        if (item == null) return "ok";
        if (item.isHasIssue()) return "blocked";
        int prep = effectivePrepSeconds(item);
        if (item.getWaitedSeconds() >= prep) return "late";
        if (item.getWaitedSeconds() >= prep * 2 / 3) return "warn";
        return "ok";
    }
    public String slaLabel(OrderItem item) {
        if (item == null) return "";
        if (item.isOvernight()) return "Trễ từ hôm qua";
        int remaining = effectivePrepSeconds(item) - item.getWaitedSeconds();
        if (remaining <= 0 && remaining > -60) return "Vừa quá hạn";
        if (remaining <= 0) return "Trễ " + durationMinutes(-remaining);
        if (remaining < 60) return "Sắp hết giờ";
        return "Còn " + durationMinutes(remaining);
    }
    public String waitProgress(OrderItem item) {
        if (item == null) return "";
        return "Đã chờ " + Math.max(0, item.getWaitedSeconds()) / 60
                + "/" + effectivePrepSeconds(item) / 60 + " phút";
    }

    private int effectivePrepSeconds(OrderItem item) {
        return item.getPrepSeconds() >= 60 ? item.getPrepSeconds() : Constants.KDS_SLA_SECONDS;
    }

    public String shelfLifeHours(Integer minutes) {
        if (minutes == null || minutes < 0) return "";
        return BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP)
                .stripTrailingZeros().toPlainString();
    }
    public String checklistBlockedReason(PrepChecklistRow row) {
        if (row == null) return "";
        if (row.isOversold()) return "Tồn đang âm — cần Manager kiểm kê.";
        if (!row.isHasTarget()) return "Manager chưa đặt mức tồn mục tiêu.";
        if (!row.isHasRecipe()) return "Admin chưa khai báo công thức.";
        if (!row.isHasShelfLife()) return "Admin chưa đặt hạn bảo quản.";
        return "";
    }

    public String scopeWindow(String kind, LocalDateTime fromUtc, LocalDateTime toUtc) {
        if ("TODAY".equals(kind)) return BusinessDay.todayVn().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        if ("BUSINESS_DAY".equals(kind)) return shortUtc(fromUtc) + " - " + shortUtc(toUtc);
        String from = shortUtc(fromUtc);
        return toUtc == null ? "Từ " + from : from + " - " + shortUtc(toUtc);
    }

    public String scopeLabel(String kind) {
        if ("BUSINESS_DAY".equals(kind)) return "Ngày kinh doanh này";
        if ("OPEN_SHIFT".equals(kind)) return "Ca đang mở";
        if ("CLOSED_SHIFT".equals(kind)) return "Ca vừa tan";
        return "Hôm nay";
    }

    public String dateRange(LocalDate from, LocalDate to) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        if (from == null || to == null) return "";
        return from.format(formatter) + " – " + to.format(formatter);
    }

    public String signed(BigDecimal value) {
        String formatted = plain(value);
        return value != null && value.signum() > 0 ? "+" + formatted : formatted;
    }

    private String utc(LocalDateTime value, DateTimeFormatter formatter) {
        return value == null ? "" : value.atZone(ZoneOffset.UTC)
                .withZoneSameInstant(BusinessDay.VN_ZONE).format(formatter);
    }
    private String numberText(String raw) {
        if (raw == null || raw.isBlank()) return "";
        try { return plain(new BigDecimal(raw.trim())); }
        catch (NumberFormatException e) { return raw; }
    }
}
