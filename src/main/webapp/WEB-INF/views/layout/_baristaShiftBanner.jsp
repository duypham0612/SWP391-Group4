<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- Banner trực ca cho màn barista. Cần: onShift, clockStatus, clockPostUrl. --%>
<c:if test="${not onShift}">
    <div class="alert alert-warn barista-offshift">
        <div class="barista-offshift__text">
            <strong>Bạn đang ngoài ca — màn hình chỉ để xem.</strong>
            <span>${not empty clockStatus.statusText ? clockStatus.statusText : 'Hôm nay bạn chưa được xếp ca.'} Vào ca để bắt đầu thao tác.</span>
        </div>
        <c:choose>
            <c:when test="${not empty clockStatus and clockStatus.canClockIn}">
                <form action="${clockPostUrl}" method="post">
                    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                    <input type="hidden" name="action" value="clockIn">
                    <button type="submit" class="btn btn-primary">Vào ca</button>
                </form>
            </c:when>
            <c:otherwise>
                <a class="btn btn-ghost" href="${pageContext.request.contextPath}/barista/handover">Tới chấm công →</a>
            </c:otherwise>
        </c:choose>
    </div>
</c:if>
