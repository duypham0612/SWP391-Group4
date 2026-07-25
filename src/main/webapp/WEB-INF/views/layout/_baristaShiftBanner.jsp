<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- Banner trực ca cho màn barista. Cần: onShift, clockStatus, clockPostUrl. --%>
<c:if test="${not onShift}">
    <div class="alert alert-warn barista-offshift">
        <%-- Một dòng: banner này nằm giữa header và danh sách món, mỗi dòng thừa đẩy việc
             xuống khỏi tầm mắt. Chi tiết trạng thái ca đã có ở màn Chấm công. --%>
        <%-- Chưa có ca nào được xếp thì nút "Vào ca" sẽ luôn báo lỗi và màn Chấm công cũng không
             giúp được gì — người duy nhất mở khoá được là Quản lý. Nói thẳng điều đó thay vì đẩy
             barista đi vòng qua một màn không có hành động nào. --%>
        <c:set var="noAssignment" value="${empty clockStatus or not clockStatus.hasAssignment}" />
        <div class="barista-offshift__text">
            <strong>Ngoài ca — chỉ xem.</strong>
            <c:choose>
                <c:when test="${noAssignment}">
                    <span>Hôm nay bạn chưa được xếp ca — liên hệ Quản lý chi nhánh để được xếp ca trước khi thao tác.</span>
                </c:when>
                <c:otherwise>
                    <span><c:out value="${clockStatus.statusText}" /></span>
                </c:otherwise>
            </c:choose>
        </div>
        <c:choose>
            <c:when test="${not empty clockStatus and clockStatus.canClockIn}">
                <form action="${clockPostUrl}" method="post">
                    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                    <input type="hidden" name="action" value="clockIn">
                    <button type="submit" class="btn btn-primary">Vào ca</button>
                </form>
            </c:when>
            <c:when test="${not noAssignment}">
                <a class="btn btn-ghost" href="${pageContext.request.contextPath}/barista/shift">Tới chấm công →</a>
            </c:when>
        </c:choose>
    </div>
</c:if>
