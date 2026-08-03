package com.cafe.web.renderer;

import com.cafe.model.ChainSummary;
import com.cafe.model.ReportRow;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Dựng file CSV doanh thu; Controller không chứa format/nhãn/escape. */
public final class ReportCsvRenderer {
    public void render(HttpServletResponse response, LocalDate from, LocalDate to,
                       ChainSummary summary, List<ReportRow> byBranch, List<ReportRow> byMethod,
                       List<ReportRow> top, List<ReportRow> daily) throws IOException {
        response.setContentType("text/csv; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"doanh-thu_" + from + "_" + to + ".csv\"");
        PrintWriter writer = response.getWriter();
        writer.write('\ufeff');
        writer.println("BÁO CÁO DOANH THU TOÀN CHUỖI");
        writer.println("Từ ngày," + from + ",Đến ngày," + to);
        writer.println();
        writer.println("TỔNG HỢP KỲ");
        writer.println("Doanh thu," + money(summary.getRevenue()));
        writer.println("Số hoá đơn," + summary.getPaidBills());
        writer.println("Tổng giảm giá," + money(summary.getDiscount()));
        writer.println("Tổng VAT," + money(summary.getVat()));
        section(writer, "DOANH THU THEO CHI NHÁNH", "Chi nhánh,Số HĐ,Doanh thu", byBranch, false);
        section(writer, "THEO HÌNH THỨC THANH TOÁN", "Hình thức,Số HĐ,Doanh thu", byMethod, true);
        section(writer, "TOP SẢN PHẨM", "Sản phẩm,Số lượng,Doanh thu", top, false);
        section(writer, "DOANH THU THEO NGÀY", "Ngày,Số HĐ,Doanh thu", daily, false);
        writer.flush();
    }

    private void section(PrintWriter writer, String title, String header,
                         List<ReportRow> rows, boolean paymentMethod) {
        writer.println();
        writer.println(title);
        writer.println(header);
        for (ReportRow row : rows) {
            String label = paymentMethod ? method(row.getLabel()) : row.getLabel();
            writer.println(csv(label) + "," + row.getCount() + "," + money(row.getAmount()));
        }
    }

    private String money(BigDecimal value) {
        return value == null ? "0" : value.toBigInteger().toString();
    }

    private String method(String value) {
        if ("CASH".equals(value)) return "Tiền mặt";
        if ("TRANSFER".equals(value)) return "Chuyển khoản";
        if ("QR_BANK".equals(value)) return "QR ngân hàng";
        return value == null ? "?" : value;
    }

    static String csv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n"))
            return "\"" + value.replace("\"", "\"\"") + "\"";
        return value;
    }
}
