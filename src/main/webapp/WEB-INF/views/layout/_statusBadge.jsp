<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%--
  Fragment dùng chung hiển thị status badge (Barista/Cashier/QR khách giống hệt nhau).
  Dùng: <jsp:include page="/WEB-INF/views/layout/_statusBadge.jsp"><jsp:param name="status" value="${item.status}"/></jsp:include>
--%>
<c:set var="st" value="${param.status}" />
<c:choose>
    <c:when test="${st == 'WAITING'}">  <span class="badge badge-waiting">Chờ làm</span></c:when>
    <c:when test="${st == 'MAKING'}">   <span class="badge badge-making">Đang pha</span></c:when>
    <c:when test="${st == 'READY'}">    <span class="badge badge-ready">Sẵn lấy</span></c:when>
    <c:when test="${st == 'SERVED'}">   <span class="badge badge-served">Đã phục vụ</span></c:when>
    <c:when test="${st == 'CANCELLED'}"><span class="badge badge-cancelled">Đã huỷ</span></c:when>
    <c:otherwise><span class="badge badge-served">${st}</span></c:otherwise>
</c:choose>
