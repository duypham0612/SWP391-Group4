<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- Banner trực ca cho màn barista. Cần: onShift, clockStatus.

     Banner này KHÔNG tự chấm công. Vào ca là bước có ngữ cảnh: barista cần thấy ca mình
     được xếp và bàn giao của ca trước đang chờ xác nhận trước khi nhận quầy — cả hai đều
     nằm ở màn "Ca làm của tôi". Đặt nút chấm công ngay đây thì thao tác đó bị rút gọn
     thành một cú bấm mù giữa lúc đang đứng máy. --%>
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
                    <span><c:out value="${view.shiftStatus(clockStatus)}" /></span>
                </c:otherwise>
            </c:choose>
        </div>
        <%-- Chưa được xếp ca thì không có hành động nào để mời: người mở khoá được là Quản lý. --%>
        <c:if test="${not noAssignment}">
            <a class="btn ${clockStatus.canClockIn ? 'btn-primary' : 'btn-ghost'}"
               href="${pageContext.request.contextPath}/barista/shift">
                <c:choose>
                    <c:when test="${clockStatus.canClockIn}">Vào ca tại Ca làm của tôi →</c:when>
                    <c:otherwise>Tới Ca làm của tôi →</c:otherwise>
                </c:choose>
            </a>
        </c:if>
    </div>
</c:if>
