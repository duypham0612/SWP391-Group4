<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="welcome">
    <h1>Xin chào, ${sessionScope.authUser.fullName}</h1>
    <p>Quản lý chi nhánh · ${sessionScope.authUser.branchName} · ${today}</p>
</div>

<c:if test="${not empty sessionScope.flashError}">
    <div class="alert alert-error">${sessionScope.flashError}</div><c:remove var="flashError" scope="session" />
</c:if>

<div class="card-grid">
    <div class="card stat">
        <span class="label">Doanh thu hôm nay</span>
        <span class="value"><fmt:formatNumber value="${summary.todayRevenue}" maxFractionDigits="0"/> ₫</span>
    </div>
    <a class="card stat" href="${ctx}/manager/inventory">
        <span class="label">Nguyên liệu sắp hết</span>
        <span class="value">${summary.lowStockCount}</span>
    </a>
    <a class="card stat" href="${ctx}/manager/inventory" style="${summary.oversoldCount gt 0 ? 'border-color:var(--st-cancelled)' : ''}">
        <span class="label">Âm kho cần kiểm kê</span>
        <span class="value">${summary.oversoldCount}</span>
        <span class="muted">
            <c:choose>
                <c:when test="${summary.oversoldCount > 0}">đang lệch số dư tồn</c:when>
                <c:otherwise>Không có tồn âm</c:otherwise>
            </c:choose>
        </span>
    </a>
    <a class="card stat" href="${ctx}/manager/shift">
        <span class="label">Nhân viên có ca hôm nay</span>
        <span class="value">${summary.staffOnShift}</span>
    </a>
    <a class="card stat" href="${ctx}/manager/attendance">
        <span class="label">Chấm công chờ duyệt</span>
        <span class="value">${summary.pendingApprovals}</span>
    </a>
    <a class="card stat" href="${ctx}/manager/menu-block"
       style="${summary.overdueMenuBlockCount gt 0 ? 'border-color:var(--st-cancelled)' : ''}">
        <span class="label">Yêu cầu tạm hết chờ xử lý</span>
        <span class="value">${summary.openMenuBlockCount}</span>
        <span class="muted">
            <c:choose>
                <c:when test="${summary.overdueMenuBlockCount > 0}">${summary.overdueMenuBlockCount} món đã quá hạn dự kiến có lại</c:when>
                <c:when test="${summary.openMenuBlockCount > 0}">món đang bị chặn bán</c:when>
                <c:otherwise>Không có món nào bị chặn bán</c:otherwise>
            </c:choose>
        </span>
    </a>
    <a class="card stat" href="${ctx}/manager/waste">
        <span class="label">Hao hụt hôm nay</span>
        <span class="value"><fmt:formatNumber value="${summary.todayWaste.totalCost}" maxFractionDigits="0"/> ₫</span>
        <span class="muted">${summary.todayWaste.activeCount} dòng · ${summary.todayWaste.remakeCount} làm lại</span>
    </a>
    <a class="card stat" href="${ctx}/manager/receipt">
        <span class="label">Nhập kho</span>
        <span class="value">→</span>
    </a>
</div>

<c:if test="${summary.hasOversold}">
<div class="card" style="margin-bottom:var(--s5);border-color:var(--st-cancelled)">
    <h3 style="margin-top:0">Tồn âm cần đối soát</h3>
    <p class="muted">Số dư hiện tại đang thấp hơn 0. Kiểm kê thực tế rồi ghi điều chỉnh để đưa tồn về đúng số.</p>
    <table class="table">
        <thead><tr><th>Nguyên liệu</th><th style="width:150px">Tồn hiện tại</th><th style="width:120px">Loại</th><th></th></tr></thead>
        <tbody>
            <c:forEach var="o" items="${oversoldAlerts}">
                <tr>
                    <td><strong>${o.ingredientName}</strong></td>
                    <td><span class="badge badge-cancelled">${o.quantityOnHand} ${o.ingredientUnit}</span></td>
                    <td><c:choose><c:when test="${o.ingredientType == 'RAW'}">Thô</c:when><c:otherwise>Pha sẵn</c:otherwise></c:choose></td>
                    <td><a class="btn btn-ghost btn-sm" href="${ctx}/manager/reconciliation?action=new">Kiểm kê</a></td>
                </tr>
            </c:forEach>
        </tbody>
    </table>
</div>
</c:if>

<c:if test="${summary.hasOpenMenuBlocks}">
<div class="card" style="margin-bottom:var(--s5)">
    <h3 style="margin-top:0">Yêu cầu tạm hết đang chờ</h3>
    <p class="muted">Món đã bị chặn bán ngay khi pha chế báo. Xác nhận hoặc mở bán lại để món quay về menu.</p>
    <table class="table">
        <thead>
            <tr><th>Món</th><th>Lý do</th><th>Người báo</th><th>Báo lúc</th><th>Dự kiến có lại</th><th></th></tr>
        </thead>
        <tbody>
            <c:forEach var="r" items="${summary.openMenuBlocks}">
                <tr>
                    <td>
                        <strong>${r.productName}</strong>
                        <c:if test="${r.overdue}"><span class="badge badge-cancelled" style="margin-left:6px">Quá hạn</span></c:if>
                    </td>
                    <td>${r.reasonLabel}</td>
                    <td>${r.requesterName}</td>
                    <td>${r.requestedAtText}</td>
                    <td>
                        <c:choose>
                            <c:when test="${empty r.backInEtaText}">Chưa rõ</c:when>
                            <c:otherwise>${r.backInEtaText}</c:otherwise>
                        </c:choose>
                    </td>
                    <td><a class="btn btn-ghost btn-sm" href="${ctx}/manager/menu-block">Xử lý</a></td>
                </tr>
            </c:forEach>
        </tbody>
    </table>
</div>
</c:if>

<div class="grid-2 manager-dashboard-grid">
    <div class="card dashboard-table-card">
        <h3 style="margin-top:0">Ca làm hôm nay</h3>
        <c:choose>
            <c:when test="${empty staffOnShift}">
                <p class="muted">Chưa xếp ca nào cho hôm nay.</p>
            </c:when>
            <c:otherwise>
                <table class="table dashboard-mini-table shift-table">
                    <thead><tr><th>Nhân viên</th><th>Ca</th><th>Giờ</th></tr></thead>
                    <tbody>
                        <c:forEach var="a" items="${staffOnShift}">
                            <tr><td>${a.userName}</td><td>${a.templateName}</td><td>${a.startTime}–${a.endTime}</td></tr>
                        </c:forEach>
                    </tbody>
                </table>
            </c:otherwise>
        </c:choose>
    </div>
    <div class="card dashboard-table-card">
        <h3 style="margin-top:0">Cảnh báo tồn thấp</h3>
        <c:choose>
            <c:when test="${empty lowStockAlerts}">
                <p class="muted">Tồn kho ổn định.</p>
            </c:when>
            <c:otherwise>
                <table class="table dashboard-mini-table low-stock-table">
                    <thead><tr><th>Nguyên liệu</th><th>Tồn</th><th>Ngưỡng</th></tr></thead>
                    <tbody>
                        <c:forEach var="l" items="${lowStockAlerts}">
                            <tr>
                                <td>${l.ingredientName} <span class="badge badge-waiting" style="margin-left:6px">Thấp</span></td>
                                <td>${l.quantityOnHand} ${l.ingredientUnit}</td>
                                <td>${l.minThreshold}</td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </c:otherwise>
        </c:choose>
    </div>
</div>

<jsp:include page="../layout/footer.jsp" />
