<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="welcome">
    <h1>Xin chào, ${sessionScope.authUser.fullName}</h1>
    <p>Quản trị hệ thống · doanh thu toàn chuỗi</p>
</div>

<%-- Bộ lọc khoảng ngày + xuất Excel --%>
<form method="get" action="${ctx}/dashboard" class="card" style="display:flex;gap:16px;align-items:flex-end;flex-wrap:wrap;margin-bottom:24px">
    <div class="form-group" style="margin:0">
        <label for="from">Từ ngày</label>
        <input id="from" type="date" name="from" value="${fromDate}" class="form-control" style="width:170px">
    </div>
    <div class="form-group" style="margin:0">
        <label for="to">Đến ngày</label>
        <input id="to" type="date" name="to" value="${toDate}" class="form-control" style="width:170px">
    </div>
    <button type="submit" class="btn btn-primary">Lọc</button>
    <div style="display:flex;gap:6px">
        <button type="button" class="btn btn-ghost btn-sm" onclick="preset(7)">7 ngày</button>
        <button type="button" class="btn btn-ghost btn-sm" onclick="preset(30)">30 ngày</button>
        <button type="button" class="btn btn-ghost btn-sm" onclick="preset(90)">90 ngày</button>
    </div>
    <a class="btn btn-ghost" style="margin-left:auto" href="${ctx}/admin/report?action=export&from=${fromDate}&to=${toDate}">
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 3v12m0 0 4-4m-4 4-4-4"/><path d="M4 17v2a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-2"/></svg>
        Xuất Excel
    </a>
</form>

<%-- Thẻ số liệu --%>
<div class="card-grid">
    <div class="card stat"><span class="label">Doanh thu hôm nay</span><span class="value"><fmt:formatNumber value="${summary.todayRevenue}" maxFractionDigits="0"/></span></div>
    <div class="card stat"><span class="label">Hoá đơn hôm nay</span><span class="value">${summary.todayBills}</span></div>
    <div class="card stat"><span class="label">Doanh thu trong kỳ</span><span class="value"><fmt:formatNumber value="${summary.revenue}" maxFractionDigits="0"/></span></div>
    <div class="card stat"><span class="label">Hoá đơn trong kỳ</span><span class="value">${summary.paidBills}</span></div>
</div>

<div style="display:flex;gap:16px;flex-wrap:wrap;margin:16px 0 0">
    <div class="card" style="flex:1;min-width:220px"><span class="muted">Tổng giảm giá (voucher)</span><div style="font-size:1.3rem;font-weight:700"><fmt:formatNumber value="${summary.discount}" maxFractionDigits="0"/> ₫</div></div>
    <div class="card" style="flex:1;min-width:220px"><span class="muted">Tổng VAT đã thu</span><div style="font-size:1.3rem;font-weight:700"><fmt:formatNumber value="${summary.vat}" maxFractionDigits="0"/> ₫</div></div>
</div>

<%-- Biểu đồ doanh thu theo ngày --%>
<div class="card" style="margin-top:24px">
    <div class="chart-head">
        <h3 style="margin:0">Biểu đồ doanh thu theo ngày</h3>
        <span class="muted">${fromDate} → ${toDate}</span>
    </div>
    <c:choose>
        <c:when test="${maxDaily <= 0}">
            <p class="muted" style="margin:8px 0 0">Chưa có doanh thu trong kỳ đã chọn.</p>
        </c:when>
        <c:otherwise>
            <div class="chart" role="img" aria-label="Biểu đồ doanh thu theo ngày">
                <c:forEach var="d" items="${daily}">
                    <div class="chart-col" title="${d.label} · ${d.count} HĐ">
                        <div class="chart-bar" style="height:${(d.amount * 100) / maxDaily}%"></div>
                    </div>
                </c:forEach>
            </div>
            <div class="chart-axis"><span>${fromDate}</span><span>${toDate}</span></div>
        </c:otherwise>
    </c:choose>
</div>

<%-- Bảng phân tích --%>
<div class="grid-2">
    <div class="card">
        <h3 style="margin-top:0">Doanh thu theo chi nhánh</h3>
        <c:choose>
            <c:when test="${empty byBranch}"><p class="muted">Chưa có dữ liệu.</p></c:when>
            <c:otherwise>
                <table class="table">
                    <thead><tr><th>Chi nhánh</th><th style="width:80px">HĐ</th><th style="width:150px">Doanh thu</th></tr></thead>
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
                    <thead><tr><th>Hình thức</th><th style="width:80px">HĐ</th><th style="width:150px">Doanh thu</th></tr></thead>
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

<div class="card" style="margin-top:24px">
    <h3 style="margin-top:0">Top sản phẩm bán chạy</h3>
    <c:choose>
        <c:when test="${empty topProducts}"><p class="muted">Chưa có sản phẩm nào được bán &amp; thanh toán trong kỳ.</p></c:when>
        <c:otherwise>
            <table class="table">
                <thead><tr><th style="width:50px">#</th><th>Sản phẩm</th><th style="width:110px">Số ly</th><th style="width:170px">Doanh thu</th></tr></thead>
                <tbody>
                    <c:forEach var="r" items="${topProducts}" varStatus="st">
                        <tr><td>${st.index + 1}</td><td>${r.label}</td><td>${r.count}</td><td><strong><fmt:formatNumber value="${r.amount}" maxFractionDigits="0"/> ₫</strong></td></tr>
                    </c:forEach>
                </tbody>
            </table>
        </c:otherwise>
    </c:choose>
</div>

<script>
  function preset(days){
    var to = new Date(), from = new Date();
    from.setDate(to.getDate() - (days - 1));
    var f = function(d){ return d.toISOString().slice(0,10); };
    document.getElementById('from').value = f(from);
    document.getElementById('to').value = f(to);
    document.getElementById('from').form.submit();
  }
</script>

<jsp:include page="../layout/footer.jsp" />
