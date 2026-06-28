<%@ page contentType="text/html;charset=UTF-8" isErrorPage="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<!DOCTYPE html>
<html lang="vi"><head><meta charset="UTF-8">
<title>500 · Lỗi hệ thống</title>
<link rel="stylesheet" href="${ctx}/assets/css/cafe-theme.css"></head>
<body><div class="login-wrap"><div class="login-card" style="text-align:center">
    <div class="empty-state"><div class="icon">⚠️</div>
        <h1>500 — Lỗi hệ thống</h1>
        <p>Đã có lỗi xảy ra. Vui lòng thử lại sau.</p>
    </div>
    <a class="btn btn-primary btn-full" href="${ctx}/dashboard">Về bảng điều khiển</a>
</div></div></body></html>
