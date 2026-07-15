<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<c:if test="${not empty clockStatus}">
    <div class="card shift-clock-card">
        <div class="shift-clock-main">
            <div>
                <div class="eyebrow">Chấm công ca hôm nay</div>
                <h3>Vào ca / Tan ca</h3>
                <p>${clockStatus.statusText}</p>
                <c:if test="${clockStatus.hasAssignment}">
                    <div class="shift-clock-meta">
                        <span>${clockStatus.templateName}</span>
                        <span>${clockStatus.shiftTimeDisplay}</span>
                    </div>
                </c:if>
            </div>
            <c:choose>
                <c:when test="${not clockStatus.hasAssignment}">
                    <span class="badge badge-cancelled">Chưa có ca</span>
                </c:when>
                <c:when test="${clockStatus.canClockIn}">
                    <span class="badge badge-waiting">Chưa vào</span>
                </c:when>
                <c:when test="${clockStatus.canClockOut}">
                    <span class="badge badge-making">Đang làm</span>
                </c:when>
                <c:otherwise>
                    <span class="badge badge-ready">Đã tan ca</span>
                </c:otherwise>
            </c:choose>
        </div>

        <div class="shift-clock-facts">
            <div>
                <span>Vào ca</span>
                <strong>${clockStatus.checkInDisplay}</strong>
            </div>
            <div>
                <span>Tan ca</span>
                <strong>${clockStatus.checkOutDisplay}</strong>
            </div>
            <div>
                <span>Giờ làm</span>
                <strong>${clockStatus.workHoursDisplay}h</strong>
            </div>
        </div>

        <div class="shift-clock-actions">
            <c:choose>
                <c:when test="${clockStatus.canClockIn}">
                    <form action="${clockPostUrl}" method="post">
                        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                        <input type="hidden" name="action" value="clockIn">
                        <button type="submit" class="btn btn-primary">Vào ca</button>
                    </form>
                </c:when>
                <c:when test="${clockStatus.canClockOut}">
                    <form action="${clockPostUrl}" method="post">
                        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                        <input type="hidden" name="action" value="clockOut">
                        <button type="submit" class="btn btn-primary">Tan ca</button>
                    </form>
                </c:when>
                <c:when test="${not clockStatus.hasAssignment}">
                    <button type="button" class="btn btn-ghost" disabled>Vào ca</button>
                </c:when>
                <c:otherwise>
                    <button type="button" class="btn btn-ghost" disabled>Đã tan ca</button>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
</c:if>
