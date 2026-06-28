<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="u" value="${sessionScope.authUser}" />
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>${pageTitle} · Cafe Chain</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:wght@400;500;600;700&family=Playfair+Display:wght@700&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="${ctx}/assets/css/cafe-theme.css">
</head>
<body>
<div class="app-shell">
    <jsp:include page="sidebar.jsp" />
    <div class="app-main">
        <header class="topbar">
            <div class="topbar-title">${pageTitle}</div>
            <div class="topbar-user">
                <span class="user-meta">
                    <strong>${u.fullName}</strong>
                    <small>${u.roleName}<c:if test="${not empty u.branchName}"> · ${u.branchName}</c:if></small>
                </span>
                <span class="avatar">${u.fullName.substring(0,1)}</span>
                <a class="btn btn-ghost btn-sm" href="${ctx}/auth/logout">Đăng xuất</a>
            </div>
        </header>
        <main class="app-content">
