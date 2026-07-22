<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><div class="eyebrow">Bán hàng</div><h1>Ca thu ngân</h1><p>payment.CashierShift — mở quỹ đầu ca, đóng &amp; chốt cuối ca</p></div>
</div>

<c:if test="${not empty sessionScope.flashOk}">
    <div class="alert alert-success">${sessionScope.flashOk}</div><c:remove var="flashOk" scope="session" />
</c:if>
<c:if test="${not empty sessionScope.flashError}">
    <div class="alert alert-error">${sessionScope.flashError}</div><c:remove var="flashError" scope="session" />
</c:if>

<div class="card form-card" style="margin-bottom:18px">
    <div style="display:flex;justify-content:space-between;gap:18px;align-items:flex-start;flex-wrap:wrap">
        <div>
            <div class="eyebrow">Trực ca thu ngân</div>
            <h3 style="margin:4px 0 6px">
                <c:choose>
                    <c:when test="${dutyState == 'ON_DUTY'}">Đang trực ca — sẵn sàng bán</c:when>
                    <c:when test="${dutyState == 'CLOCKED_NO_TILL'}">Đã vào ca — chưa mở két</c:when>
                    <c:when test="${dutyState == 'TILL_ONLY'}">Két đang mở — chưa vào ca</c:when>
                    <c:otherwise>Chưa vào ca</c:otherwise>
                </c:choose>
            </h3>
            <p class="muted" style="margin:0">
                <c:choose>
                    <c:when test="${dutyState == 'ON_DUTY'}">Bạn có thể đặt món, xử lý bàn, thu tiền và huỷ đơn theo quyền thu ngân.</c:when>
                    <c:when test="${dutyState == 'CLOCKED_NO_TILL'}">Bấm Bắt đầu ca để mở két tiền và bắt đầu thao tác bán hàng.</c:when>
                    <c:when test="${dutyState == 'TILL_ONLY'}">Bấm Bắt đầu ca để ghi nhận chấm công cho ca đang mở.</c:when>
                    <c:otherwise>Bấm Bắt đầu ca để chấm công và mở ca két trong cùng một bước.</c:otherwise>
                </c:choose>
            </p>
            <c:if test="${not empty clockStatus and clockStatus.hasAssignment}">
                <div class="shift-clock-meta">
                    <span>${clockStatus.templateName}</span>
                    <span>${clockStatus.shiftTimeDisplay}</span>
                    <span>${clockStatus.statusText}</span>
                </div>
            </c:if>
        </div>
        <c:choose>
            <c:when test="${dutyState == 'ON_DUTY'}">
                <span class="badge badge-ready">ON_DUTY</span>
            </c:when>
            <c:when test="${dutyState == 'CLOCKED_NO_TILL'}">
                <span class="badge badge-waiting">CLOCKED_NO_TILL</span>
            </c:when>
            <c:when test="${dutyState == 'TILL_ONLY'}">
                <span class="badge badge-making">TILL_ONLY</span>
            </c:when>
            <c:otherwise>
                <span class="badge badge-cancelled">OFF_DUTY</span>
            </c:otherwise>
        </c:choose>
    </div>

    <c:choose>
        <c:when test="${dutyState == 'ON_DUTY' and not empty current}">
            <form action="${ctx}/cashier/shift" method="post" style="margin-top:18px;display:flex;gap:10px;align-items:flex-end;flex-wrap:wrap">
                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                <input type="hidden" name="action" value="closeDuty">
                <input type="hidden" name="shiftId" value="${current.cashierShiftId}">
                <div class="form-group" style="margin:0;width:280px"><label>Quỹ cuối ca (đếm tay)</label>
                    <input type="number" name="closingCash" class="form-control" min="0" step="1" required value="0">
                    <small class="muted">Phải bằng quỹ đầu ca + doanh thu ca:
                        <strong><fmt:formatNumber value="${current.openingCash + current.totalCollected}" maxFractionDigits="0"/> ₫</strong>
                    </small>
                </div>
                <button type="submit" class="btn btn-primary" onclick="return confirm('Kết ca và tan ca?');">Kết ca</button>
                <a class="btn btn-ghost" href="${ctx}/cashier/checkout">Tới thanh toán</a>
            </form>
        </c:when>
        <c:otherwise>
            <form action="${ctx}/cashier/shift" method="post" style="margin-top:18px;display:flex;gap:10px;align-items:flex-end;flex-wrap:wrap">
                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                <input type="hidden" name="action" value="startDuty">
                <div class="form-group" style="margin:0;width:220px"><label>Quỹ đầu ca (tiền mặt)</label>
                    <input type="number" name="openingCash" class="form-control" min="0" step="1000" value="${not empty current ? current.openingCash : 0}"></div>
                <button type="submit" class="btn btn-primary">Bắt đầu ca</button>
            </form>
        </c:otherwise>
    </c:choose>
</div>

<%-- R1 · Tổng doanh thu theo ngày (toàn chi nhánh, các bill đã thu hôm nay) --%>
<div class="card-grid" style="margin-bottom:18px">
    <div class="card stat"><span class="label">Doanh thu hôm nay</span><span class="value"><fmt:formatNumber value="${todayRevenue}" maxFractionDigits="0"/> ₫</span></div>
    <div class="card stat"><span class="label">Số hoá đơn đã thu hôm nay</span><span class="value">${todayBillCount}</span></div>
</div>

<c:choose>
    <%-- Báo cáo ca vừa đóng --%>
    <c:when test="${not empty shift and not shift.open}">
        <div class="card form-card">
            <h3 style="margin-top:0">Báo cáo ca #${shift.cashierShiftId}</h3>
            <table class="table">
                <tr><td>Thu ngân</td><td>${shift.cashierName}</td></tr>
                <tr><td>Quỹ đầu ca</td><td><fmt:formatNumber value="${shift.openingCash}" maxFractionDigits="0"/> ₫</td></tr>
                <tr><td>Số hoá đơn đã thu</td><td>${shift.billCount}</td></tr>
                <tr><td>Tổng tiền thu (PAID)</td><td><strong><fmt:formatNumber value="${shift.totalCollected}" maxFractionDigits="0"/> ₫</strong></td></tr>
                <tr><td>Quỹ cuối ca (đếm tay)</td><td><fmt:formatNumber value="${shift.closingCash}" maxFractionDigits="0"/> ₫</td></tr>
            </table>
            <a class="btn btn-primary" href="${ctx}/cashier/shift">Mở ca mới</a>
        </div>
    </c:when>
    <%-- Đang trực ca --%>
    <c:when test="${dutyState == 'ON_DUTY' and not empty current}">
        <div class="card form-card">
            <h3 style="margin-top:0">Ca đang mở #${current.cashierShiftId}</h3>
            <p>Mở lúc ${current.openedAt} · Quỹ đầu ca <strong><fmt:formatNumber value="${current.openingCash}" maxFractionDigits="0"/> ₫</strong></p>
            <p>Doanh thu ca <strong><fmt:formatNumber value="${current.totalCollected}" maxFractionDigits="0"/> ₫</strong>
                · Quỹ phải bàn giao <strong><fmt:formatNumber value="${current.openingCash + current.totalCollected}" maxFractionDigits="0"/> ₫</strong></p>
            <div style="display:flex;gap:10px">
                <a class="btn btn-primary" href="${ctx}/cashier/checkout">Tới thanh toán →</a>
            </div>
        </div>
    </c:when>
    <%-- Chưa trực ca --%>
    <c:otherwise>
        <div class="alert alert-info">Bạn có thể xem các màn thu ngân, nhưng cần Bắt đầu ca trước khi đặt món, thu tiền hoặc thao tác ghi.</div>
    </c:otherwise>
</c:choose>

<h3 style="margin-top:24px">Các ca gần đây</h3>
<c:choose>
    <c:when test="${empty history}">
        <div class="card empty-state"><div class="icon">∅</div><p>Chưa có ca nào.</p></div>
    </c:when>
    <c:otherwise>
        <table class="table">
            <thead><tr><th>#</th><th>Thu ngân</th><th>Mở</th><th>Đóng</th><th style="width:120px">Trạng thái</th></tr></thead>
            <tbody>
                <c:forEach var="s" items="${history}">
                    <tr>
                        <td><a href="${ctx}/cashier/shift?action=report&shiftId=${s.cashierShiftId}">#${s.cashierShiftId}</a></td>
                        <td>${s.cashierName}</td>
                        <td>${s.openedAt}</td>
                        <td><c:choose><c:when test="${s.open}"><span class="badge badge-making">Đang mở</span></c:when><c:otherwise>${s.closedAt}</c:otherwise></c:choose></td>
                        <td><c:choose><c:when test="${s.open}"><span class="badge badge-waiting">OPEN</span></c:when><c:otherwise><span class="badge badge-served">Đã đóng</span></c:otherwise></c:choose></td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </c:otherwise>
</c:choose>

<jsp:include page="../layout/footer.jsp" />
