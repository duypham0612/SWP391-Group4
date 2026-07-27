<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- Nhắc bàn giao chưa xác nhận. BaristaShift.expose nạp pendingHandoverCount cho mọi trang đầy đủ. --%>
<c:if test="${pendingHandoverCount > 0}">
    <a class="alert alert-warn" href="${pageContext.request.contextPath}/barista/handover"
       style="display:block;margin-bottom:var(--s4);text-decoration:none">
        <strong>${pendingHandoverCount} bàn giao ca đang chờ bạn xác nhận.</strong>
        Mở để tiếp nhận việc ca trước để lại →
    </a>
</c:if>
