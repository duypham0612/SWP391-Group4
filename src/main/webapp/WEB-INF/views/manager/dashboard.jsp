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
    <a class="card stat" href="${ctx}/manager/shift">
        <span class="label">Nhân viên có ca hôm nay</span>
        <span class="value">${summary.staffOnShift}</span>
    </a>
    <a class="card stat" href="${ctx}/manager/attendance">
        <span class="label">Chấm công chờ duyệt</span>
        <span class="value">${summary.pendingApprovals}</span>
    </a>
    <a class="card stat" href="${ctx}/manager/receipt">
        <span class="label">Nhập kho</span>
        <span class="value">→</span>
    </a>
</div>

<div class="grid-2">
    <div class="card">
        <h3 style="margin-top:0">Ca làm hôm nay</h3>
        <c:choose>
            <c:when test="${empty staffOnShift}">
                <p class="muted">Chưa xếp ca nào cho hôm nay.</p>
            </c:when>
            <c:otherwise>
                <table class="table">
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
    <div class="card">
        <h3 style="margin-top:0">Cảnh báo tồn thấp</h3>
        <c:choose>
            <c:when test="${empty lowStockAlerts}">
                <p class="muted">Tồn kho ổn định.</p>
            </c:when>
            <c:otherwise>
                <table class="table">
                    <thead><tr><th>Nguyên liệu</th><th>Tồn</th><th>Ngưỡng</th></tr></thead>
                    <tbody>
                        <c:forEach var="l" items="${lowStockAlerts}">
                            <tr>
                                <td>${l.ingredientName} <span class="badge badge-cancelled" style="margin-left:6px">Thấp</span></td>
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
