<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:choose>
    <c:when test="${not empty sessionScope.authUser}"><c:redirect url="/dashboard" /></c:when>
    <c:otherwise><c:redirect url="/auth/login" /></c:otherwise>
</c:choose>
