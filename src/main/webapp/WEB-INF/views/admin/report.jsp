<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><div class="eyebrow">Báo cáo</div><h1>Doanh thu toàn chuỗi</h1><p>Tổng hợp doanh thu, hóa đơn và sản phẩm bán chạy.</p></div>
</div>

<div class="card-grid">
    <div class="card stat"><span class="label">Doanh thu hôm nay</span><span class="value"><fmt:formatNumber value="${summary.todayRevenue}" maxFractionDigits="0"/></span></div>
    <div class="card stat"><span class="label">Hoá đơn hôm nay</span><span class="value">${summary.todayBills}</span></div>
    <div class="card stat"><span class="label">Tổng doanh thu</span><span class="value"><fmt:formatNumber value="${summary.revenue}" maxFractionDigits="0"/></span></div>
    <div class="card stat"><span class="label">Tổng hoá đơn</span><span class="value">${summary.paidBills}</span></div>
</div>

<div style="display:flex;gap:14px;flex-wrap:wrap;margin:14px 0 24px">
    <div class="card" style="flex:1;min-width:220px"><span class="muted">Tổng giảm giá (voucher)</span><div style="font-size:1.3rem;font-weight:700"><fmt:formatNumber value="${summary.discount}" maxFractionDigits="0"/> ₫</div></div>
    <div class="card" style="flex:1;min-width:220px"><span class="muted">Tổng VAT đã thu</span><div style="font-size:1.3rem;font-weight:700"><fmt:formatNumber value="${summary.vat}" maxFractionDigits="0"/> ₫</div></div>
</div>

<div style="display:grid;grid-template-columns:1fr 1fr;gap:20px">
    <div class="card">
        <h3 style="margin-top:0">Doanh thu theo chi nhánh</h3>
        <c:choose>
            <c:when test="${empty byBranch}"><p class="muted">Chưa có dữ liệu.</p></c:when>
            <c:otherwise>
                <table class="table">
                    <thead><tr><th>Chi nhánh</th><th style="width:90px">HĐ</th><th style="width:160px">Doanh thu</th></tr></thead>
                    <tbody>
                        <c:forEach var="r" items="${byBranch}">
                            <tr><td>${r.label}</td><td>${r.count}</td><td><strong><fmt:formatNumber value="${r.amount}" maxFractionDigits="0"/> ₫</strong></td></tr>
                        </c:forEach>
                    </tbody>
                </table>
            </c:otherwise>
        </c:choose>
    </div>

    <div class="card">
        <h3 style="margin-top:0">Theo hình thức thanh toán</h3>
        <c:choose>
            <c:when test="${empty byMethod}"><p class="muted">Chưa có dữ liệu.</p></c:when>
            <c:otherwise>
                <table class="table">
                    <thead><tr><th>Hình thức</th><th style="width:90px">HĐ</th><th style="width:160px">Doanh thu</th></tr></thead>
                    <tbody>
                        <c:forEach var="r" items="${byMethod}">
                            <tr>
                                <td>
                                    <c:choose>
                                        <c:when test="${r.label == 'CASH'}">Tiền mặt</c:when>
                                        <c:when test="${r.label == 'TRANSFER'}">Chuyển khoản</c:when>
                                        <c:when test="${r.label == 'QR_BANK'}">QR ngân hàng</c:when>
                                        <c:otherwise>${r.label}</c:otherwise>
                                    </c:choose>
                                </td>
                                <td>${r.count}</td>
                                <td><strong><fmt:formatNumber value="${r.amount}" maxFractionDigits="0"/> ₫</strong></td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </c:otherwise>
        </c:choose>
    </div>
</div>

<div class="card" style="margin-top:20px">
    <h3 style="margin-top:0">Top sản phẩm bán chạy</h3>
    <c:choose>
        <c:when test="${empty topProducts}"><p class="muted">Chưa có sản phẩm nào được bán &amp; thanh toán.</p></c:when>
        <c:otherwise>
            <table class="table">
                <thead><tr><th style="width:50px">#</th><th>Sản phẩm</th><th style="width:120px">Số ly</th><th style="width:180px">Doanh thu</th></tr></thead>
                <tbody>
                    <c:forEach var="r" items="${topProducts}" varStatus="st">
                        <tr><td>${st.index + 1}</td><td>${r.label}</td><td>${r.count}</td><td><strong><fmt:formatNumber value="${r.amount}" maxFractionDigits="0"/> ₫</strong></td></tr>
                    </c:forEach>
                </tbody>
            </table>
        </c:otherwise>
    </c:choose>
</div>

<jsp:include page="../layout/footer.jsp" />
