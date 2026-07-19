<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%--
  Fragment dùng chung hiển thị status badge (Barista/Cashier/QR khách giống hệt nhau).
  Dùng: <jsp:include page="/WEB-INF/views/layout/_statusBadge.jsp"><jsp:param name="status" value="${item.status}"/></jsp:include>
--%>
<c:set var="st" value="${param.status}" />
<c:choose>
    <c:when test="${st == 'WAITING'}">  <span class="badge badge-waiting">Chờ pha</span></c:when>
    <c:when test="${st == 'MAKING'}"><span class="badge badge-making">Đang pha</span></c:when>
    <c:when test="${st == 'READY'}">    <span class="badge badge-ready">Đã pha xong</span></c:when>
    <c:when test="${st == 'PICKED_UP'}"><span class="badge badge-ready">Đã được nhận</span></c:when>
    <c:when test="${st == 'BLOCKED'}">  <span class="badge badge-cancelled">Bị chặn</span></c:when>
    <c:when test="${st == 'REMAKE'}">   <span class="badge badge-waiting">Làm lại</span></c:when>
    <c:when test="${st == 'SERVED'}">   <span class="badge badge-served">Đã phục vụ</span></c:when>
    <c:when test="${st == 'CANCELLED'}"><span class="badge badge-cancelled">Đã huỷ</span></c:when>
    <c:otherwise><span class="badge badge-served">${st}</span></c:otherwise>
</c:choose>
