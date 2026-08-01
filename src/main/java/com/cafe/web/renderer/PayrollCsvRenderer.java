package com.cafe.web.renderer;

import com.cafe.model.PayrollRow;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;

/** Dựng CSV bảng lương. */
public final class PayrollCsvRenderer {
    public void render(HttpServletResponse response, YearMonth month, List<PayrollRow> rows) throws IOException {
        response.setContentType("text/csv; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"payroll-" + month + ".csv\"");
        PrintWriter writer = response.getWriter();
        writer.write('\ufeff');
        writer.println("Mã NV,Họ tên,Vai trò,Số ca duyệt,Tổng giờ,Lương/giờ,Thành tiền");
        for (PayrollRow row : rows) {
            writer.println(row.getUserId() + "," + ReportCsvRenderer.csv(row.getUserName()) + ","
                    + ReportCsvRenderer.csv(row.getRoleName()) + "," + row.getApprovedShifts() + ","
                    + String.format(Locale.US, "%.2f", row.getTotalHours()) + ","
                    + row.getHourlyRate().toPlainString() + "," + row.getSalary().toPlainString());
        }
        writer.flush();
    }
}
