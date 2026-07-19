<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><div class="eyebrow">Pha chế</div><h1>Ca làm của tôi</h1><p>Chấm công và giờ làm của bạn</p></div>
</div>

<c:if test="${not empty sessionScope.flashError}">
    <div class="alert alert-error">${sessionScope.flashError}</div>
    <c:remove var="flashError" scope="session" />
</c:if>
<c:if test="${not empty sessionScope.flashOk}">
    <div class="alert alert-success">${sessionScope.flashOk}</div>
    <c:remove var="flashOk" scope="session" />
</c:if>

<jsp:include page="../layout/_shiftClockCard.jsp" />

<c:if test="${not empty monthSummary}">
    <div class="page-header" style="margin-top:var(--s6)">
        <div><h2 style="margin:0">Giờ làm tháng ${month}</h2><p>Lịch đi làm và giờ công đã ghi nhận</p></div>
        <div style="display:flex;gap:8px;align-items:center">
            <a class="btn btn-ghost btn-sm" href="${ctx}/barista/shift?month=${prevMonth}">← Tháng trước</a>
            <strong>${month}</strong>
            <a class="btn btn-ghost btn-sm" href="${ctx}/barista/shift?month=${nextMonth}">Tháng sau →</a>
        </div>
    </div>

    <div class="card-grid">
        <div class="card stat">
            <span class="label">Giờ đã duyệt</span>
            <span class="value">${monthSummary.approvedHours}h</span>
            <div class="muted" style="font-size:.8em;margin-top:4px">quản lý đã duyệt</div>
        </div>
        <div class="card stat">
            <span class="label">Chờ duyệt</span>
            <span class="value">${monthSummary.pendingHours}h</span>
            <div class="muted" style="font-size:.8em;margin-top:4px">chưa tính vào lương</div>
        </div>
        <div class="card stat">
            <span class="label">Số ca đã làm</span>
            <span class="value">${monthSummary.shiftsWorked}</span>
            <div class="muted" style="font-size:.8em;margin-top:4px">trung bình ${monthSummary.avgHoursPerShift}h mỗi ca</div>
        </div>
        <c:if test="${monthSummary.payrollLocked}">
            <div class="card stat">
                <span class="label">Lương đã chốt</span>
                <span class="value"><fmt:formatNumber value="${monthSummary.lockedPay}" maxFractionDigits="0"/> ₫</span>
                <div class="muted" style="font-size:.8em;margin-top:4px">
                    <c:choose>
                        <c:when test="${monthSummary.hoursMismatch}">
                            quản lý chốt ${monthSummary.lockedHours}h (chấm công của bạn: ${monthSummary.approvedHours}h)
                        </c:when>
                        <c:otherwise>${monthSummary.lockedHours}h × lương/giờ</c:otherwise>
                    </c:choose>
                </div>
            </div>
        </c:if>
    </div>

    <c:if test="${not monthSummary.payrollLocked}">
        <div class="alert alert-info" style="margin-top:var(--s4)">
            Lương tháng này sẽ có khi quản lý chốt bảng lương.
        </div>
    </c:if>

    <c:if test="${monthSummary.openCount > 0}">
        <div class="alert alert-warn">
            Có ${monthSummary.openCount} ca bạn quên bấm Tan ca — những ca này chưa được tính giờ.
            Báo quản lý để chỉnh giúp.
        </div>
    </c:if>

    <c:choose>
        <c:when test="${empty monthRows}">
            <div class="card empty-state"><div class="icon">∅</div><p>Tháng này bạn chưa được xếp ca nào.</p></div>
        </c:when>
        <c:otherwise>
            <div class="table-scroll">
                <table class="table" style="min-width:640px">
                    <thead><tr>
                        <th style="width:80px">Ngày</th>
                        <th>Ca</th>
                        <th style="width:80px">Vào</th>
                        <th style="width:80px">Tan</th>
                        <th style="width:80px">Giờ</th>
                        <th style="width:120px">Trạng thái</th>
                    </tr></thead>
                    <tbody>
                        <c:forEach var="r" items="${monthRows}">
                            <tr>
                                <td>${r.workDateDisplay}</td>
                                <td>${r.templateName} <span class="muted">${r.shiftTimeDisplay}</span></td>
                                <td>${r.checkInDisplay}</td>
                                <td>${r.checkOutDisplay}</td>
                                <td>${r.workHoursDisplay}</td>
                                <td><span class="badge ${r.stateBadge}">${r.stateLabel}</span></td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:otherwise>
    </c:choose>
</c:if>

<jsp:include page="../layout/footer.jsp" />
